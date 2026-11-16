package app.tutora.usecases

import app.tutora.data.CreditRow
import app.tutora.data.LessonRow
import app.tutora.data.LessonShareRow
import app.tutora.data.PackageRow
import app.tutora.data.PaymentLessonRow
import app.tutora.data.PaymentRepository
import app.tutora.data.PaymentRow
import app.tutora.data.ReceiptRow
import app.tutora.data.ScheduleRepository
import app.tutora.data.SettingsRepository
import app.tutora.data.inTransaction
import app.tutora.data.payerRef
import app.tutora.data.toStudent
import app.tutora.domain.Cents
import app.tutora.domain.LessonStatus
import app.tutora.domain.PackageState
import app.tutora.domain.PayMethod
import app.tutora.domain.PayerKind
import app.tutora.domain.PayerRef
import app.tutora.domain.RateType
import app.tutora.domain.Who
import app.tutora.domain.canSplit
import app.tutora.domain.dueOf
import app.tutora.domain.isoText
import app.tutora.domain.localOf
import app.tutora.domain.localToInstant
import app.tutora.domain.memberDues
import app.tutora.domain.minutesCover
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
    if (status == LessonStatus.SCHEDULED || status == LessonStatus.LIVE) {
        // Credit or minutes put on it go back to the payer (AC-008-30); a split class's shares go too.
        ctx.db.payments().deleteUsesOn(row.id)
        ctx.db.payments().deleteShares(row.id)
        return row.copy(payer = null, dueCents = null, packageId = null)
    }
    val lesson = ScheduleRepository(ctx.db).load(row) ?: return row
    val dao = ctx.db.payments()
    val pkg =
        (lesson.who as? Who.OfStudent)?.let { dao.lastPackage(it.student.id) }?.let {
            PackageState(it.id, it.lessonsTotal, dao.packageUsed(it.id, except = row.id))
        }
    val due = dueOf(lesson, pkg, row.packageId?.takeIf { status == LessonStatus.DONE })
    val payer = payerOf(lesson.place, lesson.who)
    dao.deleteShares(row.id)
    // A split group: each member owes their share and the group owes nothing for it (AC-020-34).
    val group = (lesson.who as? Who.OfGroup)?.group
    val since = group?.splitSince
    if (group != null &&
        since != null &&
        row.startAt >= since &&
        payer.kind == PayerKind.GROUP &&
        (due?.cents ?: 0) > 0 &&
        canSplit(group.memberIds.size, lesson.place.kind, lesson.place.payer)
    ) {
        val names =
            group.memberIds.associateWith {
                ctx.db
                    .students()
                    .get(it)
                    ?.name
                    .orEmpty()
            }
        val order = group.memberIds.sortedBy { foldName(names.getValue(it)) }
        val dues = memberDues(due?.cents ?: 0, order, group.shares)
        dao.insertShares(dues.filterValues { it > 0 }.map { (sid, cents) -> LessonShareRow(row.id, sid, cents) })
        return row.copy(payer = payer.kind.key, dueCents = 0, packageId = null)
    }
    return row.copy(payer = payer.kind.key, dueCents = due?.cents, packageId = due?.packageId)
}

