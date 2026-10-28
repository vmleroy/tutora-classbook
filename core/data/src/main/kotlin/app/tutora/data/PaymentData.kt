package app.tutora.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import app.tutora.domain.Cents
import app.tutora.domain.Ledger
import app.tutora.domain.OwedClass
import app.tutora.domain.OwedElsewhere
import app.tutora.domain.PackageState
import app.tutora.domain.PayMethod
import app.tutora.domain.PayerKind
import app.tutora.domain.PayerRef
import app.tutora.domain.PaymentRecord
import app.tutora.domain.ledgerOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** A student's class owed by the place, which pays the teacher. */
data class PlacePaidRow(
    val id: String,
    @ColumnInfo(name = "place_id") val placeId: String,
)

/** What one payer owes and has paid, summed. */
data class BalanceRow(
    val payer: String,
    @ColumnInfo(name = "payer_id") val payerId: String,
    val due: Long,
    val paid: Long,
)

// The payer's id column, matched against the payer's kind.
private const val LESSON_PAYER =
    "lesson.payer = :kind AND CASE lesson.payer WHEN 'student' THEN lesson.student_id " +
        "WHEN 'group' THEN lesson.group_id ELSE lesson.place_id END = :id"
private const val PAYMENT_PAYER =
    "((:kind = 'student' AND payment.student_id = :id) OR (:kind = 'group' AND payment.group_id = :id) " +
        "OR (:kind = 'place' AND payment.place_id = :id))"

@Dao
interface PaymentDao {
    /** Every payer's owed and paid totals; a package's purchase pays no class, so it is left out. */
    @Query(
        """SELECT payer, payer_id, SUM(due) AS due, SUM(paid) AS paid FROM (
             SELECT payer, CASE payer WHEN 'student' THEN student_id WHEN 'group' THEN group_id ELSE place_id END AS payer_id,
               due_cents AS due, 0 AS paid FROM lesson WHERE due_cents IS NOT NULL AND payer IS NOT NULL
             UNION ALL
             SELECT CASE WHEN student_id IS NOT NULL THEN 'student' WHEN group_id IS NOT NULL THEN 'group' ELSE 'place' END,
               COALESCE(student_id, group_id, place_id), 0, amount_cents FROM payment WHERE package_id IS NULL
           ) GROUP BY payer, payer_id""",
    )
    fun observeBalances(): Flow<List<BalanceRow>>

    @Query("SELECT * FROM lesson WHERE $LESSON_PAYER AND due_cents IS NOT NULL AND due_cents > 0 ORDER BY start_at")
    fun observeOwed(
        kind: String,
        id: String,
    ): Flow<List<LessonRow>>

    @Query("SELECT * FROM lesson WHERE $LESSON_PAYER AND due_cents IS NOT NULL AND due_cents > 0")
    suspend fun owed(
        kind: String,
        id: String,
    ): List<LessonRow>

    @Query("SELECT * FROM payment WHERE $PAYMENT_PAYER ORDER BY paid_at DESC")
    fun observePayments(
        kind: String,
        id: String,
    ): Flow<List<PaymentRow>>

    @Query("SELECT * FROM payment WHERE $PAYMENT_PAYER")
    suspend fun payments(
        kind: String,
        id: String,
    ): List<PaymentRow>

    @Query("SELECT payment_lesson.* FROM payment_lesson JOIN payment ON payment.id = payment_lesson.payment_id WHERE $PAYMENT_PAYER")
    fun observeChosen(
        kind: String,
        id: String,
    ): Flow<List<PaymentLessonRow>>

    @Query("SELECT payment_lesson.* FROM payment_lesson JOIN payment ON payment.id = payment_lesson.payment_id WHERE $PAYMENT_PAYER")
    suspend fun chosen(
        kind: String,
        id: String,
    ): List<PaymentLessonRow>

    @Query("SELECT method FROM payment WHERE $PAYMENT_PAYER ORDER BY paid_at DESC LIMIT 1")
    suspend fun lastMethod(
        kind: String,
        id: String,
    ): String?

    @Query("SELECT id, place_id FROM lesson WHERE student_id = :studentId AND payer = 'place' AND due_cents > 0")
    fun observePaidByPlace(studentId: String): Flow<List<PlacePaidRow>>

    @Query("SELECT id, place_id FROM lesson WHERE student_id = :studentId AND payer = 'place' AND due_cents > 0")
    suspend fun paidByPlace(studentId: String): List<PlacePaidRow>

    @Query("SELECT * FROM payment WHERE id = :id")
    suspend fun payment(id: String): PaymentRow?

    @Query("SELECT * FROM payment WHERE id = :id")
    fun observePayment(id: String): Flow<PaymentRow?>

    @Upsert
    suspend fun upsertPayment(row: PaymentRow)

    @Query("DELETE FROM payment WHERE id = :id")
    suspend fun deletePayment(id: String)

