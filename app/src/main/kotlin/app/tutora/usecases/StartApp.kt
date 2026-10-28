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
            val ctx = Ctx(db, now, newId, live = notifier, alarm = alarm)
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
