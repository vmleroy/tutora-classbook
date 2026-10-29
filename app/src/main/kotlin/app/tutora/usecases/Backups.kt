package app.tutora.usecases

import app.tutora.data.BACKUP_LOG_SIZE
import app.tutora.data.BackupStore
import app.tutora.data.EarningsRepository
import app.tutora.data.SettingsRepository
import app.tutora.data.TutoraDatabase
import app.tutora.domain.BACKUP_APP
import app.tutora.domain.BACKUP_KEEP_MAX
import app.tutora.domain.BACKUP_KEEP_MIN
import app.tutora.domain.BackupEvery
import app.tutora.domain.BackupFile
import app.tutora.domain.BackupLogEntry
import app.tutora.domain.BackupRead
import app.tutora.domain.BackupRow
import app.tutora.domain.BackupState
import app.tutora.domain.PayMethod
import app.tutora.domain.Period
import app.tutora.domain.PeriodKind
import app.tutora.domain.SheetRange
import app.tutora.domain.TypeFilter
import app.tutora.domain.autoBackupDue
import app.tutora.domain.backupFileName
import app.tutora.domain.dateOf
import app.tutora.domain.encodeBackup
import app.tutora.domain.filesToPrune
import app.tutora.domain.instantOf
import app.tutora.domain.isoText
import app.tutora.domain.long
import app.tutora.domain.monthOf
import app.tutora.domain.nextAutoBackup
import app.tutora.domain.readBackup
import app.tutora.domain.text
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import java.io.File

/** The folder picked once for automatic backups (AC-011-5): a Drive, OneDrive or phone folder. */
interface BackupFolder {
    /** Writes [name] into the folder at [uri] and returns its size; throws when it can't (AC-011-9). */
    fun write(
        uri: String,
        name: String,
        bytes: ByteArray,
    ): Long

    /** Deletes a file Tutora wrote there, if it is still there (AC-011-7). */
    fun delete(
        uri: String,
        name: String,
    )

    /** Keeps access to a folder just picked, across restarts, and returns its name for "Salvar em" (AC-011-5). */
    fun adopt(uri: String): String

    /** The bytes of a file picked in Restaurar backup, or null if it can't be read. */
    fun read(uri: String): ByteArray?

    object None : BackupFolder {
        override fun adopt(uri: String) = ""

        override fun read(uri: String): ByteArray? = null

        override fun write(
            uri: String,
            name: String,
            bytes: ByteArray,
        ): Long = error("No folder")

        override fun delete(
            uri: String,
            name: String,
        ) = Unit
    }
}

/** Wakes the app for the next automatic backup, at [at] (an instant), or never with null. */
interface BackupScheduler {
    fun plan(at: String?)

    object None : BackupScheduler {
        override fun plan(at: String?) = Unit
    }
}

/** The start of every backup's file name, the same in both languages. */
const val BACKUP_NAME = "Tutora backup"

/** A class on the spreadsheet's Aulas sheet. */
data class SheetClass(
    val day: LocalDate,
    val minutes: Int,
    val who: String,
    val placeId: String?,
    val status: String,
    val cents: Long?,
)

data class SheetPayment(
    val day: LocalDate,
    val who: String,
    val method: PayMethod?,
    val cents: Long,
)

data class SheetStudent(
    val name: String,
    val subject: String?,
    val phone: String?,
    val archived: Boolean,
)

/** A place; a built-in one has no name of its own, it follows the app language. */
data class SheetPlace(
    val id: String,
    val builtin: String?,
    val name: String?,
    val kind: String,
)

/** A line of Resumo: one month's earned, received and to receive (AC-011-19). */
data class SheetMonth(
    val month: LocalDate,
    val earned: Long,
    val received: Long,
    val toReceive: Long,
)

data class SheetData(
    val from: LocalDate,
    val to: LocalDate,
    val classes: List<SheetClass>,
    val payments: List<SheetPayment>,
    val students: List<SheetStudent>,
    val places: List<SheetPlace>,
    val months: List<SheetMonth>,
)

sealed interface RestoreResult {
    data object Ok : RestoreResult

    /** A class is live: it would be lost or doubled (Victor, 29 Oct 2026). */
    data object LiveClass : RestoreResult

    /** Nothing to undo any more. */
    data object NoUndo : RestoreResult
}

/**
 * Backup and restore (spec 011). A backup is every table as one JSON file; a restore replaces
 * every table with a file's, in one transaction, after copying the current data aside.
 */
