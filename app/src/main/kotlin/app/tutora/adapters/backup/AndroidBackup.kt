package app.tutora.adapters.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.tutora.TutoraApplication
import app.tutora.domain.instantOf
import app.tutora.usecases.BackupFolder
import app.tutora.usecases.BackupScheduler
import java.util.concurrent.TimeUnit

// Automatic backups to a folder the teacher picked once (spec 011): the Storage Access Framework
// reaches Google Drive, OneDrive or a phone folder with no sign-in, and WorkManager wakes the app
// at 20:00 even when it is closed.

class AndroidBackupFolder(
    private val context: Context,
) : BackupFolder {
    private val resolver get() = context.contentResolver

    private fun folderDoc(tree: Uri) = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))

    override fun adopt(uri: String): String {
        val tree = uri.toUri()
        resolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        return resolver
            .query(folderDoc(tree), arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
            .orEmpty()
    }

    override fun read(uri: String): ByteArray? = runCatching { resolver.openInputStream(uri.toUri())?.use { it.readBytes() } }.getOrNull()

    override fun write(
        uri: String,
        name: String,
        bytes: ByteArray,
    ): Long {
        val tree = uri.toUri()
        val doc =
            checkNotNull(DocumentsContract.createDocument(resolver, folderDoc(tree), "application/json", name)) { "Folder refused $name" }
        checkNotNull(resolver.openOutputStream(doc, "wt")) { "Can't write $name" }.use { it.write(bytes) }
        return bytes.size.toLong()
    }

    /** The document of the file called [name] in the folder, if it is there. */
    private fun find(
        tree: Uri,
        name: String,
    ): Uri? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val id =
            resolver.query(children, columns, null, null, null)?.use { c ->
                generateSequence { if (c.moveToNext()) c.getString(0) to c.getString(1) else null }.firstOrNull { it.second == name }?.first
            } ?: return null
        return DocumentsContract.buildDocumentUriUsingTree(tree, id)
    }

    override fun read(
        uri: String,
        name: String,
    ): ByteArray? = find(uri.toUri(), name)?.let { doc -> resolver.openInputStream(doc)?.use { it.readBytes() } }

    override fun delete(
        uri: String,
        name: String,
    ) {
        val doc = find(uri.toUri(), name) ?: return
        DocumentsContract.deleteDocument(resolver, doc)
    }
}

class WorkManagerBackupScheduler(
    private val context: Context,
) : BackupScheduler {
    override fun plan(at: String?) {
        val work = WorkManager.getInstance(context)
        if (at == null) {
            work.cancelUniqueWork(WORK)
            return
        }
        val delay = (instantOf(at).toEpochMilliseconds() - System.currentTimeMillis()).coerceAtLeast(0)
        work.enqueueUniqueWork(
            WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<BackupWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build(),
        )
    }

    companion object {
        const val WORK = "auto-backup"
    }
}

/** The 20:00 run: writes the backup if it is due, then plans the next (AC-011-6, 8). */
class BackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        (applicationContext as TutoraApplication).container.backups.runIfDue()
        return Result.success()
    }
}
