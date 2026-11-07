package app.tutora.usecases

import app.tutora.data.SettingsRepository
import app.tutora.data.inTransaction
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.Cents
import app.tutora.domain.Language
import app.tutora.domain.REMIND_BEFORE_DEFAULT
import app.tutora.domain.Teaching
import app.tutora.domain.TravelMode

/** What the first-run setup chose; each step not seen keeps these defaults (AC-014-17 to 23). */
data class SetupChoices(
    val language: Language,
    val teaching: Set<Teaching> = setOf(Teaching.PRIVATE),
    val onlineCents: Cents = SETUP_ONLINE_CENTS,
    val homeCents: Cents = SETUP_HOME_CENTS,
    val travel: TravelMode = TravelMode.VARIES,
    /** "Fixo": the amount typed, R$ 20,00 at first (AC-014-25). */
    val travelCents: Cents = SETUP_TRAVEL_CENTS,
    val remind: Boolean = true,
    /** Bloquear o Tutora, offered in step 4 (AC-015-5); off at first. */
    val appLock: Boolean = false,
)

const val SETUP_ONLINE_CENTS = 10_000L
const val SETUP_HOME_CENTS = 12_000L

/** "Fixo" in the setup starts at R$ 20,00, as the mockup shows; the teacher types another (AC-014-25). */
const val SETUP_TRAVEL_CENTS = 2_000L

/** The first-run setup (spec 014). Backup automático is saved by [Backups] when a folder is chosen. */
class Setup(
    private val ctx: Ctx,
) {
    private val db get() = ctx.db

    /** Whether the teacher already has data: they never see the setup (AC-014-17). */
    suspend fun needed(): Boolean = !SettingsRepository(db).load().setupDone && !db.schedule().hasData()

    /**
     * Saves every step at once: language, how they teach, the two built-in places' prices and
     * travel, and the reminder before class; the setup is then done (AC-014-18 to 23).
     */
    suspend fun save(c: SetupChoices) {
        db.inTransaction {
            val repo = SettingsRepository(db)
            val s = repo.load()
            repo.save(
                s.copy(
                    language = c.language,
                    teaching = c.teaching,
                    remindBefore = if (c.remind) REMIND_BEFORE_DEFAULT else 0,
                    appLock = c.appLock,
                    setupDone = true,
                ),
                ctx.now(),
            )
            db.places().get(BuiltinPlace.ONLINE.id)?.let {
                db.places().upsert(it.copy(rateCents = c.onlineCents, updatedAt = ctx.now()))
            }
            db.places().get(BuiltinPlace.STUDENT_HOME.id)?.let {
                db.places().upsert(
                    it.copy(
                        rateCents = c.homeCents,
                        travelMode = c.travel.name,
                        travelFeeCents = if (c.travel == TravelMode.FIXED) c.travelCents.coerceAtLeast(0) else 0,
                        updatedAt = ctx.now(),
                    ),
                )
            }
        }
        Reminders(ctx).plan()
    }

    /** A teacher from before spec 014, or one who restored a backup, never sees the setup. */
    suspend fun markDoneIfData() {
        val repo = SettingsRepository(db)
        val s = repo.load()
        if (!s.setupDone && db.schedule().hasData()) repo.save(s.copy(setupDone = true), ctx.now())
    }
}
