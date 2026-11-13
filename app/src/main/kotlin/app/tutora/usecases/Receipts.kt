package app.tutora.usecases

import app.tutora.data.ReceiptRow
import app.tutora.data.inTransaction
import app.tutora.domain.ReceiptKind
import java.io.File

/** Why a picked or shared file was not taken (AC-021-2). */
enum class ReceiptRefusal { UNSUPPORTED, TOO_LARGE, UNREADABLE }

/** A file picked or shared, already copied into Tutora's pending folder, ready to go on a payment. */
data class StagedReceipt(
    /** Its name in the pending folder. */
    val name: String,
    val mime: String,
    val bytes: Long,
)

sealed interface StageResult {
    data class Ok(
        val staged: StagedReceipt,
    ) : StageResult

    data class Refused(
        val reason: ReceiptRefusal,
    ) : StageResult
}

/**
 * The phone's side of comprovantes (spec 021): reading a picked or shared file and copying it
 * into Tutora's own storage, a large photo reduced first (AC-021-2). The rest is plain files.
 */
interface ReceiptFiles {
    /** Copies the file at [uri] into [dir] under [name] (plus its extension); refuses what can't be a comprovante. */
    fun stage(
        uri: String,
        dir: File,
        name: String,
    ): StageResult

    object None : ReceiptFiles {
        override fun stage(
            uri: String,
            dir: File,
            name: String,
        ): StageResult = StageResult.Refused(ReceiptRefusal.UNREADABLE)
    }
}

/** A comprovante on a payment being saved: one it already has, or a file just staged. */
sealed interface ReceiptRef {
    data class Existing(
        val id: String,
    ) : ReceiptRef

    data class New(
        val staged: StagedReceipt,
    ) : ReceiptRef
}

/** The extension a kept file gets from its type. */
fun receiptExtension(mime: String): String =
    when (mime) {
        "application/pdf" -> "pdf"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/heic", "image/heif" -> "heic"
        else -> "jpg"
    }

/**
 * Tutora's comprovantes folder: `comprovantes/<id>.<ext>` for kept files, `comprovantes/pending/`
 * for files picked but not saved yet. Nothing here is ever exposed to other apps directly.
 */
class ReceiptStore(
    privateDir: File,
) {
    val dir = File(privateDir, RECEIPT_DIR)
    val pending = File(dir, "pending")

    fun file(name: String) = File(dir, name)

    /** Copies a staged file to its kept name; the staged one stays, since one share can go on several payments. */
    fun keep(
        staged: StagedReceipt,
        id: String,
    ): String {
        val name = "$id.${receiptExtension(staged.mime)}"
        dir.mkdirs()
        File(pending, staged.name).copyTo(File(dir, name), overwrite = true)
        return name
    }

    fun delete(name: String) {
        File(dir, name).delete()
    }

    /**
     * Deletes kept files no row points to (an Excluir not undone, a payer removed) and staged
     * files older than a day (a sheet closed without saving). A file shared a moment ago stays.
     */
    fun sweep(
        rows: Set<String>,
        nowMs: Long,
    ) {
        dir.listFiles()?.filter { it.isFile && it.name !in rows }?.forEach { it.delete() }
        pending.listFiles()?.filter { nowMs - it.lastModified() > DAY_MS }?.forEach { it.delete() }
    }

    companion object {
        const val RECEIPT_DIR = "comprovantes"
        private const val DAY_MS = 24L * 60 * 60 * 1000
    }
}

/** Anexar comprovante, the viewer's Substituir and Remover, and the start-up sweep (spec 021). */
class Receipts(
    private val ctx: Ctx,
) {
    private val db get() = ctx.db
    private val dao get() = db.receipts()
    val store = ReceiptStore(ctx.privateDir)

    /** Copies a picked or shared file into Tutora at once: a content link doesn't last (AC-021-2, 3). */
    fun stage(uri: String): StageResult {
        store.pending.mkdirs()
        return ctx.receiptFiles.stage(uri, store.pending, ctx.newId())
    }

    /** Anexar comprovante: the files go on the payment, after any it already has (AC-021-3). */
    suspend fun attach(
        paymentId: String,
        staged: List<StagedReceipt>,
    ): Boolean =
        db.inTransaction {
            db.payments().payment(paymentId) ?: return@inTransaction false
            val start = dao.forPayment(paymentId).maxOfOrNull { it.position + 1 } ?: 0
            dao.insert(staged.mapIndexed { i, s -> newRow(paymentId, s, start + i) })
            true
        }

    /** Substituir: a new file in the same place on the payment (AC-021-5). */
    suspend fun replace(
        id: String,
        staged: StagedReceipt,
    ): Boolean {
        val old =
            db.inTransaction {
                val row = dao.get(id) ?: return@inTransaction null
                dao.delete(id)
                dao.insert(listOf(newRow(row.paymentId, staged, row.position)))
                row
            } ?: return false
        store.delete(old.file)
        return true
    }

    /** Remover, after the teacher confirms (AC-021-5). */
    suspend fun remove(id: String): Boolean {
        val row = db.inTransaction { dao.get(id)?.also { dao.delete(id) } } ?: return false
        store.delete(row.file)
        return true
    }

    /** At start-up: files nothing points to any more go (AC-021-6). */
    suspend fun sweep() {
        store.sweep(dao.files().toSet(), System.currentTimeMillis())
    }

    /** Inside a transaction: a row for a staged file, its copy kept under the row's id. */
    internal fun newRow(
        paymentId: String,
        staged: StagedReceipt,
        position: Int,
    ): ReceiptRow {
        val id = ctx.newId()
        val name = store.keep(staged, id)
        return ReceiptRow(
            id,
            paymentId,
            (ReceiptKind.ofMime(staged.mime) ?: ReceiptKind.IMAGE).key,
            staged.mime,
            name,
            staged.bytes,
            position,
            ctx.now(),
            ctx.now(),
        )
    }
}
