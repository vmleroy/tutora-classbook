package app.tutora.data

import androidx.room.deferredTransaction
import androidx.room.execSQL
import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLiteStatement
import app.tutora.domain.BACKUP_KEEP_DEFAULT
import app.tutora.domain.BackupEvery
import app.tutora.domain.BackupLogEntry
import app.tutora.domain.BackupRow
import app.tutora.domain.BackupState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** The settings keys that belong to this phone, not to the teacher's data. */
val DEVICE_KEYS =
    setOf(
        "backup_auto",
        "backup_every",
        "backup_keep",
        "backup_folder_uri",
        "backup_folder_name",
        "backup_enabled_at",
        "backup_written",
        "backup_log",
        "backup_failed",
        "last_backup_at",
        "restore_undo",
    )

/** How many backups Recentes lists (AC-011-11). */
const val BACKUP_LOG_SIZE = 5

/**
 * Reads and replaces the whole database, table by table, for the backup (spec 011), and keeps
 * this phone's backup state in its own settings keys.
 */
class BackupStore(
    private val db: TutoraDatabase,
) {
    private val json = Json

    /** Every table and its rows, read on one connection so the copy is consistent (AC-011-2). */
    suspend fun dump(): Map<String, List<BackupRow>> =
        db.useReaderConnection { c ->
            // One read transaction keeps every table at the same moment.
            c.deferredTransaction {
                val tables =
                    c
                        .usePrepared("SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name") { st ->
                            buildList { while (st.step()) add(st.getText(0)) }
                        }.filter(::isUserTable)
                tables.associateWith { t ->
                    val rows = c.usePrepared("SELECT * FROM `$t`") { st -> buildList { while (st.step()) add(st.row()) } }
                    if (t == "settings") rows.filter { (it["key"] as? JsonPrimitive)?.content !in DEVICE_KEYS } else rows
                }
            }
        }

    /**
     * Replaces every table with [tables] in one transaction: if anything fails, nothing changed
     * (AC-011-14). This phone's own keys stay. Columns the backup lacks take their defaults;
     * columns the table no longer has are left out.
     */
    suspend fun replaceAll(tables: Map<String, List<BackupRow>>) {
        db.useWriterConnection { w ->
            w.immediateTransaction {
                execSQL("PRAGMA defer_foreign_keys = ON")
                val names =
                    usePrepared("SELECT name FROM sqlite_master WHERE type = 'table'") { st ->
                        buildList { while (st.step()) add(st.getText(0)) }
                    }.filter(::isUserTable)
                for (t in names) {
                    if (t == "settings") {
                        execSQL("DELETE FROM settings WHERE key NOT IN (${DEVICE_KEYS.joinToString { "'$it'" }})")
                    } else {
                        execSQL("DELETE FROM `$t`")
                    }
                }
                for (t in names) {
                    val columns =
                        usePrepared("PRAGMA table_info(`$t`)") { st -> buildSet { while (st.step()) add(st.getText(1)) } }
                    for (row in tables[t].orEmpty()) {
                        if (t == "settings" && (row["key"] as? JsonPrimitive)?.content in DEVICE_KEYS) continue
                        val cols = row.keys.filter { it in columns }
                        if (cols.isEmpty()) continue
                        val sql =
                            "INSERT INTO `$t` (${cols.joinToString { "`$it`" }}) VALUES (${cols.joinToString { "?" }})"
                        usePrepared(sql) { st ->
                            cols.forEachIndexed { i, col -> st.bind(i + 1, row.getValue(col)) }
                            st.step()
                        }
                    }
                }
            }
        }
        // The rows changed under Room's DAOs; every screen reads them again.
        db.invalidationTracker.refreshAsync()
    }

    suspend fun firstLessonAt(): String? = db.backup().firstLessonAt()

    suspend fun liveCount(): Int = db.backup().liveCount()

    fun observe(): Flow<BackupState> = db.settings().observeAll().map { toState(it) }

    suspend fun load(): BackupState = toState(db.settings().all())

    suspend fun save(
        s: BackupState,
        now: String,
    ) = db.settings().upsert(rows(s, now))

    /** The defaults for a phone that never set up a backup. */
    suspend fun ensureDefaults(now: String) = db.settings().insertMissing(rows(BackupState(), now))

    private fun rows(
        s: BackupState,
        now: String,
    ): List<SettingRow> =
        listOf(
            "backup_auto" to JsonPrimitive(s.auto),
            "backup_every" to JsonPrimitive(s.every.key),
            "backup_keep" to JsonPrimitive(s.keep),
            "backup_folder_uri" to JsonPrimitive(s.folderUri),
            "backup_folder_name" to JsonPrimitive(s.folderName),
            "backup_enabled_at" to JsonPrimitive(s.enabledAt),
            "backup_written" to JsonArray(s.written.map(::JsonPrimitive)),
            "backup_log" to
                JsonArray(
                    s.log.map {
                        JsonObject(
                            mapOf(
                                "at" to JsonPrimitive(it.at),
                                "auto" to JsonPrimitive(it.auto),
                                "where" to JsonPrimitive(it.where),
                                "bytes" to JsonPrimitive(it.bytes),
                                "ok" to JsonPrimitive(it.ok),
                                "name" to JsonPrimitive(it.name),
                            ),
                        )
                    },
                ),
            "backup_failed" to JsonPrimitive(s.failed),
            "last_backup_at" to JsonPrimitive(s.lastBackupAt),
            "restore_undo" to JsonPrimitive(s.restoreUndo),
        ).map { (k, v) -> SettingRow(k, v.toString(), now) }

    private fun toState(rows: List<SettingRow>): BackupState {
        val v = rows.filter { it.key in DEVICE_KEYS }.associate { it.key to runCatching { json.parseToJsonElement(it.value) }.getOrNull() }

        fun text(k: String) = (v[k] as? JsonPrimitive)?.contentOrNull.orEmpty()

        fun bool(k: String) = (v[k] as? JsonPrimitive)?.booleanOrNull ?: false
        return BackupState(
            auto = bool("backup_auto"),
            every = BackupEvery.fromKey(text("backup_every")),
            keep = (v["backup_keep"] as? JsonPrimitive)?.intOrNull ?: BACKUP_KEEP_DEFAULT,
            folderUri = text("backup_folder_uri"),
            folderName = text("backup_folder_name"),
            enabledAt = text("backup_enabled_at"),
            written = (v["backup_written"] as? JsonArray)?.map { it.jsonPrimitive.content }.orEmpty(),
            log =
                (v["backup_log"] as? JsonArray)
                    ?.map { e ->
                        val o = e.jsonObject
                        BackupLogEntry(
                            o.getValue("at").jsonPrimitive.content,
                            o["auto"]?.jsonPrimitive?.booleanOrNull ?: false,
                            o["where"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                            o["bytes"]?.jsonPrimitive?.longOrNull ?: 0,
                            o["ok"]?.jsonPrimitive?.booleanOrNull ?: true,
                            o["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        )
                    }.orEmpty(),
            failed = bool("backup_failed"),
            lastBackupAt = text("last_backup_at"),
            restoreUndo = bool("restore_undo"),
        )
    }

    companion object {
        private fun isUserTable(name: String) = !name.startsWith("room_") && !name.startsWith("sqlite_") && name != "android_metadata"

        private fun SQLiteStatement.row(): BackupRow =
            (0 until getColumnCount()).associate { i ->
                getColumnName(i) to
                    when (getColumnType(i)) {
                        SQLITE_DATA_NULL -> JsonNull
                        SQLITE_DATA_INTEGER -> JsonPrimitive(getLong(i))
                        SQLITE_DATA_FLOAT -> JsonPrimitive(getDouble(i))
                        else -> JsonPrimitive(getText(i))
                    }
            }

        private fun SQLiteStatement.bind(
            index: Int,
            value: JsonElement,
        ) {
            val p = value as? JsonPrimitive
            when {
                p == null || p is JsonNull -> bindNull(index)
                p.isString -> bindText(index, p.content)
                p.content == "true" || p.content == "false" -> bindLong(index, if (p.content == "true") 1 else 0)
                p.longOrNull != null -> bindLong(index, p.content.toLong())
                else -> bindDouble(index, p.content.toDouble())
            }
        }
    }
}
