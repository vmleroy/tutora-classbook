package app.tutora.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlin.time.Instant

// The full backup (spec 011): one JSON file with every table, so a later Tutora can read an
// older one (architecture, backup format). The rows are kept as JSON values, column by column.

/** A row: column name to its value (a string, a number or null). */
typealias BackupRow = Map<String, JsonElement>

data class BackupFile(
    val app: String,
    val schemaVersion: Int,
    val exportedAt: String,
    val tables: Map<String, List<BackupRow>>,
)

const val BACKUP_APP = "tutora"

sealed interface BackupRead {
    data class Ok(
        val file: BackupFile,
    ) : BackupRead

    /** Not JSON, or not a Tutora backup: a spreadsheet, a photo, a damaged file (AC-011-12, 20). */
    data object NotTutora : BackupRead

    /** Made by a newer Tutora than this one (AC-011-15). */
    data class Newer(
        val version: Int,
    ) : BackupRead
}

private val json = Json { prettyPrint = false }

fun encodeBackup(file: BackupFile): String =
    json.encodeToString(
        JsonElement.serializer(),
        JsonObject(
            mapOf(
                "app" to JsonPrimitive(file.app),
                "schemaVersion" to JsonPrimitive(file.schemaVersion),
                "exportedAt" to JsonPrimitive(file.exportedAt),
                "tables" to JsonObject(file.tables.mapValues { (_, rows) -> JsonArray(rows.map { JsonObject(it) }) }),
            ),
        ),
    )

/** Reads a picked file; anything that isn't a backup this app can restore says why (AC-011-12, 15). */
fun readBackup(
    bytes: ByteArray,
    current: Int,
): BackupRead {
    val root = runCatching { json.parseToJsonElement(bytes.decodeToString()) as? JsonObject }.getOrNull() ?: return BackupRead.NotTutora
    val app = (root["app"] as? JsonPrimitive)?.takeIf { it.isString }?.content
    val version = (root["schemaVersion"] as? JsonPrimitive)?.intOrNull
    val tables = root["tables"] as? JsonObject
    if (app != BACKUP_APP || version == null || version < 1 || tables == null) return BackupRead.NotTutora
    if (version > current) return BackupRead.Newer(version)
    val parsed =
        runCatching {
            tables.mapValues { (_, rows) -> (rows as JsonArray).map { (it as JsonObject).toMap() } }
        }.getOrNull() ?: return BackupRead.NotTutora
    val exportedAt = (root["exportedAt"] as? JsonPrimitive)?.content.orEmpty()
    return BackupRead.Ok(upgradeBackup(BackupFile(app, version, exportedAt, parsed), current))
}

/**
 * One step per schema version, mirroring the database migrations: each brings a backup from
 * version n to n + 1. So far each only added tables that start empty or columns that may be null,
 * which the restore fills with their defaults, so the steps only add the empty tables.
 */
val backupUpgrades: Map<Int, (Map<String, List<BackupRow>>) -> Map<String, List<BackupRow>>> =
    mapOf(
        // 006: a done class's price and a cancelled class's charge, all nullable columns.
        1 to { t -> t },
        // 007: the live-class clock.
        2 to { t -> t + ("lesson_timer" to t["lesson_timer"].orEmpty()) },
        // 008: payments and packages.
        3 to { t ->
            t + listOf("package", "payment", "payment_lesson").associateWith { t[it].orEmpty() }
        },
        // 008: bonuses and credit.
        4 to { t -> t + ("credit" to t["credit"].orEmpty()) },
    )

fun upgradeBackup(
    file: BackupFile,
    to: Int,
): BackupFile {
    var tables = file.tables
    for (v in file.schemaVersion until to) {
        val step = checkNotNull(backupUpgrades[v]) { "No backup step from version $v" }
        tables = step(tables)
    }
    return file.copy(schemaVersion = to, tables = tables)
}

/** What the confirmation says the backup holds (AC-011-13). */
data class BackupSummary(
    val exportedAt: String,
    val students: Int,
    val lessons: Int,
    val payments: Int,
)

fun backupSummary(file: BackupFile) =
    BackupSummary(
        file.exportedAt,
        file.tables["student"].orEmpty().size,
        file.tables["lesson"].orEmpty().size,
        file.tables["payment"].orEmpty().size,
    )

/** A row value as text, for the few places that read one (names, dates). */
fun BackupRow.text(column: String): String? = (this[column] as? JsonPrimitive)?.takeIf { it.isString }?.content

fun BackupRow.long(column: String): Long? = (this[column] as? JsonPrimitive)?.let { if (it.isString) null else it.content.toLongOrNull() }

/** One line of Recentes (AC-011-11). */
data class BackupLogEntry(
    val at: String,
    val auto: Boolean,
    /** The folder's name for an automatic backup; empty for a shared one. */
    val where: String,
    val bytes: Long,
    val ok: Boolean,
)

