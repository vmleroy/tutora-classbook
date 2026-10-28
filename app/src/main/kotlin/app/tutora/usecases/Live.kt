package app.tutora.usecases

import app.tutora.data.LessonRow
import app.tutora.data.ScheduleRepository
import app.tutora.data.SettingsRepository
import app.tutora.data.TimerRow
import app.tutora.data.inTransaction
import app.tutora.data.toTimer
import app.tutora.domain.AutoStart
import app.tutora.domain.Lesson
import app.tutora.domain.LessonStatus
import app.tutora.domain.LessonTimer
import app.tutora.domain.Who
import app.tutora.domain.dueToStart
import app.tutora.domain.instantOf
import app.tutora.domain.isoText
import app.tutora.domain.pause
import app.tutora.domain.resume
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.hours

/** What the live notification shows: who, and the clock to draw (AC-007-15). */
data class LiveNote(
    val lessonId: String,
    val title: String,
    val timer: LessonTimer,
    val now: String,
)

/** The live-class notification, called by the use cases after each transaction. */
interface LiveNotifier {
    fun show(note: LiveNote)

    fun hide()

    /** No notification: tests, and anywhere without one. */
    object None : LiveNotifier {
        override fun show(note: LiveNote) = Unit

        override fun hide() = Unit
    }
}

/** Wakes the app at a class's start time, for "Também com o app fechado" (AC-007-22). */
interface AutoStartAlarm {
    /** Sets the one alarm for [at] (an instant), or clears it with null. */
    fun set(at: String?)

    object None : AutoStartAlarm {
        override fun set(at: String?) = Unit
    }
}

sealed interface LiveResult {
    data object Ok : LiveResult

    /** Only one class is live at a time; this one is (AC-007-2). */
    data class AlreadyLive(
        val lessonId: String,
    ) : LiveResult

    data class Err(
        val error: ScheduleError,
    ) : LiveResult
}

// How far back a class can have begun and still be running (the longest duration is under this).
private val LOOK_BACK = 12.hours

// Discarded classes remembered so they don't start by themselves again.
private const val SKIP_KEEP = 50

private val Lesson.whoTitle: String get() =
    when (val w = who) {
        is Who.OfStudent -> w.student.name
        is Who.OfGroup -> w.group.name
    }

