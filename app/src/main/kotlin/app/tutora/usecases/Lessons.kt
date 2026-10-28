package app.tutora.usecases

import app.tutora.data.LessonRow
import app.tutora.data.PaymentLessonRow
import app.tutora.data.PaymentRow
import app.tutora.data.ScheduleRepository
import app.tutora.data.inTransaction
import app.tutora.domain.CancelReason
import app.tutora.domain.Charge
import app.tutora.domain.LessonStatus
import app.tutora.domain.PayMethod
import app.tutora.domain.PayerKind
import app.tutora.domain.chargeCents
import app.tutora.domain.currentPrice
import app.tutora.domain.isoText
import app.tutora.domain.localOf
import app.tutora.domain.localToInstant
import app.tutora.domain.travelFor
import app.tutora.domain.valueWithoutTravel

/**
 * A row made Dada, with the price it has now, its travel and who owes it frozen on it
 * (AC-006-2, 3, AC-008-1). Inside a transaction.
 */
internal suspend fun freezeDone(
    ctx: Ctx,
    row: LessonRow,
): LessonRow? {
    val lesson = ScheduleRepository(ctx.db).load(row) ?: return null
    val price = currentPrice(lesson.place, lesson.who)
    return withDue(
        ctx,
        row.copy(
            status = LessonStatus.DONE.key,
            rateCents = price?.cents,
            rateType = price?.rateType?.name,
            travelFeeCents = travelFor(lesson.place, row.travelFeeCents),
            updatedAt = ctx.now(),
        ),
    )
}

/** What the cancel sheet confirms (AC-006-18). */
data class Cancellation(
    val reason: CancelReason,
    val note: String? = null,
    /** null = "Cobrar esta aula?" off. */
    val charge: Charge? = null,
)

/** Marking classes done, logging lessons given, cancelling and undoing (spec 006). Every write runs in one transaction. */
class Lessons(
    private val ctx: Ctx,
) {
    private val db get() = ctx.db
    private val dao get() = db.schedule()

    private suspend fun done(row: LessonRow): LessonRow? = freezeDone(ctx, row)

    /** Marcar como dada with the real minutes; a class of a series then stands on its own (AC-006-1, 2, 4). */
    suspend fun markDone(
        id: String,
        minutes: Int,
    ): ScheduleResult =
        db.inTransaction {
            val row = dao.lesson(id) ?: return@inTransaction ScheduleResult.Err(ScheduleError.NOT_FOUND)
            if (row.startAt > ctx.now()) return@inTransaction ScheduleResult.Err(ScheduleError.NOT_STARTED)
            val next = done(row.copy(plannedMin = minutes)) ?: return@inTransaction ScheduleResult.Err(ScheduleError.NOT_FOUND)
            dao.upsertLesson(next.copy(isDetached = row.ruleId != null))
            ScheduleResult.Ok(id)
        }

    /**
     * Registrar aula dada: a one-off class that already happened, saved as Dada with its price
     * frozen. A date or start still ahead is refused (AC-006-6, 8).
     */
    suspend fun logLesson(
        draft: LessonDraft,
        paidWith: PayMethod? = null,
    ): ScheduleResult =
        db.inTransaction {
            val today = localOf(ctx.now(), ctx.zone()).date
            if (draft.date > today) return@inTransaction ScheduleResult.Err(ScheduleError.FUTURE_DATE)
            val startAt = localToInstant(draft.date, draft.time, ctx.zone()).isoText()
            if (startAt > ctx.now()) return@inTransaction ScheduleResult.Err(ScheduleError.NOT_STARTED)
            if (!placeAllowed(db, draft.who, draft.placeId)) return@inTransaction ScheduleResult.Err(ScheduleError.PLACE_NOT_LINKED)
            val row =
                LessonRow(
                    id = ctx.newId(),
                    ruleId = null,
                    occurrenceDate = null,
                    studentId = (draft.who as? WhoId.OfStudent)?.id,
                    groupId = (draft.who as? WhoId.OfGroup)?.id,
                    placeId = draft.placeId,
                    startAt = startAt,
                    plannedMin = draft.plannedMin,
                    valueCents = draft.valueCents,
                    travelFeeCents = draft.travelFeeCents,
                    notes = draft.notes,
                    createdAt = ctx.now(),
                    updatedAt = ctx.now(),
                )
            val next = done(row) ?: return@inTransaction ScheduleResult.Err(ScheduleError.PLACE_NOT_LINKED)
            dao.insertLessons(listOf(next))
            // "Já foi pago": a payment of what the class owes, paying this class (AC-008-17).
            val due = next.dueCents?.takeIf { it > 0 }
            val payer = PayerKind.fromKey(next.payer)
            if (paidWith != null && due != null && payer != null) {
                val paymentId = ctx.newId()
                db.payments().upsertPayment(
                    PaymentRow(
                        paymentId,
                        next.studentId.takeIf { payer == PayerKind.STUDENT },
                        next.groupId.takeIf { payer == PayerKind.GROUP },
                        next.placeId.takeIf { payer == PayerKind.PLACE },
                        due,
                        paidWith.key,
                        ctx.now(),
                        null,
                        null,
                        ctx.now(),
                        ctx.now(),
                    ),
                )
                db.payments().insertChosen(listOf(PaymentLessonRow(paymentId, next.id)))
            }
            ScheduleResult.Ok(row.id)
        }

    /**
     * Cancelar aula: Cancelada, or Faltou for a no-show, with the reason and the charge worked out
     * now and stored. The class stays on its day and leaves its series (AC-006-10, 11, 19 to 22).
     */
    suspend fun cancel(
        id: String,
        c: Cancellation,
    ): ScheduleResult =
        db.inTransaction {
            val row = dao.lesson(id) ?: return@inTransaction ScheduleResult.Err(ScheduleError.NOT_FOUND)
            if (c.reason == CancelReason.NO_SHOW && row.startAt > ctx.now()) {
                return@inTransaction ScheduleResult.Err(ScheduleError.NOT_STARTED)
            }
            val lesson = ScheduleRepository(db).load(row) ?: return@inTransaction ScheduleResult.Err(ScheduleError.NOT_FOUND)
            val charge = c.charge?.takeIf { c.reason.chargeable }
            val cents =
                charge
                    ?.let {
                        chargeCents(it, lesson.valueWithoutTravel()) ?: return@inTransaction ScheduleResult.Err(ScheduleError.NO_PRICE)
                    }?.takeIf { it > 0 }
            dao.upsertLesson(
                withDue(
                    ctx,
                    row.copy(
                        status = c.reason.status.key,
                        cancelReason = c.reason.key,
                        cancelNote = c.note?.trim()?.ifEmpty { null },
                        feeCents = cents,
                        rateCents = null,
                        rateType = null,
                        isDetached = row.ruleId != null,
                        updatedAt = ctx.now(),
                    ),
                ),
            )
            ScheduleResult.Ok(id)
        }

    /** Desfazer: back to Agendada, following the current price again (AC-006-13). */
    suspend fun undo(id: String): ScheduleResult =
        db.inTransaction {
            val row = dao.lesson(id) ?: return@inTransaction ScheduleResult.Err(ScheduleError.NOT_FOUND)
            dao.upsertLesson(
                row.copy(
                    status = LessonStatus.SCHEDULED.key,
                    rateCents = null,
                    rateType = null,
                    feeCents = null,
                    cancelReason = null,
                    cancelNote = null,
                    payer = null,
                    dueCents = null,
                    packageId = null,
                    updatedAt = ctx.now(),
                ),
            )
            ScheduleResult.Ok(id)
        }
}
