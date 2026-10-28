package app.tutora.state

import androidx.compose.runtime.staticCompositionLocalOf
import app.tutora.data.CatalogRepository
import app.tutora.data.ScheduleRepository
import app.tutora.data.SettingsRepository
import app.tutora.data.TutoraDatabase
import app.tutora.domain.Settings
import app.tutora.usecases.Catalog
import app.tutora.usecases.Ctx
import app.tutora.usecases.Lessons
import app.tutora.usecases.Live
import app.tutora.usecases.Schedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

// The state layer (architecture, layers): screens read Flows and call actions here, never
// the data layer. Each write goes through a use case.

typealias PlaceItem = app.tutora.data.PlaceWithCount
typealias StudentItem = app.tutora.data.StudentWithLinks

class AppServices(
    db: TutoraDatabase,
    val ctx: Ctx,
    val schemaVersion: Int,
) {
    private val settingsRepo = SettingsRepository(db)
    val catalogRepo = CatalogRepository(db)
    val scheduleRepo = ScheduleRepository(db)
    val catalog = Catalog(ctx)
    val schedule = Schedule(ctx)
    val lessons = Lessons(ctx)
    val live = Live(ctx)

    val settings: Flow<Settings> = settingsRepo.observe()

    /**
     * Saves a settings change; the Flow above repaints the app (AC-002-5, AC-003-12). The write
     * runs on the IO dispatcher whatever the caller's, so it never waits on the UI thread.
     */
    suspend fun saveSettings(settings: Settings) = withContext(Dispatchers.IO) { settingsRepo.save(settings, ctx.now()) }

    /** The saved settings, read once (no subscription). */
    suspend fun currentSettings(): Settings = withContext(Dispatchers.IO) { settingsRepo.load() }

    /** The device time zone; classes are scheduled in it. */
    val zone: String get() = ctx.zone()

    fun now(): String = ctx.now()
}

val LocalServices = staticCompositionLocalOf<AppServices> { error("AppServices are missing") }
