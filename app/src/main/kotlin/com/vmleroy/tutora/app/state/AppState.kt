package com.vmleroy.tutora.app.state

import androidx.compose.runtime.staticCompositionLocalOf
import com.vmleroy.tutora.app.usecases.Catalog
import com.vmleroy.tutora.app.usecases.Ctx
import com.vmleroy.tutora.app.usecases.Schedule
import com.vmleroy.tutora.data.CatalogRepository
import com.vmleroy.tutora.data.ScheduleRepository
import com.vmleroy.tutora.data.SettingsRepository
import com.vmleroy.tutora.data.TutoraDatabase
import com.vmleroy.tutora.domain.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

// The state layer (architecture, layers): screens read Flows and call actions here, never
// the data layer. Each write goes through a use case.

typealias PlaceItem = com.vmleroy.tutora.data.PlaceWithCount
typealias StudentItem = com.vmleroy.tutora.data.StudentWithLinks

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
