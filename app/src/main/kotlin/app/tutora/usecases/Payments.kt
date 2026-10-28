package app.tutora.usecases

import app.tutora.data.LessonRow
import app.tutora.data.PackageRow
import app.tutora.data.PaymentLessonRow
import app.tutora.data.PaymentRow
import app.tutora.data.ScheduleRepository
import app.tutora.data.inTransaction
import app.tutora.data.toStudent
import app.tutora.domain.Cents
import app.tutora.domain.LessonStatus
import app.tutora.domain.PackageState
import app.tutora.domain.PayMethod
import app.tutora.domain.PayerKind
import app.tutora.domain.PayerRef
import app.tutora.domain.RateType
import app.tutora.domain.Who
import app.tutora.domain.dueOf
import app.tutora.domain.isoText
import app.tutora.domain.localOf
import app.tutora.domain.localToInstant
import app.tutora.domain.payerOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * A row with who owes it, what it owes and the package it used, worked out from its status
 * (AC-008-1 to 3, 20, 22). Every write that changes a class's status or minutes passes through
 * here. Inside a transaction.
 */
internal suspend fun withDue(
    ctx: Ctx,
    row: LessonRow,
): LessonRow {
    val status = LessonStatus.fromKey(row.status)
    if (status == LessonStatus.SCHEDULED || status == LessonStatus.LIVE) return row.copy(payer = null, dueCents = null, packageId = null)
    val lesson = ScheduleRepository(ctx.db).load(row) ?: return row
    val dao = ctx.db.payments()
    val pkg =
        (lesson.who as? Who.OfStudent)?.let { dao.lastPackage(it.student.id) }?.let {
            PackageState(it.id, it.lessonsTotal, dao.packageUsed(it.id, except = row.id))
        }
    val due = dueOf(lesson, pkg, row.packageId?.takeIf { status == LessonStatus.DONE })
    return row.copy(payer = payerOf(lesson.place, lesson.who).kind.key, dueCents = due?.cents, packageId = due?.packageId)
}

data class PaymentDraft(
    val payer: PayerRef,
    val cents: Cents,
    val method: PayMethod,
    val date: LocalDate,
    val note: String? = null,
    /** The classes ticked in "Escolher aulas", which this payment pays first. */
    val chosen: List<String> = emptyList(),
)

enum class PaymentError { NOT_FOUND, AMOUNT, FUTURE_DATE, NOT_OWED, NO_PACKAGE }

sealed interface PaymentResult {
    data class Ok(
        val paymentId: String,
    ) : PaymentResult

    data class Err(
        val error: PaymentError,
    ) : PaymentResult
}

