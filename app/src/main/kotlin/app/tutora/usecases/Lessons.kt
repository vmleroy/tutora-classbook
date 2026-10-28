package app.tutora.usecases

import app.tutora.data.LessonRow
import app.tutora.data.ScheduleRepository
import app.tutora.data.SettingsRepository
import app.tutora.data.inTransaction
import app.tutora.domain.FeeSuggestion
import app.tutora.domain.LessonStatus
import app.tutora.domain.Who
import app.tutora.domain.currentPrice
import app.tutora.domain.isoText
import app.tutora.domain.localOf
import app.tutora.domain.localToInstant
import app.tutora.domain.ruleFor
import app.tutora.domain.suggestFee
import app.tutora.domain.travelFor
import app.tutora.domain.valueWithoutTravel

/** A new status for one class; [keepFee] is the fee switch (AC-006-23). */
data class StatusChange(
    val status: LessonStatus,
    /** When the student gave notice of a cancellation (AC-006-22). */
    val noticeAt: String? = null,
    val keepFee: Boolean = true,
)

/**
 * Sets a class's status inside the caller's transaction (AC-006-9 to 13, 22 to 27). Done freezes
 * the price and the travel; back to Agendada lets the value follow the price again; Cancelada and
 * Faltou store the fee from the class's rule, or that it was waived. Dada and Faltou wait for the
 * start.
 */
internal suspend fun applyStatus(
    ctx: Ctx,
    row: LessonRow,
    change: StatusChange,
): Applied {
    val s = change.status
    if ((s == LessonStatus.DONE || s == LessonStatus.MISSED) && row.startAt > ctx.now()) return Applied.Err(ScheduleError.NOT_STARTED)
    val cleared = row.copy(status = s.key, feeCents = null, feeWaived = false, noticeAt = null, updatedAt = ctx.now())
    return when (s) {
        LessonStatus.SCHEDULED -> Applied.Ok(cleared.copy(rateCents = null, rateType = null))
        LessonStatus.DONE -> {
            // A class already done keeps what it froze.
            if (row.status == LessonStatus.DONE.key) return Applied.Ok(cleared.copy(rateCents = row.rateCents, rateType = row.rateType))
            val lesson = ScheduleRepository(ctx.db).load(row) ?: return Applied.Err(ScheduleError.NOT_FOUND)
            val price = currentPrice(lesson.place, lesson.who)
            Applied.Ok(
                cleared.copy(
                    rateCents = price?.cents,
                    rateType = price?.rateType?.name,
                    travelFeeCents = travelFor(lesson.place, row.travelFeeCents),
                ),
            )
        }
        LessonStatus.CANCELLED, LessonStatus.MISSED -> {
            val lesson = ScheduleRepository(ctx.db).load(row) ?: return Applied.Err(ScheduleError.NOT_FOUND)
            val default = SettingsRepository(ctx.db).load().feeRule
            val rule = ruleFor((lesson.who as? Who.OfStudent)?.student?.feeChoice, lesson.place.feeChoice, default)
            val notice = if (s == LessonStatus.CANCELLED) change.noticeAt ?: ctx.now() else null
            val fee = suggestFee(rule, s, lesson.valueWithoutTravel(), row.startAt, notice) as? FeeSuggestion.Charge
            Applied.Ok(
                cleared.copy(
                    rateCents = null,
                    rateType = null,
                    noticeAt = notice,
                    feeCents = fee?.cents?.takeIf { change.keepFee },
                    feeWaived = fee != null && !change.keepFee,
                ),
            )
        }
    }
}

/** A class row with its new status, or why the status can't be set. */
internal sealed interface Applied {
    data class Ok(
        val value: LessonRow,
    ) : Applied

    data class Err(
        val error: ScheduleError,
    ) : Applied
}

/** Marking classes done and logging lessons given (spec 006). Every write runs in one transaction. */
class Lessons(
    private val ctx: Ctx,
) {
    private val db get() = ctx.db
    private val dao get() = db.schedule()

    /** Marcar como dada with the real minutes; a class of a series then stands on its own (AC-006-1, 2, 4). */
    suspend fun markDone(
        id: String,
        minutes: Int,
    ): ScheduleResult =
        db.inTransaction {
            val row = dao.lesson(id) ?: return@inTransaction ScheduleResult.Err(ScheduleError.NOT_FOUND)
            when (val r = applyStatus(ctx, row.copy(plannedMin = minutes), StatusChange(LessonStatus.DONE))) {
                is Applied.Err -> ScheduleResult.Err(r.error)
                is Applied.Ok -> {
                    dao.upsertLesson(r.value.copy(isDetached = row.ruleId != null))
                    ScheduleResult.Ok(id)
                }
            }
        }

    /**
     * Registrar aula dada: a one-off class that already happened, saved as Dada with its price
     * frozen. A date or start still ahead is refused (AC-006-6, 8).
     */
    suspend fun logLesson(draft: LessonDraft): ScheduleResult =
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
            when (val r = applyStatus(ctx, row, StatusChange(LessonStatus.DONE))) {
                is Applied.Err -> ScheduleResult.Err(r.error)
                is Applied.Ok -> {
                    dao.insertLessons(listOf(r.value))
                    ScheduleResult.Ok(row.id)
                }
            }
        }
}
