package app.tutora.usecases

import app.tutora.data.LessonRow
import app.tutora.data.SettingsRepository
import app.tutora.data.inTransaction
import app.tutora.domain.ConfirmWhen
import app.tutora.domain.PastClasses
import app.tutora.domain.LessonStatus
import app.tutora.domain.instantOf
import app.tutora.domain.isToConfirm

/**
 * Confirming the classes that happened while still scheduled (spec 014): one by one, all at once
 * with Desfazer, or by themselves with Aulas passadas on "Marcar como dadas sozinhas". Each write
 * runs in one transaction; Cancelada and Faltou go through [Lessons.cancel].
 */
class Confirming(
    private val ctx: Ctx,
) {
    private val db get() = ctx.db
    private val dao get() = db.schedule()

    /** The classes to confirm now, oldest first (AC-014-1, 6). */
    private suspend fun due(whenTo: ConfirmWhen): List<LessonRow> {
        val now = instantOf(ctx.now())
        return dao.scheduledBefore(ctx.now()).filter {
            isToConfirm(LessonStatus.fromKey(it.status), false, it.startAt, it.plannedMin, now, ctx.zone(), whenTo)
        }
    }

    suspend fun count(): Int = due(SettingsRepository(db).load().confirmWhen).size

    /** Dada: done with its planned duration and value, as Marcar como dada (AC-014-7). */
    suspend fun confirm(id: String): ScheduleResult {
        val row = dao.lesson(id) ?: return ScheduleResult.Err(ScheduleError.NOT_FOUND)
        return Lessons(ctx).markDone(id, row.plannedMin)
    }

    /** Confirmar todas como dadas: every class to confirm, done with its planned duration; the ids for Desfazer (AC-014-9). */
    suspend fun confirmAll(): List<String> = markAll(auto = false)

    /** Desfazer after Confirmar todas: the classes back to Agendada (AC-014-9). */
    suspend fun undoAll(ids: List<String>) =
        db.inTransaction {
            ids.forEach { id -> dao.lesson(id)?.let { dao.upsertLesson(undone(ctx, it)) } }
        }

    /** With Aulas passadas on "Marcar como dadas sozinhas", the classes due are marked "Dada sozinha" (AC-014-2). */
    suspend fun autoConfirm(): List<String> =
        if (SettingsRepository(db).load().pastClasses == PastClasses.AUTO) markAll(auto = true) else emptyList()

    private suspend fun markAll(auto: Boolean): List<String> =
        db.inTransaction {
            val whenTo = SettingsRepository(db).load().confirmWhen
            due(whenTo).mapNotNull { row ->
                val next = freezeDone(ctx, row.copy(autoDone = auto)) ?: return@mapNotNull null
                // Past classes are never touched by series edits, so they keep their place in the series and Desfazer is exact.
                dao.upsertLesson(next)
                row.id
            }
        }
}