/** Recording, changing and removing payments, and buying packages (spec 008). Every write runs in one transaction. */
class Payments(
    private val ctx: Ctx,
) {
    private val db get() = ctx.db
    private val dao get() = db.payments()

    /** Today keeps the time it was recorded; an earlier day is noon of that day. */
    private fun paidAt(date: LocalDate): String {
        val today = localOf(ctx.now(), ctx.zone()).date
        return if (date == today) ctx.now() else localToInstant(date, LocalTime(12, 0), ctx.zone()).isoText()
    }

    private fun check(draft: PaymentDraft): PaymentError? {
        if (draft.cents <= 0) return PaymentError.AMOUNT
        if (draft.date > localOf(ctx.now(), ctx.zone()).date) return PaymentError.FUTURE_DATE
        return null
    }

    private fun PayerRef.row(
        id: String,
        draft: PaymentDraft,
        packageId: String?,
        createdAt: String,
    ) = PaymentRow(
        id = id,
        studentId = this.id.takeIf { kind == PayerKind.STUDENT },
        groupId = this.id.takeIf { kind == PayerKind.GROUP },
        placeId = this.id.takeIf { kind == PayerKind.PLACE },
        amountCents = draft.cents,
        method = draft.method.key,
        paidAt = paidAt(draft.date),
        note = draft.note?.trim()?.ifEmpty { null },
        packageId = packageId,
        createdAt = createdAt,
        updatedAt = ctx.now(),
    )

    /** Inside a transaction: the chosen classes must be owed by this payer (AC-008-7, 8). */
    private suspend fun checkChosen(draft: PaymentDraft): PaymentError? {
        val owed = dao.owed(draft.payer.kind.key, draft.payer.id).map { it.id }.toSet()
        return if (owed.containsAll(draft.chosen)) null else PaymentError.NOT_OWED
    }

    private suspend fun choose(
        paymentId: String,
        draft: PaymentDraft,
    ) {
        dao.clearChosen(paymentId)
        dao.insertChosen(draft.chosen.distinct().map { PaymentLessonRow(paymentId, it) })
    }

    /** Registrar pagamento / Receber da salinha (AC-008-5 to 8, 10). */
    suspend fun record(draft: PaymentDraft): PaymentResult =
        db.inTransaction {
            (check(draft) ?: checkChosen(draft))?.let { return@inTransaction PaymentResult.Err(it) }
            val id = ctx.newId()
            dao.upsertPayment(draft.payer.row(id, draft, null, ctx.now()))
            choose(id, draft)
            PaymentResult.Ok(id)
        }

    /** Editar: the balance follows the new amount, date or classes (AC-008-12). */
    suspend fun edit(
        id: String,
        draft: PaymentDraft,
    ): PaymentResult =
        db.inTransaction {
            val before = dao.payment(id) ?: return@inTransaction PaymentResult.Err(PaymentError.NOT_FOUND)
            (check(draft) ?: checkChosen(draft))?.let { return@inTransaction PaymentResult.Err(it) }
            dao.upsertPayment(draft.payer.row(id, draft, before.packageId, before.createdAt))
            choose(id, draft)
            PaymentResult.Ok(id)
        }

    /**
     * Excluir: the classes it paid go back to owing (AC-008-12). A package's payment takes the
     * package with it, and the classes that used it owe its price per class again.
     */
    suspend fun delete(id: String): PaymentResult =
        db.inTransaction {
            val row = dao.payment(id) ?: return@inTransaction PaymentResult.Err(PaymentError.NOT_FOUND)
            val packageId = row.packageId
            if (packageId == null) {
                dao.deletePayment(id)
                return@inTransaction PaymentResult.Ok(id)
            }
            val used = dao.packageLessons(packageId)
            // Deleting the package deletes its payment and frees its classes.
            dao.deletePackage(packageId)
            used.forEach { db.schedule().upsertLesson(withDue(ctx, it.copy(packageId = null))) }
            PaymentResult.Ok(id)
        }

    /** Novo pacote: the package at the student's current terms, and its payment (AC-008-20). */
    suspend fun buyPackage(
        studentId: String,
        method: PayMethod,
        date: LocalDate,
    ): PaymentResult =
        db.inTransaction {
            val student = db.students().get(studentId)?.toStudent() ?: return@inTransaction PaymentResult.Err(PaymentError.NOT_FOUND)
            val lessons = student.packageLessons?.takeIf { student.rateType == RateType.PACKAGE && it > 0 }
            val price = student.packagePriceCents?.takeIf { it > 0 }
            if (lessons == null || price == null) return@inTransaction PaymentResult.Err(PaymentError.NO_PACKAGE)
            val draft = PaymentDraft(PayerRef(PayerKind.STUDENT, studentId), price, method, date)
            check(draft)?.let { return@inTransaction PaymentResult.Err(it) }
            val packageId = ctx.newId()
            val at = paidAt(date)
            dao.insertPackage(PackageRow(packageId, studentId, lessons, price, at, ctx.now(), ctx.now()))
            val id = ctx.newId()
            dao.upsertPayment(draft.payer.row(id, draft, packageId, ctx.now()))
            PaymentResult.Ok(id)
        }

    /** The method the sheet starts at: the last one this payer used, or Pix (AC-008-5). */
    suspend fun lastMethod(payer: PayerRef): PayMethod = dao.lastMethod(payer.kind.key, payer.id)?.let(PayMethod::fromKey) ?: PayMethod.PIX

    /** Classes done or charged before spec 008 get their payer and what they owe, once. */
    suspend fun backfill() =
        db.inTransaction {
            dao.unsettled().forEach { db.schedule().upsertLesson(withDue(ctx, it)) }
        }
}
