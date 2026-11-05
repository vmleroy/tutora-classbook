package app.tutora.usecases

import app.tutora.data.NoticeRow
import app.tutora.domain.NOTICE_DAYS
import app.tutora.domain.Notice
import app.tutora.domain.NoticeKind
import app.tutora.domain.instantOf
import app.tutora.domain.isoText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.days

/** The notifications Tutora sent, kept 30 days for Notificações (AC-014-39, 40). */
class Notices(
    private val ctx: Ctx,
) {
    private val dao get() = ctx.db.notices()

    private fun since(): String = (instantOf(ctx.now()) - NOTICE_DAYS.days).isoText()

    /** Keeps what a reminder said, whether or not the phone shows it. */
    suspend fun record(notice: ReminderNotice) {
        val (kind, row) =
            when (notice) {
                is ReminderNotice.Class -> NoticeKind.CLASS to Triple(notice.lesson.id, 0, 0L)
                is ReminderNotice.Owed -> NoticeKind.OWED to Triple(null, notice.summary.count, notice.summary.cents)
                is ReminderNotice.Confirm -> NoticeKind.CONFIRM to Triple(null, notice.count, 0L)
            }
        dao.insert(
            NoticeRow(
                ctx.newId(),
                kind.key,
                row.first,
                row.second,
                row.third,
                (notice as? ReminderNotice.Class)?.let { minutesUntil(it) } ?: 0,
                (notice as? ReminderNotice.Owed)?.onlyStudents ?: true,
                ctx.now(),
                false,
            ),
        )
        prune()
    }

    private fun minutesUntil(n: ReminderNotice.Class): Int =
        (instantOf(n.lesson.startAt) - instantOf(ctx.now())).inWholeMinutes.toInt().coerceAtLeast(0)

    /** Opening Notificações marks everything seen (AC-014-40). */
    suspend fun markSeen() = dao.markAllSeen()

    /** Older than 30 days goes. */
    suspend fun prune() = dao.deleteBefore(since())

    fun recent(): Flow<List<Notice>> =
        dao.observeSince(since()).map { rows ->
            rows.mapNotNull { r ->
                NoticeKind.fromKey(r.kind)?.let { k ->
                    Notice(r.id, k, r.at, r.lessonId, r.count, r.cents, r.minutes, r.onlyStudents, r.seen)
                }
            }
        }

    fun unseen(): Flow<Int> = dao.observeUnseen(since())
}