/** This phone's backup setup and history: never written into a backup, kept through a restore (AC-011-17). */
data class BackupState(
    val auto: Boolean = false,
    val every: BackupEvery = BackupEvery.WEEK,
    val keep: Int = BACKUP_KEEP_DEFAULT,
    val folderUri: String = "",
    val folderName: String = "",
    /** When automatic backup was last turned on, so the first run waits for its slot. */
    val enabledAt: String = "",
    /** The files Tutora wrote in the folder, oldest first: the only ones it ever deletes (AC-011-7). */
    val written: List<String> = emptyList(),
    val log: List<BackupLogEntry> = emptyList(),
    /** The last automatic run couldn't write to the folder (AC-011-9). */
    val failed: Boolean = false,
    val lastBackupAt: String = "",
    /** Desfazer restauração is offered (AC-011-16). */
    val restoreUndo: Boolean = false,
)

// Automatic backup (AC-011-6 to 8, 10).

enum class BackupEvery(
    val key: String,
) {
    DAY("day"),
    WEEK("week"),
    MONTH("month"),
    ;

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: WEEK
    }
}

/** Automatic backups run in the evening, when the phone is usually charging. */
val BACKUP_TIME = LocalTime(20, 0)

/** The first 20:00 slot after [after]: every day, every Sunday, or the 1st of each month (AC-011-6). */
fun nextAutoBackup(
    after: Instant,
    every: BackupEvery,
    zone: String,
): Instant {
    var day = after.toLocalDateTime(kotlinx.datetime.TimeZone.of(zone)).date
    while (true) {
        val fits =
            when (every) {
                BackupEvery.DAY -> true
                BackupEvery.WEEK -> day.dayOfWeek == DayOfWeek.SUNDAY
                BackupEvery.MONTH -> day.day == 1
            }
        val at = localToInstant(day, BACKUP_TIME, zone)
        if (fits && at > after) return at
        day = day.plus(DatePeriod(days = 1))
    }
}

/**
 * Whether a run is owed: the first slot after the last backup (or after turning it on) has
 * passed. However many slots were missed, one backup brings it up to date (AC-011-8).
 */
fun autoBackupDue(
    lastBackupAt: Instant?,
    enabledAt: Instant,
    every: BackupEvery,
    now: Instant,
    zone: String,
): Boolean = nextAutoBackup(maxOf(lastBackupAt ?: enabledAt, enabledAt), every, zone) <= now

/** Which of the files Tutora wrote, oldest first, go so that only the newest [keep] stay (AC-011-7). */
fun filesToPrune(
    written: List<String>,
    keep: Int,
): List<String> = written.dropLast(keep.coerceIn(BACKUP_KEEP_MIN, BACKUP_KEEP_MAX))

const val BACKUP_KEEP_MIN = 1
const val BACKUP_KEEP_MAX = 30
const val BACKUP_KEEP_DEFAULT = 5
const val BACKUP_WARN_DAYS = 14

/**
 * Days without a backup, once there are 14 or more; null while there is nothing to lose yet or
 * the last backup is recent. With none ever made, it counts from the first class (AC-011-10).
 */
fun backupWarningDays(
    lastBackupAt: Instant?,
    firstLessonAt: Instant?,
    now: Instant,
    zone: String,
): Int? {
    val since = lastBackupAt ?: firstLessonAt ?: return null
    val tz = kotlinx.datetime.TimeZone.of(zone)
    val days = since.toLocalDateTime(tz).date.daysUntil(now.toLocalDateTime(tz).date)
    return days.takeIf { it >= BACKUP_WARN_DAYS }
}

/** "Tutora backup 2026-10-29 20-00.json" (AC-011-2). */
fun backupFileName(
    at: Instant,
    zone: String,
    base: String,
): String {
    val t = at.toLocalDateTime(kotlinx.datetime.TimeZone.of(zone))
    val hh = t.hour.toString().padStart(2, '0')
    val mm = t.minute.toString().padStart(2, '0')
    return "$base ${t.date} $hh-$mm.json"
}

/** The local date of [at], for spreadsheet ranges and the file name. */
fun dateOf(
    at: Instant,
    zone: String,
): LocalDate = at.toLocalDateTime(kotlinx.datetime.TimeZone.of(zone)).date

/** Este mês, Mês passado, Este ano, Tudo or Personalizado for the spreadsheet (AC-011-19). */
sealed interface SheetRange {
    data object ThisMonth : SheetRange

    data object LastMonth : SheetRange

    data object ThisYear : SheetRange

    /** Everything, from the first class or payment. */
    data object All : SheetRange

    data class Custom(
        val from: LocalDate,
        val to: LocalDate,
    ) : SheetRange

    fun dates(
        today: LocalDate,
        first: LocalDate?,
    ): Pair<LocalDate, LocalDate> =
        when (this) {
            ThisMonth -> ReportRange.ThisMonth.dates(today)
            LastMonth -> ReportRange.LastMonth.dates(today)
            ThisYear -> LocalDate(today.year, 1, 1) to today
            All -> minOf(first ?: today, today) to today
            is Custom -> ReportRange.Custom(from, to).dates(today)
        }
}