/** A name without accents or capitals, so members sort as people read them. */
internal fun foldName(s: String) =
    java.text.Normalizer
        .normalize(s, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .lowercase()

data class PaymentDraft(
    val payer: PayerRef,
    val cents: Cents,
    val method: PayMethod,
    val date: LocalDate,
    val note: String? = null,
    /** The classes ticked in "Escolher aulas", which this payment pays first. */
    val chosen: List<String> = emptyList(),
    /** Its comprovantes, in order: those it keeps and files just picked (AC-021-1). */
    val receipts: List<ReceiptRef> = emptyList(),
)

/**
 * A payment just removed, with everything that went with it, so Desfazer can write it all back
 * (AC-021-6): its chosen classes, its comprovantes (whose files stay until the start-up sweep),
 * and a package's purchase with the classes that used it.
 */
data class DeletedPayment(
    val payment: PaymentRow,
    val chosen: List<PaymentLessonRow>,
    val receipts: List<ReceiptRow>,
    val pkg: PackageRow? = null,
    val packageLessons: List<String> = emptyList(),
)

enum class PaymentError { NOT_FOUND, AMOUNT, FUTURE_DATE, NOT_OWED, NO_PACKAGE, CREDITS_OFF, NO_CREDIT }

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
    private val rdao get() = db.receipts()
    private val receipts = Receipts(ctx)

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
        // A student's shares of split groups' classes count as theirs (AC-020-36).
        val shares = if (draft.payer.kind == PayerKind.STUDENT) dao.shares(draft.payer.id).map { it.lesson.id } else emptyList()
        val owed = dao.owed(draft.payer.kind.key, draft.payer.id).map { it.lesson.id }.toSet() + shares
        return if (owed.containsAll(draft.chosen)) null else PaymentError.NOT_OWED
    }

    private suspend fun choose(
        paymentId: String,
        draft: PaymentDraft,
    ) {
        dao.clearChosen(paymentId)
        dao.insertChosen(draft.chosen.distinct().map { PaymentLessonRow(paymentId, it) })
    }

    /**
     * Several students' payments at once, from a split group's "Registrar pagamento": all of them
     * or none (AC-020-40).
     */
    suspend fun recordAll(drafts: List<PaymentDraft>): PaymentResult {
        var refused: PaymentError? = null
        return runCatching {
            db.inTransaction {
                val ids =
                    drafts.map { d ->
                        when (val r = record(d)) {
                            is PaymentResult.Ok -> r.paymentId
                            is PaymentResult.Err -> {
                                refused = r.error
                                // Throwing undoes the payments already written in this transaction.
                                error("refused")
                            }
                        }
                    }
                PaymentResult.Ok(ids.joinToString(","))
            }
        }.getOrElse { PaymentResult.Err(refused ?: throw it) }
    }

    /** Registrar pagamento / Receber da salinha (AC-008-5 to 8, 10). */
    suspend fun record(draft: PaymentDraft): PaymentResult =
        db.inTransaction {
            check(draft)?.let { return@inTransaction PaymentResult.Err(it) }
            // A student's payment can also pay their classes a place owes: that part is
            // recorded as the place's payment, so the place's balance drops (AC-008-27).
            val elsewhere =
                if (draft.payer.kind == PayerKind.STUDENT) PaymentRepository(db).elsewhereNow(draft.payer.id) else emptyList()
            val foreign = elsewhere.filter { it.owed.lessonId in draft.chosen && it.left > 0 }
            val own = draft.copy(chosen = draft.chosen - elsewhere.map { it.owed.lessonId }.toSet())
            checkChosen(own)?.let { return@inTransaction PaymentResult.Err(it) }
            // Paying more than is owed is allowed: the rest is a tip, kept as received, never credit (AC-016-24).
            var remaining = draft.cents
            var first: String? = null
            for ((payer, classes) in foreign.groupBy { it.payer }) {
                val cents = minOf(remaining, classes.sumOf { it.left })
                if (cents <= 0) continue
                val id = ctx.newId()
                val part = PaymentDraft(payer, cents, draft.method, draft.date, draft.note, classes.map { it.owed.lessonId })
                dao.upsertPayment(payer.row(id, part, null, ctx.now()))
                choose(id, part)
                // One transfer that pays two payers: each payment keeps its own copy of the proof.
                attachNew(id, draft.receipts)
                remaining -= cents
                first = first ?: id
            }
            if (remaining > 0) {
                val id = ctx.newId()
                val rest = own.copy(cents = remaining)
                dao.upsertPayment(rest.payer.row(id, rest, null, ctx.now()))
                choose(id, rest)
                attachNew(id, draft.receipts)
                first = first ?: id
            }
            PaymentResult.Ok(first ?: "")
        }

    /** Editar: the balance follows the new amount, date or classes (AC-008-12). */
    suspend fun edit(
        id: String,
        draft: PaymentDraft,
    ): PaymentResult {
        val dropped = mutableListOf<String>()
        return db
            .inTransaction {
                val before = dao.payment(id) ?: return@inTransaction PaymentResult.Err(PaymentError.NOT_FOUND)
                (check(draft) ?: checkChosen(draft))?.let { return@inTransaction PaymentResult.Err(it) }
                dao.upsertPayment(draft.payer.row(id, draft, before.packageId, before.createdAt))
                choose(id, draft)
                dropped += setReceipts(id, draft.receipts)
                PaymentResult.Ok(id)
            }.also { if (it is PaymentResult.Ok) dropped.forEach(receipts.store::delete) }
    }

    /** Inside a transaction: rows for files just picked, after those the payment has. */
    private suspend fun attachNew(
        paymentId: String,
        refs: List<ReceiptRef>,
    ) {
        val start = rdao.forPayment(paymentId).maxOfOrNull { it.position + 1 } ?: 0
        rdao.insert(refs.filterIsInstance<ReceiptRef.New>().mapIndexed { i, r -> receipts.newRow(paymentId, r.staged, start + i) })
    }

    /**
     * Inside a transaction: the payment's comprovantes become exactly [refs], in that order.
     * Returns the files of those taken off, deleted once the transaction holds (AC-021-1).
     */
    private suspend fun setReceipts(
        paymentId: String,
        refs: List<ReceiptRef>,
    ): List<String> {
        val had = rdao.forPayment(paymentId).associateBy { it.id }
        val keep = refs.filterIsInstance<ReceiptRef.Existing>().map { it.id }.toSet()
        val gone = had.values.filter { it.id !in keep }
        gone.forEach { rdao.delete(it.id) }
        rdao.insert(
            refs.mapIndexedNotNull { i, r ->
                when (r) {
                    is ReceiptRef.Existing -> had[r.id]?.copy(position = i, updatedAt = ctx.now())
                    is ReceiptRef.New -> receipts.newRow(paymentId, r.staged, i)
                }
            },
        )
        return gone.map { it.file }
    }

    /**
     * Excluir: the classes it paid go back to owing (AC-008-12). A package's payment takes the
     * package with it, and the classes that used it owe its price per class again. What went is
     * returned, so Desfazer can bring it back with its comprovantes (AC-021-6).
     */
    suspend fun delete(id: String): DeletedPayment? =
        db.inTransaction {
            val row = dao.payment(id) ?: return@inTransaction null
            val chosen = dao.chosenOf(id)
            val receipts = rdao.forPayment(id)
            val packageId = row.packageId
            if (packageId == null) {
                dao.deletePayment(id)
                return@inTransaction DeletedPayment(row, chosen, receipts)
            }
            val pkg = dao.packageRow(packageId)
            val used = dao.packageLessons(packageId)
            // Deleting the package deletes its payment and frees its classes.
            dao.deletePackage(packageId)
            used.forEach { db.schedule().upsertLesson(withDue(ctx, it.copy(packageId = null))) }
            DeletedPayment(row, chosen, receipts, pkg, used.map { it.id })
        }

    /** Desfazer after Excluir: the payment, its classes, its comprovantes and its package, as they were (AC-021-6). */
    suspend fun restore(deleted: DeletedPayment) =
        db.inTransaction {
            deleted.pkg?.let { dao.insertPackage(it) }
            dao.upsertPayment(deleted.payment)
            dao.insertChosen(deleted.chosen)
            rdao.insert(deleted.receipts.filter { receipts.store.file(it.file).exists() })
            deleted.pkg?.let { pkg ->
                deleted.packageLessons.forEach { lid ->
                    db.schedule().lesson(lid)?.let { db.schedule().upsertLesson(withDue(ctx, it.copy(packageId = pkg.id))) }
                }
            }
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

    /**
     * Turns Créditos e bônus off: every bonus and every credit put on a class is deleted, after the
     * warning in Ajustes (AC-008-38). Money paid beyond what was owed then pays the next classes
     * by itself again.
     */
    suspend fun turnCreditsOff() =
        db.inTransaction {
            dao.deleteAllCredits()
            val settings = SettingsRepository(db)
            settings.save(settings.load().copy(credits = false), ctx.now())
        }

    /** A bonus is money or minutes, never both, and never nothing (AC-008-31). */
    private fun oneKind(
        cents: Cents,
        minutes: Int,
    ) = cents >= 0 && minutes >= 0 && (cents > 0) != (minutes > 0)

    /** Dar bônus: free money, free minutes or both, as credit (AC-008-31). */
    suspend fun giveBonus(
        payer: PayerRef,
        cents: Cents,
        minutes: Int,
        note: String? = null,
    ): PaymentResult =
        db.inTransaction {
            if (!SettingsRepository(db).load().credits) return@inTransaction PaymentResult.Err(PaymentError.CREDITS_OFF)
            if (!oneKind(cents, minutes)) return@inTransaction PaymentResult.Err(PaymentError.AMOUNT)
            val id = ctx.newId()
            dao.upsertCredit(creditRow(id, payer, "bonus", cents, minutes, null, note))
            PaymentResult.Ok(id)
        }

    /** Editar bônus: its money, minutes and note (AC-008-37). */
    suspend fun editBonus(
        id: String,
        cents: Cents,
        minutes: Int,
        note: String?,
    ): PaymentResult =
        db.inTransaction {
            val row = dao.credit(id)?.takeIf { it.kind == "bonus" } ?: return@inTransaction PaymentResult.Err(PaymentError.NOT_FOUND)
            if (!oneKind(cents, minutes)) return@inTransaction PaymentResult.Err(PaymentError.AMOUNT)
            dao.upsertCredit(row.copy(cents = cents, minutes = minutes, note = note?.trim()?.ifEmpty { null }, updatedAt = ctx.now()))
            settleCredit(row.payerRef())
            PaymentResult.Ok(id)
        }

    /**
     * Removes a bonus, or credit put on a class (which gives it back). Credit never goes below
     * zero: if a bonus already paid for classes, the newest of those uses go too (AC-008-37).
     */
    suspend fun removeCredit(id: String): PaymentResult =
        db.inTransaction {
            val row = dao.credit(id) ?: return@inTransaction PaymentResult.Err(PaymentError.NOT_FOUND)
            dao.deleteCredit(id)
            settleCredit(row.payerRef())
            PaymentResult.Ok(id)
        }

    /** Inside a transaction: drops the newest uses while more credit or minutes are used than there are. */
    private suspend fun settleCredit(payer: PayerRef) {
        val repo = PaymentRepository(db)
        var ledger = repo.ledgerNow(payer)
        val uses =
            dao
                .credits(payer.kind.key, payer.id)
                .filter { it.kind == "use" }
                .sortedByDescending { it.createdAt }
                .toMutableList()
        while (ledger.creditCents < 0 || ledger.creditMinutes < 0) {
            val drop = uses.firstOrNull { if (ledger.creditMinutes < 0) it.minutes > 0 else it.minutes == 0 } ?: break
            dao.deleteCredit(drop.id)
            uses.remove(drop)
            ledger = repo.ledgerNow(payer)
        }
    }

    /**
     * Usar crédito: puts the payer's money credit, or its bonus minutes, on the chosen classes,
     * oldest first, as far as it goes (AC-008-30, 31).
     */
    suspend fun useCredit(
        payer: PayerRef,
        lessonIds: List<String>,
        minutes: Boolean,
        /** At most this much money (cents) or this many minutes; null = as far as the credit goes (AC-008-39). */
        limit: Long? = null,
    ): PaymentResult =
        db.inTransaction {
            if (!SettingsRepository(db).load().credits) return@inTransaction PaymentResult.Err(PaymentError.CREDITS_OFF)
            val ledger = PaymentRepository(db).ledgerNow(payer)
            val chosen = ledger.owed.filter { it.lessonId in lessonIds && (ledger.allocation.left[it.lessonId] ?: 0) > 0 }
            if (chosen.isEmpty()) return@inTransaction PaymentResult.Err(PaymentError.NOT_OWED)
            var money = limit?.let { minOf(it, ledger.creditCents) } ?: ledger.creditCents
            var mins = limit?.let { minOf(it.toInt(), ledger.creditMinutes) } ?: ledger.creditMinutes
            if ((if (minutes) mins.toLong() else money) <= 0) return@inTransaction PaymentResult.Err(PaymentError.NO_CREDIT)
            var first: String? = null
            for (c in chosen.sortedBy { it.startAt }) {
                val left = ledger.allocation.left[c.lessonId] ?: 0
                val row = db.schedule().lesson(c.lessonId) ?: continue
                val (cents, used) =
                    if (minutes) {
                        val lesson = ScheduleRepository(db).load(row) ?: continue
                        val take = minOf(mins, row.plannedMin)
                        minutesCover(lesson.frozenRate, row.plannedMin, take, left) to take
                    } else {
                        minOf(money, left) to 0
                    }
                if (cents <= 0) continue
                val id = ctx.newId()
                dao.upsertCredit(creditRow(id, payer, "use", cents, used, c.lessonId, null))
                if (minutes) mins -= used else money -= cents
                first = first ?: id
                if ((if (minutes) mins.toLong() else money) <= 0) break
            }
            first?.let { PaymentResult.Ok(it) } ?: PaymentResult.Err(PaymentError.NO_CREDIT)
        }

    private fun creditRow(
        id: String,
        payer: PayerRef,
        kind: String,
        cents: Cents,
        minutes: Int,
        lessonId: String?,
        note: String?,
    ) = CreditRow(
        id,
        payer.id.takeIf { payer.kind == PayerKind.STUDENT },
        payer.id.takeIf { payer.kind == PayerKind.GROUP },
        payer.id.takeIf { payer.kind == PayerKind.PLACE },
        kind,
        cents,
        minutes,
        lessonId,
        note?.trim()?.ifEmpty { null },
        ctx.now(),
        ctx.now(),
    )

    /** The method the sheet starts at: the last one this payer used, or Pix (AC-008-5). */
    suspend fun lastMethod(payer: PayerRef): PayMethod = dao.lastMethod(payer.kind.key, payer.id)?.let(PayMethod::fromKey) ?: PayMethod.PIX

    /** Classes done or charged before spec 008 get their payer and what they owe, once. */
    suspend fun backfill() =
        db.inTransaction {
            dao.unsettled().forEach { db.schedule().upsertLesson(withDue(ctx, it)) }
        }
}