/** The live timer (spec 007). Every write runs in one transaction, then updates the notification. */
class Live(
    private val ctx: Ctx,
) {
    private val db get() = ctx.db
    private val dao get() = db.schedule()

    private fun TimerRow.with(t: LessonTimer) = copy(pausedMs = t.pausedMs, pausedAt = t.pausedAt)

    private suspend fun note(row: LessonRow): LiveNote? {
        val timer = dao.timer(row.id)?.toTimer() ?: return null
        val lesson = ScheduleRepository(db).load(row) ?: return null
        return LiveNote(row.id, lesson.whoTitle, timer, ctx.now())
    }

    /** Shows the notification for [id] if it is live, hides it otherwise. */
    private suspend fun refresh(id: String) {
        val row = dao.lesson(id)
        val n = if (row?.status == LessonStatus.LIVE.key) note(row) else null
        if (n != null) ctx.live.show(n) else ctx.live.hide()
    }

    /** Iniciar aula: the class goes Ao vivo and its clock starts now, or at [startedAt] (AC-007-1, 2). */
    suspend fun start(
        id: String,
        startedAt: String = ctx.now(),
    ): LiveResult {
        val r =
            db.inTransaction {
                val row = dao.lesson(id) ?: return@inTransaction LiveResult.Err(ScheduleError.NOT_FOUND)
                val live = dao.liveLesson()
                if (live != null && live.id != id) return@inTransaction LiveResult.AlreadyLive(live.id)
                if (row.status != LessonStatus.SCHEDULED.key) return@inTransaction LiveResult.Err(ScheduleError.NOT_SCHEDULED)
                dao.deleteStaleTimers()
                dao.upsertTimer(TimerRow(id, startedAt, 0, null, null, null))
                // A live class stands apart from its series, so a series change never removes it.
                dao.upsertLesson(row.copy(status = LessonStatus.LIVE.key, isDetached = row.ruleId != null, updatedAt = ctx.now()))
                LiveResult.Ok
            }
        if (r == LiveResult.Ok) refresh(id)
        return r
    }

    /** Pausar (AC-007-7). */
    suspend fun pause(id: String) = update(id) { pause(it, ctx.now()) }

    /** Retomar (AC-007-7). */
    suspend fun resume(id: String) = update(id) { resume(it, ctx.now()) }

    private suspend fun update(
        id: String,
        change: (LessonTimer) -> LessonTimer,
    ) {
        db.inTransaction {
            val row = dao.timer(id) ?: return@inTransaction
            dao.upsertTimer(row.with(change(row.toTimer())))
        }
        refresh(id)
    }

    /** Anotação rápida, kept with the clock until the class ends (AC-007-8). */
    suspend fun setNote(
        id: String,
        text: String,
    ) = db.inTransaction {
        val row = dao.timer(id) ?: return@inTransaction
        dao.upsertTimer(row.copy(note = text.ifBlank { null }))
    }

    /**
     * Encerrar aula, after the sheet: Dada with the real minutes, the price frozen and the quick
     * note added to the notes. The clock stays, paused, for Desfazer (AC-007-10).
     */
    suspend fun end(
        id: String,
        minutes: Int,
    ): ScheduleResult {
        val r =
            db.inTransaction {
                val row = dao.lesson(id) ?: return@inTransaction ScheduleResult.Err(ScheduleError.NOT_FOUND)
                val timer = dao.timer(id) ?: return@inTransaction ScheduleResult.Err(ScheduleError.NOT_FOUND)
                if (row.status != LessonStatus.LIVE.key) return@inTransaction ScheduleResult.Err(ScheduleError.NOT_FOUND)
                val notes = listOfNotNull(row.notes, timer.note).joinToString("\n").ifEmpty { null }
                val done =
                    freezeDone(ctx, row.copy(plannedMin = minutes, notes = notes))
                        ?: return@inTransaction ScheduleResult.Err(ScheduleError.NOT_FOUND)
                dao.upsertLesson(done)
                dao.upsertTimer(timer.with(pause(timer.toTimer(), ctx.now())).copy(notesBefore = row.notes))
                ScheduleResult.Ok(id)
            }
        refresh(id)
        return r
    }

    /** Desfazer on Aula concluída: Ao vivo again, paused where it ended, with the notes as before (AC-007-11). */
    suspend fun undoEnd(id: String): ScheduleResult {
        val r =
            db.inTransaction {
                val row = dao.lesson(id) ?: return@inTransaction ScheduleResult.Err(ScheduleError.NOT_FOUND)
                val timer = dao.timer(id) ?: return@inTransaction ScheduleResult.Err(ScheduleError.NOT_FOUND)
                val live = dao.liveLesson()
                if (live != null && live.id != id) return@inTransaction ScheduleResult.Err(ScheduleError.NOT_SCHEDULED)
                dao.upsertLesson(
                    row.copy(
                        status = LessonStatus.LIVE.key,
                        notes = timer.notesBefore,
                        rateCents = null,
                        rateType = null,
                        updatedAt = ctx.now(),
                    ),
                )
                ScheduleResult.Ok(id)
            }
        refresh(id)
        return r
    }

    /**
     * Descartar: back to Agendada as if it had never started (AC-007-12). It won't start by itself
     * again (AC-007-20).
     */
    suspend fun discard(id: String): ScheduleResult {
        val r =
            db.inTransaction {
                val row = dao.lesson(id) ?: return@inTransaction ScheduleResult.Err(ScheduleError.NOT_FOUND)
                dao.deleteTimer(id)
                dao.upsertLesson(row.copy(status = LessonStatus.SCHEDULED.key, updatedAt = ctx.now()))
                val settings = SettingsRepository(db).load()
                SettingsRepository(
                    db,
                ).save(settings.copy(autoStartSkipped = (settings.autoStartSkipped + id).toList().takeLast(SKIP_KEEP).toSet()), ctx.now())
                ScheduleResult.Ok(id)
            }
        ctx.live.hide()
        return r
    }

    /**
     * Início automático: starts the class whose time has come, counting from its planned start,
     * then sets the alarm for the next one when the app may be closed (AC-007-20 to 22). Safe to
     * run any number of times.
     */
    suspend fun autoStart() {
        val settings = SettingsRepository(db).load()
        val on = settings.liveTimer && settings.autoStart != AutoStart.OFF
        val repo = ScheduleRepository(db)
        if (on && dao.liveLesson() == null) {
            val now = ctx.now()
            val since = (instantOf(now) - LOOK_BACK).isoText()
            val due = dueToStart(repo.between(since, now).first(), now, settings.autoStartSkipped)
            if (due != null) start(due.id, due.startAt)
        }
        val next = if (on && settings.autoStart == AutoStart.ALWAYS) repo.firstAfter(ctx.now()).first()?.startAt else null
        ctx.alarm.set(next)
    }

    /** On start-up, a class still live gets its notification back (after a restart, say). */
    suspend fun restoreNotification() {
        val row = dao.liveLesson() ?: return
        note(row)?.let(ctx.live::show)
    }
}