    @Query("DELETE FROM payment_lesson WHERE payment_id = :paymentId")
    suspend fun clearChosen(paymentId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChosen(rows: List<PaymentLessonRow>)

    @Insert
    suspend fun insertPackage(row: PackageRow)

    @Query("DELETE FROM package WHERE id = :id")
    suspend fun deletePackage(id: String)

    @Query("SELECT * FROM package WHERE student_id = :studentId ORDER BY bought_at DESC, id DESC LIMIT 1")
    suspend fun lastPackage(studentId: String): PackageRow?

    @Query("SELECT * FROM package WHERE student_id = :studentId ORDER BY bought_at DESC, id DESC LIMIT 1")
    fun observeLastPackage(studentId: String): Flow<PackageRow?>

    @Query("SELECT COUNT(*) FROM lesson WHERE package_id = :packageId AND id != :except")
    suspend fun packageUsed(
        packageId: String,
        except: String = "",
    ): Int

    @Query("SELECT COUNT(*) FROM lesson WHERE package_id = :packageId")
    fun observePackageUsed(packageId: String): Flow<Int>

    @Query("SELECT * FROM lesson WHERE package_id = :packageId")
    suspend fun packageLessons(packageId: String): List<LessonRow>

    /** Classes done, cancelled or missed before spec 008, still without a payer. */
    @Query("SELECT * FROM lesson WHERE status IN ('done', 'cancelled', 'missed') AND payer IS NULL")
    suspend fun unsettled(): List<LessonRow>
}

fun PaymentRow.payerRef(): PayerRef =
    when {
        studentId != null -> PayerRef(PayerKind.STUDENT, studentId)
        groupId != null -> PayerRef(PayerKind.GROUP, groupId)
        else -> PayerRef(PayerKind.PLACE, placeId.orEmpty())
    }

fun PaymentRow.toPayment(chosen: List<String>) =
    PaymentRecord(id, payerRef(), amountCents, PayMethod.fromKey(method) ?: PayMethod.PIX, paidAt, note, packageId, chosen)

fun LessonRow.toOwed() = OwedClass(id, startAt, plannedMin, dueCents ?: 0)

fun ledgerOf(
    owed: List<LessonRow>,
    payments: List<PaymentRow>,
    chosen: List<PaymentLessonRow>,
): Ledger {
    val byPayment = chosen.groupBy({ it.paymentId }, { it.lessonId })
    return ledgerOf(owed.map { it.toOwed() }, payments.map { it.toPayment(byPayment[it.id].orEmpty()) })
}

/** Balances, ledgers and packages as Flows; Room re-emits after any write (AC-008-10). */
class PaymentRepository(
    private val db: TutoraDatabase,
) {
    private val dao get() = db.payments()

    fun balances(): Flow<Map<PayerRef, Cents>> =
        dao.observeBalances().map { rows ->
            rows.mapNotNull { r -> PayerKind.fromKey(r.payer)?.let { PayerRef(it, r.payerId) to r.due - r.paid } }.toMap()
        }

    fun ledger(payer: PayerRef): Flow<Ledger> =
        combine(
            dao.observeOwed(payer.kind.key, payer.id),
            dao.observePayments(payer.kind.key, payer.id),
            dao.observeChosen(payer.kind.key, payer.id),
        ) { owed, payments, chosen -> ledgerOf(owed, payments, chosen) }

    suspend fun ledgerNow(payer: PayerRef): Ledger =
        ledgerOf(dao.owed(payer.kind.key, payer.id), dao.payments(payer.kind.key, payer.id), dao.chosen(payer.kind.key, payer.id))

    fun payment(id: String): Flow<PaymentRecord?> = dao.observePayment(id).map { it?.toPayment(emptyList()) }

    /** The student's latest package and how many classes have used it. */
    fun lastPackage(studentId: String): Flow<PackageState?> =
        // The balances read the lesson table, so the count is read again whenever a class changes.
        combine(dao.observeLastPackage(studentId), dao.observeBalances()) { row, _ -> row }
            .map { row -> row?.let { PackageState(it.id, it.lessonsTotal, dao.packageUsed(it.id)) } }

    /** A student's classes that their places owe instead of them, by place id. */
    fun paidByPlace(studentId: String): Flow<Map<String, List<String>>> =
        dao.observePaidByPlace(studentId).map { rows -> rows.groupBy({ it.placeId }, { it.id }) }

    /**
     * A student's classes that their places owe, with what each place still owes on them. The
     * balances read the lesson and payment tables, so this is read again after any change.
     */
    fun elsewhere(studentId: String): Flow<List<OwedElsewhere>> = dao.observeBalances().map { elsewhereNow(studentId) }

    suspend fun elsewhereNow(studentId: String): List<OwedElsewhere> =
        dao.paidByPlace(studentId).groupBy({ it.placeId }, { it.id }).flatMap { (placeId, ids) ->
            val payer = PayerRef(PayerKind.PLACE, placeId)
            val ledger = ledgerNow(payer)
            ledger.owed.filter { it.lessonId in ids }.map { OwedElsewhere(it, ledger.allocation.left[it.lessonId] ?: 0, payer) }
        }

    suspend fun lastMethod(payer: PayerRef): PayMethod? = dao.lastMethod(payer.kind.key, payer.id)?.let(PayMethod::fromKey)
}
