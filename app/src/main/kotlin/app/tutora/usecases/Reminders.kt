package app.tutora.usecases

import app.tutora.data.PaymentRepository
import app.tutora.data.ScheduleRepository
import app.tutora.data.SettingsRepository
import app.tutora.domain.Lesson
import app.tutora.domain.LessonStatus
import app.tutora.domain.OwedSummary
import app.tutora.domain.PastClasses
import app.tutora.domain.Settings
import app.tutora.domain.balanceOf
import app.tutora.domain.dayRange
import app.tutora.domain.instantOf
import app.tutora.domain.isoText
import app.tutora.domain.nextClassReminder
import app.tutora.domain.nextDaily
import app.tutora.domain.nextWeekly
import app.tutora.domain.owedSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** The three reminders, each with one alarm on the phone (spec 014). */
enum class ReminderKind(
    val key: String,
) {
    CLASS("class"),
    OWED("owed"),
    CONFIRM("confirm"),
    ;

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key }
    }
}

/** The phone's alarms for the reminders: inexact, allowed while idle, kept across restarts by the boot receiver. */
interface ReminderAlarm {
    /** Sets the alarm of [kind] for [at] (an instant), or clears it with null; [lessonId] for a class reminder. */
    fun set(
        kind: ReminderKind,
        at: String?,
        lessonId: String? = null,
    )

    object None : ReminderAlarm {
        override fun set(
            kind: ReminderKind,
            at: String?,
            lessonId: String?,
        ) = Unit
    }
}

/** What a reminder says when its alarm goes off. */
sealed interface ReminderNotice {
    /** "Ana Souza · 14:00 · Online / Remoto"; tapping opens the class (AC-014-12). */
    data class Class(
        val lesson: Lesson,
    ) : ReminderNotice

    /** "3 alunos te devem R$ 480,00"; tapping opens Cadastro on those who owe (AC-014-13). */
    data class Owed(
        val summary: OwedSummary,
        /** Everyone owing is a student: "3 alunos te devem"; otherwise "3 pagamentos pendentes", as Hoje says (AC-008-15). */
        val onlyStudents: Boolean = true,
    ) : ReminderNotice

    /** "5 aulas para confirmar", with Marcar todas como dadas and Ver (AC-014-14). */
    data class Confirm(
        val count: Int,
    ) : ReminderNotice
}

class Reminders(
    private val ctx: Ctx,
) {
    private val settings get() = SettingsRepository(ctx.db)

    /**
     * Sets the next alarm of each reminder from the classes and settings as they are now; a reminder
     * turned off has its alarm removed (AC-014-15).
     */
    suspend fun plan() {
        val s = settings.load()
        val now = instantOf(ctx.now())
        val next =
            if (s.remindBefore > 0) {
                val repo = ScheduleRepository(ctx.db)
                val rows = ctx.db.schedule().scheduledFrom(ctx.now(), UPCOMING)
                nextClassReminder(rows.mapNotNull { repo.load(it) }, now, s.remindBefore)
            } else {
                null
            }
        ctx.reminders.set(ReminderKind.CLASS, next?.at?.isoText(), next?.lessonId)
        ctx.reminders.set(
            ReminderKind.OWED,
            if (s.owedWeekly) nextWeekly(now, s.owedDay, s.owedHour, ctx.zone(), s.owedMinute).isoText() else null,
        )
        // With "Marcar como dadas sozinhas" the daily alarm still comes, to mark them with the app closed, but says nothing.
        ctx.reminders.set(
            ReminderKind.CONFIRM,
            if (dailyAlarm(s)) nextDaily(now, s.confirmHour, ctx.zone(), s.confirmMinute).isoText() else null,
        )
    }

    private fun dailyAlarm(s: Settings) = (s.confirmDaily && s.pastClasses == PastClasses.ASK) || s.pastClasses == PastClasses.AUTO

    /**
     * An alarm went off: what to post, or null when there is nothing to say (the class was
     * cancelled, no one owes, nothing to confirm, or the reminder was turned off). The alarm of
     * that kind is set again either way.
     */
    suspend fun fire(
        kind: ReminderKind,
        lessonId: String?,
    ): ReminderNotice? {
        val s = settings.load()
        val notice =
            when (kind) {
                ReminderKind.CLASS ->
                    lessonId
                        ?.takeIf { s.remindBefore > 0 }
                        ?.let { id -> ctx.db.schedule().lesson(id) }
                        ?.takeIf { it.status == LessonStatus.SCHEDULED.key }
                        ?.let { ScheduleRepository(ctx.db).load(it) }
                        ?.let { ReminderNotice.Class(it) }
                ReminderKind.OWED ->
                    if (s.owedWeekly) {
                        val owing = PaymentRepository(ctx.db).balances().first().filterValues { it > 0 }
                        owedSummary(owing.values.map { balanceOf(it, 0) })
                            ?.let { ReminderNotice.Owed(it, owing.keys.all { p -> p.kind == app.tutora.domain.PayerKind.STUDENT }) }
                    } else {
                        null
                    }
                ReminderKind.CONFIRM -> {
                    Confirming(ctx).autoConfirm()
                    if (s.confirmDaily && s.pastClasses == PastClasses.ASK) {
                        Confirming(ctx).count().takeIf { it > 0 }?.let { ReminderNotice.Confirm(it) }
                    } else {
                        null
                    }
                }
            }
        // Notificações keeps it, whatever the phone allows (AC-014-40).
        notice?.let { Notices(ctx).record(it) }
        plan()
        return notice
    }

    /**
     * Every change to the classes or the settings sets the alarms again a moment later
     * (AC-014-15); [days] moves the window of classes looked at as days pass.
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    fun changes(days: Flow<LocalDate>): Flow<Unit> =
        days
            .flatMapLatest { day ->
                val (from, to) = dayRange(day, day.plus(DatePeriod(days = WINDOW_DAYS)), ctx.zone())
                combine(ScheduleRepository(ctx.db).between(from, to), settings.observe()) { lessons, s -> lessons to s }
            }.debounce(SETTLE_MS)
            .map { runCatching { plan() } }

    companion object {
        /** Classes looked at for the next reminder: enough to pass any cancelled ones. */
        private const val UPCOMING = 50
        private const val WINDOW_DAYS = 63
        const val SETTLE_MS = 800L
    }
}
