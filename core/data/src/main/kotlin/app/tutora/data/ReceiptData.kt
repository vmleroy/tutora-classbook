package app.tutora.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import app.tutora.domain.Receipt
import app.tutora.domain.ReceiptKind
import app.tutora.domain.ReceiptUsage
import app.tutora.domain.receiptUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * A comprovante on a payment (spec 021). The file lives in Tutora's own storage under [file];
 * removing the payment removes the row, and the start-up sweep removes the file once nothing
 * points to it, so Desfazer can bring both back first (AC-021-6).
 */
@Entity(
    tableName = "payment_receipt",
    foreignKeys = [ForeignKey(PaymentRow::class, ["id"], ["payment_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("payment_id")],
)
data class ReceiptRow(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "payment_id") val paymentId: String,
    val kind: String,
    val mime: String,
    val file: String,
    val bytes: Long,
    val position: Int,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
)

fun ReceiptRow.toReceipt() = Receipt(id, paymentId, ReceiptKind.fromKey(kind) ?: ReceiptKind.IMAGE, mime, file, bytes, position)

data class ReceiptCountRow(
    @ColumnInfo(name = "payment_id") val paymentId: String,
    val n: Int,
)

data class ReceiptTotalRow(
    val n: Int,
    val total: Long,
)

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM payment_receipt WHERE payment_id = :paymentId ORDER BY position, created_at")
    fun observeFor(paymentId: String): Flow<List<ReceiptRow>>

    @Query("SELECT * FROM payment_receipt WHERE payment_id = :paymentId ORDER BY position, created_at")
    suspend fun forPayment(paymentId: String): List<ReceiptRow>

    @Query("SELECT * FROM payment_receipt WHERE id = :id")
    suspend fun get(id: String): ReceiptRow?

    @Query("SELECT * FROM payment_receipt ORDER BY payment_id, position")
    fun observeAll(): Flow<List<ReceiptRow>>

    @Query("SELECT payment_id, COUNT(*) AS n FROM payment_receipt GROUP BY payment_id")
    fun observeCounts(): Flow<List<ReceiptCountRow>>

    @Query("SELECT COUNT(*) AS n, COALESCE(SUM(bytes), 0) AS total FROM payment_receipt")
    fun observeTotal(): Flow<ReceiptTotalRow>

    @Query("SELECT file FROM payment_receipt")
    suspend fun files(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rows: List<ReceiptRow>)

    @Query("DELETE FROM payment_receipt WHERE id = :id")
    suspend fun delete(id: String)
}

/** Comprovantes as Flows; Room re-emits after any write. */
class ReceiptRepository(
    private val db: TutoraDatabase,
) {
    private val dao get() = db.receipts()

    fun forPayment(paymentId: String): Flow<List<Receipt>> = dao.observeFor(paymentId).map { rows -> rows.map { it.toReceipt() } }

    /** Every comprovante by payment, for the documents (AC-021-7). */
    fun byPayment(): Flow<Map<String, List<Receipt>>> =
        dao.observeAll().map { rows ->
            rows.map { it.toReceipt() }.groupBy { it.paymentId }
        }

    /** How many each payment holds, for the mark on its row (AC-021-4). */
    fun counts(): Flow<Map<String, Int>> = dao.observeCounts().map { rows -> rows.associate { it.paymentId to it.n } }

    /** "12 comprovantes · 8,4 MB" (AC-021-12). */
    fun usage(): Flow<ReceiptUsage> = dao.observeTotal().map { receiptUsage(it.n, it.total) }
}
