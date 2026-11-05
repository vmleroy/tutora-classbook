package app.tutora.state

import androidx.compose.runtime.staticCompositionLocalOf
import app.tutora.data.CatalogRepository
import app.tutora.data.EarningsRepository
import app.tutora.data.PaymentRepository
import app.tutora.data.ReportsRepository
import app.tutora.data.ScheduleRepository
import app.tutora.data.SettingsRepository
import app.tutora.data.TutoraDatabase
import app.tutora.domain.Settings
import app.tutora.domain.teaching
import app.tutora.domain.toConfirm
import app.tutora.usecases.Backups
import app.tutora.usecases.Catalog
import app.tutora.usecases.Ctx
import app.tutora.usecases.Lessons
import app.tutora.usecases.Live
import app.tutora.usecases.Payments
import app.tutora.usecases.Schedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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
    val paymentRepo = PaymentRepository(db)
    val earningsRepo = EarningsRepository(db)
    val reportsRepo = ReportsRepository(db)
    val payments = Payments(ctx)
    val backups = Backups(ctx)
    val calendar = app.tutora.usecases.CalendarSync(ctx)
    val appearance = app.tutora.usecases.Appearance(ctx)
    val confirming = app.tutora.usecases.Confirming(ctx)
    val reminders = app.tutora.usecases.Reminders(ctx)
    val setup = app.tutora.usecases.Setup(ctx)

    val settings: Flow<Settings> = settingsRepo.observe()

    /**
     * Saves a settings change; the Flow above repaints the app (AC-002-5, AC-003-12). The write
     * runs on the IO dispatcher whatever the caller's, so it never waits on the UI thread.
     */
    suspend fun saveSettings(settings: Settings) = withContext(Dispatchers.IO) { settingsRepo.save(settings, ctx.now()) }

    /** The saved settings, read once (no subscription). */
    suspend fun currentSettings(): Settings = withContext(Dispatchers.IO) { settingsRepo.load() }

    /**
     * The Agenda's classes from [first] to [last]: the stored ones, and past the horizon the ones
     * forecast from their series (Victor, 4 Nov 2026; AC-016-35).
     */
    fun agendaBetween(
        first: kotlinx.datetime.LocalDate,
        last: kotlinx.datetime.LocalDate,
    ): Flow<List<app.tutora.domain.Lesson>> {
        val (from, to) = app.tutora.domain.dayRange(first, last, zone)
        return kotlinx.coroutines.flow.combine(
            scheduleRepo.between(from, to),
            scheduleRepo.forecastBetween(first, last),
        ) { stored, forecast ->
            stored + forecast
        }
    }

    /**
     * The classes to confirm, oldest first, read again each minute as classes end and after any
     * change (AC-014-1, 4, 6). The clock runs off the main thread.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun toConfirm(): Flow<List<app.tutora.domain.Lesson>> {
        val ticks =
            kotlinx.coroutines.flow
                .flow {
                    while (true) {
                        emit(Unit)
                        kotlinx.coroutines.delay(MINUTE_MS)
                    }
                }.flowOn(Dispatchers.Default)
        return kotlinx.coroutines.flow
            .combine(ticks, settings) { _, s -> s.confirmWhen }
            .flatMapLatest { whenTo ->
                val now = now()
                scheduleRepo.scheduledBefore(now).map { list ->
                    list.filter { it.toConfirm(app.tutora.domain.instantOf(now), zone, whenTo) }
                }
            }
    }

    /** The kinds of teaching the teacher already has places or classes of: never hidden (AC-014-20). */
    fun teachingInUse(): Flow<Set<app.tutora.domain.Teaching>> = kindsInUse().map { kinds -> kinds.map { it.teaching() }.toSet() }

    /** The place kinds the teacher has places or classes of (AC-014-20). */
    fun kindsInUse(): Flow<Set<app.tutora.domain.PlaceKind>> = scheduleRepo.kindsInUse()

    /** The device time zone; classes are scheduled in it. */
    val zone: String get() = ctx.zone()

    fun now(): String = ctx.now()
}

private const val MINUTE_MS = 60_000L

val LocalServices = staticCompositionLocalOf<AppServices> { error("AppServices are missing") }
