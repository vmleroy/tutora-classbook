package app.tutora.usecases

import app.tutora.data.SettingsRepository
import app.tutora.data.TutoraDatabase
import app.tutora.domain.Language
import app.tutora.domain.Settings
import app.tutora.domain.firstRunLanguage
import app.tutora.domain.startupLanguage

sealed interface StartResult {
    data class Ready(
        val db: TutoraDatabase,
        val settings: Settings,
        val schemaVersion: Int,
        val ctx: Ctx,
    ) : StartResult

    data class Failed(
        val language: Language,
        val backupPath: String?,
        val message: String,
    ) : StartResult
}

/**
 * Opens and migrates the database, makes sure every setting and built-in place exists, and
 * fills repeating classes. A first run takes
 * the language from the phone; afterwards the saved setting rules (AC-001-5, AC-001-6).
 */
class StartApp(
    private val openDatabase: () -> Pair<TutoraDatabase, String?>,
    private val phoneTags: () -> List<String>,
    private val now: () -> String,
    private val newId: () -> String,
    private val notifier: LiveNotifier = LiveNotifier.None,
    private val alarm: AutoStartAlarm = AutoStartAlarm.None,
    private val backupFolder: BackupFolder = BackupFolder.None,
    private val backupScheduler: BackupScheduler = BackupScheduler.None,
    private val privateDir: java.io.File = java.io.File(System.getProperty("java.io.tmpdir"), "tutora"),
    private val calendar: PhoneCalendar = PhoneCalendar.None,
    private val calendarWords: (Language) -> app.tutora.domain.CalendarWords = NO_CALENDAR_WORDS,
) {
    suspend operator fun invoke(): StartResult {
        val phone = phoneTags()
        var backup: String? = null
        return try {
            val (db, copy) = openDatabase()
            backup = copy
            // The first query runs Room's pending migrations.
            val repo = SettingsRepository(db)
            repo.ensureDefaults(firstRunLanguage(phone), now())
            val ctx =
                Ctx(
                    db,
                    now,
                    newId,
                    live = notifier,
                    alarm = alarm,
                    backupFolder = backupFolder,
                    backupScheduler = backupScheduler,
                    privateDir = privateDir,
                    calendar = calendar,
                    calendarWords = calendarWords,
                )
            // This phone's calendar setup starts off (AC-012-2).
            app.tutora.data
                .CalendarStore(db)
                .ensureDefaults(now())
            // This phone's backup setup starts off (AC-011-5).
            app.tutora.data
                .BackupStore(db)
                .ensureDefaults(now())
            Catalog(ctx).ensureBuiltinPlaces()
            // Classes given before payments existed get who owes them (spec 008).
            Payments(ctx).backfill()
            // Repeating classes stay filled 8 weeks ahead (AC-005-8).
            Schedule(ctx).fillSchedule()
            // A class still live (the phone restarted, say) gets its notification back.
            Live(ctx).restoreNotification()
            // A class whose time came while the app was closed starts now (AC-007-21).
            Live(ctx).autoStart()
            StartResult.Ready(db, repo.load(), TutoraDatabase.VERSION, ctx)
        } catch (e: Exception) {
            StartResult.Failed(startupLanguage(null, phone), backup, e.message ?: e.toString())
        }
    }
}