class Backups(
    private val ctx: Ctx,
) {
    private val db: TutoraDatabase get() = ctx.db
    private val store = BackupStore(ctx.db)

    /** Before a restore, the data it replaced, for Desfazer restauração (AC-011-16). */
    private val undoFile get() = File(ctx.privateDir, "backups/before-restore.json")

    private suspend fun current(): BackupFile = BackupFile(BACKUP_APP, TutoraDatabase.VERSION, ctx.now(), store.dump())

    /** A backup made by hand, named with the day and time, for the share sheet (AC-011-2, 4). */
    suspend fun makeBackup(): Pair<String, ByteArray> {
        val file = current()
        val bytes = encodeBackup(file).encodeToByteArray()
        val name = backupFileName(instantOf(file.exportedAt), ctx.zone(), BACKUP_NAME)
        record(BackupLogEntry(file.exportedAt, auto = false, where = "", bytes = bytes.size.toLong(), ok = true))
        return name to bytes
    }

    /** Writes a backup to the folder, keeps the newest N there, and plans the next run (AC-011-6, 7, 9). */
    suspend fun runAuto() {
        val s = store.load()
        if (!s.auto || s.folderUri.isEmpty()) return
        val file = current()
        val bytes = encodeBackup(file).encodeToByteArray()
        val name = backupFileName(instantOf(file.exportedAt), ctx.zone(), BACKUP_NAME)
        val size = runCatching { ctx.backupFolder.write(s.folderUri, name, bytes) }.getOrNull()
        if (size == null) {
            val failed = BackupLogEntry(file.exportedAt, auto = true, where = s.folderName, bytes = 0, ok = false)
            store.save(s.copy(failed = true, log = (listOf(failed) + s.log).take(BACKUP_LOG_SIZE)), ctx.now())
        } else {
            val written = (s.written - name) + name
            val prune = filesToPrune(written, s.keep)
            prune.forEach { runCatching { ctx.backupFolder.delete(s.folderUri, it) } }
            store.save(s.copy(written = written - prune.toSet(), failed = false), ctx.now())
            record(BackupLogEntry(file.exportedAt, auto = true, where = s.folderName, bytes = size, ok = true))
        }
        planNext()
    }

    /** On every start: a run that was missed happens now, once (AC-011-8). */
    suspend fun runIfDue() {
        store.ensureDefaults(ctx.now())
        val s = store.load()
        if (!s.auto) return
        val last = s.lastBackupAt.ifEmpty { null }?.let(::instantOf)
        val enabled = s.enabledAt.ifEmpty { null }?.let(::instantOf) ?: instantOf(ctx.now())
        if (autoBackupDue(last, enabled, s.every, instantOf(ctx.now()), ctx.zone())) runAuto() else planNext()
    }

    /** Backup automático's switch, frequency, folder and how many to keep (AC-011-5 to 7). */
    suspend fun setAuto(
        auto: Boolean,
        every: BackupEvery,
        keep: Int,
        folderUri: String,
        folderName: String,
    ) {
        val s = store.load()
        val turnedOn = auto && !s.auto
        store.save(
            s.copy(
                auto = auto && folderUri.isNotEmpty(),
                every = every,
                keep = keep.coerceIn(BACKUP_KEEP_MIN, BACKUP_KEEP_MAX),
                folderUri = folderUri,
                folderName = folderName,
                enabledAt = if (turnedOn) ctx.now() else s.enabledAt,
                failed = if (folderUri != s.folderUri) false else s.failed,
            ),
            ctx.now(),
        )
        planNext()
    }

    private suspend fun planNext() {
        val s = store.load()
        ctx.backupScheduler.plan(
            if (s.auto) nextAutoBackup(instantOf(ctx.now()), s.every, ctx.zone()).isoText() else null,
        )
    }

    /** A successful backup: the date, Recentes, and the copy taken before a restore is let go. */
    private suspend fun record(entry: BackupLogEntry) {
        val s = store.load()
        store.save(
            s.copy(lastBackupAt = entry.at, log = (listOf(entry) + s.log).take(BACKUP_LOG_SIZE), restoreUndo = false),
            ctx.now(),
        )
        undoFile.delete()
    }

    /** Reads a picked file: a backup this app can restore, or why not (AC-011-12, 15, 20). */
    fun inspect(bytes: ByteArray): BackupRead = readBackup(bytes, TutoraDatabase.VERSION)

    /**
     * Replaces all data with [file]'s (AC-011-14): the current data goes aside first (AC-011-16),
     * this phone's backup setup stays (AC-011-17), and start-up's upkeep runs on the new data.
     */
    suspend fun restore(file: BackupFile): RestoreResult {
        if (store.liveCount() > 0) return RestoreResult.LiveClass
        val before = encodeBackup(current())
        undoFile.parentFile?.mkdirs()
        undoFile.writeText(before)
        replace(file)
        store.save(store.load().copy(restoreUndo = true), ctx.now())
        return RestoreResult.Ok
    }

    /** Desfazer restauração: brings back exactly what was there before the last restore (AC-011-16). */
    suspend fun undoRestore(): RestoreResult {
        if (store.liveCount() > 0) return RestoreResult.LiveClass
        val read = undoFile.takeIf { it.exists() }?.readBytes()?.let(::inspect) as? BackupRead.Ok ?: return RestoreResult.NoUndo
        replace(read.file)
        store.save(store.load().copy(restoreUndo = false), ctx.now())
        undoFile.delete()
        return RestoreResult.Ok
    }

    private suspend fun replace(file: BackupFile) {
        val language = SettingsRepository(db).load().language
        store.replaceAll(file.tables)
        // An older backup may lack settings added since; they get their defaults.
        SettingsRepository(db).ensureDefaults(language, ctx.now())
        Catalog(ctx).ensureBuiltinPlaces()
        Payments(ctx).backfill()
        Schedule(ctx).fillSchedule()
    }

    /**
     * Everything the spreadsheet shows for [range] (AC-011-19): classes and payments in it, every
     * student and place, and one line per month from the earnings rules (spec 009).
     */
    suspend fun sheetData(range: SheetRange): SheetData {
        val zone = ctx.zone()
        val today = dateOf(instantOf(ctx.now()), zone)
        val t = store.dump()
        val first =
            (t["lesson"].orEmpty().mapNotNull { it.text("start_at") } + t["payment"].orEmpty().mapNotNull { it.text("paid_at") })
                .minOrNull()
                ?.let { dateOf(instantOf(it), zone) }
        val (from, to) = range.dates(today, first)
        val students = t["student"].orEmpty().associateBy { it.text("id") }
        val groups = t["group_class"].orEmpty().associateBy { it.text("id") }
        val places =
            t["place"].orEmpty().map {
                SheetPlace(it.text("id").orEmpty(), it.text("builtin"), it.text("name"), it.text("kind").orEmpty())
            }

        fun who(row: BackupRow): String =
            row.text("student_id")?.let { students[it]?.text("name") }
                ?: row.text("group_id")?.let { groups[it]?.text("name") }
                ?: row.text("place_id")?.let { id -> places.firstOrNull { it.id == id }?.name }
                ?: ""

        fun inRange(iso: String?) = iso != null && dateOf(instantOf(iso), zone) in from..to
        val earnings = EarningsRepository(db)
        val months =
            generateSequence(monthOf(from)) { it.plus(DatePeriod(months = 1)) }.takeWhile { it <= monthOf(to) }.toList().map { m ->
                val v = earnings.view(Period(PeriodKind.MONTH, m), TypeFilter.ALL, today, zone).first()
                SheetMonth(m, v.summary.earned, v.summary.received, v.summary.toReceive)
            }
        return SheetData(
            from = from,
            to = to,
            classes =
                t["lesson"].orEmpty().filter { inRange(it.text("start_at")) }.sortedBy { it.text("start_at") }.map {
                    SheetClass(
                        dateOf(instantOf(it.text("start_at").orEmpty()), zone),
                        it.long("planned_min")?.toInt() ?: 0,
                        who(it),
                        it.text("place_id"),
                        it.text("status").orEmpty(),
                        it.long("due_cents"),
                    )
                },
            payments =
                t["payment"].orEmpty().filter { inRange(it.text("paid_at")) }.sortedBy { it.text("paid_at") }.map {
                    SheetPayment(
                        dateOf(instantOf(it.text("paid_at").orEmpty()), zone),
                        who(it),
                        PayMethod.fromKey(it.text("method")),
                        it.long("amount_cents") ?: 0,
                    )
                },
            students =
                t["student"].orEmpty().sortedBy { it.text("name") }.map {
                    SheetStudent(it.text("name").orEmpty(), it.text("subject"), it.text("phone"), it.text("archived_at") != null)
                },
            places = places,
            months = months,
        )
    }

    fun observe() = store.observe()

    suspend fun state(): BackupState = store.load()

    fun firstLessonAt() = db.backup().observeFirstLessonAt()
}
