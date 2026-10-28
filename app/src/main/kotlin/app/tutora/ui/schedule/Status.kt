package app.tutora.ui.schedule

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.BadgeTone
import app.tutora.designsystem.components.BlockState
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.domain.CancelReason
import app.tutora.domain.Lesson
import app.tutora.domain.LessonStatus

/** "Agendada", "Dada", "Cancelada pelo aluno", "Cancelada por você", "Faltou" (AC-006-14). */
@Composable
fun statusLabel(l: Lesson): String =
    stringResource(
        when (l.status) {
            LessonStatus.SCHEDULED -> R.string.status_scheduled
            LessonStatus.DONE -> R.string.status_done
            LessonStatus.MISSED -> R.string.status_missed
            LessonStatus.CANCELLED ->
                if (l.cancelReason == CancelReason.TEACHER) R.string.status_cancelledByYou else R.string.status_cancelledByStudent
        },
    )

/** The label with its charge: "Cancelada pelo aluno · cobra R$ 60,00" (AC-006-14). */
@Composable
fun statusWithFee(l: Lesson): String {
    val label = statusLabel(l)
    val fee = l.feeCents ?: return label
    return stringResource(R.string.lesson_statusFee, label, formatMoney(fee, currentLanguage()))
}

/** The badge a day list shows for a class that is no longer just scheduled (AC-006-15). */
@Composable
fun statusBadge(l: Lesson): Triple<BadgeTone, String, IconName?>? {
    val fee = l.feeCents?.let { formatMoney(it, currentLanguage()) }

    @Composable
    fun short(label: Int) = fee?.let { stringResource(R.string.lesson_statusFee, stringResource(label), it) } ?: stringResource(label)
    return when (l.status) {
        LessonStatus.SCHEDULED -> null
        LessonStatus.DONE -> Triple(BadgeTone.SUCCESS, stringResource(R.string.status_done), IconName.CHECK)
        LessonStatus.CANCELLED -> Triple(BadgeTone.NEUTRAL, short(R.string.status_cancelled), null)
        LessonStatus.MISSED -> Triple(BadgeTone.DANGER, short(R.string.status_missed), null)
    }
}

fun blockState(
    l: Lesson,
    nextId: String?,
): BlockState =
    when {
        l.id == nextId -> BlockState.NEXT
        l.status == LessonStatus.DONE -> BlockState.DONE
        l.status == LessonStatus.CANCELLED || l.status == LessonStatus.MISSED -> BlockState.MUTED
        else -> BlockState.DEFAULT
    }

/** A muted line of explanation under a field. */
@Composable
fun HintLine(text: String) {
    Text(text, style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
}
