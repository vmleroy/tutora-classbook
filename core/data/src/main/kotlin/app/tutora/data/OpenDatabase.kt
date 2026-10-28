package app.tutora.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.Dispatchers
import java.io.File

/** A migration failed; the copy taken before it is at [backupPath]. */
class MigrationFailed(
    val backupPath: String?,
    cause: Throwable,
) : Exception("Migration failed: ${cause.message}", cause)

/**
 * Opens the database: copies an older file aside first (constitution rule 3), then lets Room
 * run the pending migrations, with WAL and foreign keys on.
 */
fun openTutoraDatabase(
    context: Context,
    file: File = context.getDatabasePath("tutora.db"),
    driver: SQLiteDriver = BundledSQLiteDriver(),
): Pair<TutoraDatabase, String?> {
    file.parentFile?.mkdirs()
    val backup = PreMigrationBackup(driver).copyIfOlder(file, TutoraDatabase.VERSION)
    val db =
        Room
            .databaseBuilder<TutoraDatabase>(context, file.absolutePath)
            .setDriver(driver)
            .setQueryCoroutineContext(Dispatchers.IO)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(*TutoraDatabase.MIGRATIONS)
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onOpen(connection: SQLiteConnection) {
                        connection.execSQL("PRAGMA foreign_keys = ON")
                    }
                },
            ).build()
    return db to backup
}

class PreMigrationBackup(
    private val driver: SQLiteDriver = BundledSQLiteDriver(),
) {
    /** The schema version stored in the file's `user_version`, or 0 for a new file. */
    fun versionOf(file: File): Int {
        if (!file.exists()) return 0
        return driver.open(file.absolutePath).use { c ->
            c.prepare("PRAGMA user_version").use {
                it.step()
                it.getLong(0).toInt()
            }
        }
    }

    /**
     * Writes a consistent copy to `tutora-pre-v{target}.db` before an upgrade. A copy already
     * there comes from an earlier failed attempt and holds the older data, so it is kept.
     */
    fun copyIfOlder(
        file: File,
        target: Int,
    ): String? {
        val from = versionOf(file)
        if (from == 0 || from >= target) return null
        val copy = File(file.parentFile, "tutora-pre-v$target.db")
        if (!copy.exists()) {
            driver.open(file.absolutePath).use { c ->
                c.execSQL("VACUUM INTO '${copy.absolutePath.replace("'", "''")}'")
            }
        }
        return copy.absolutePath
    }
}

private inline fun <T> SQLiteConnection.use(block: (SQLiteConnection) -> T): T =
    try {
        block(this)
    } finally {
        close()
    }
