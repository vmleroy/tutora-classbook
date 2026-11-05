package app.tutora.ui.schedule

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.BadgeTone
import app.tutora.designsystem.components.BlockState
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.domain.CancelReason
import app.tutora.domain.ConfirmWhen
import app.tutora.domain.Lesson
import app.tutora.domain.LessonStatus
import app.tutora.domain.instantOf
import app.tutora.domain.toConfirm

/** "Agendada", "Dada", "Cancelada pelo aluno", "Cancelada por você", "Faltou" (AC-006-14). */
@Composable
fun statusLabel(l: Lesson): String =
    stringResource(
        when (l.status) {
            LessonStatus.SCHEDULED -> R.string.status_scheduled
            LessonStatus.LIVE -> R.string.status_live
            LessonStatus.DONE -> if (l.autoDone) R.string.status_autoDone else R.string.status_done
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

/**
 * The badge a day list shows for a class that is no longer just scheduled (AC-006-15), or that
 * ended still scheduled: "A confirmar" (AC-014-3). A class done by itself says "Dada sozinha" (AC-014-2).
 */
@Composable
fun statusBadge(
    l: Lesson,
    pending: Boolean = false,
): Triple<BadgeTone, String, IconName?>? {
    val fee = l.feeCents?.let { formatMoney(it, currentLanguage()) }

    @Composable
    fun short(label: Int) = fee?.let { stringResource(R.string.lesson_statusFee, stringResource(label), it) } ?: stringResource(label)
    return when (l.status) {
        LessonStatus.SCHEDULED -> if (pending) Triple(BadgeTone.SECONDARY, stringResource(R.string.confirm_mark), null) else null
        LessonStatus.LIVE -> Triple(BadgeTone.ACCENT, stringResource(R.string.status_live), null)
        LessonStatus.DONE ->
            Triple(BadgeTone.SUCCESS, stringResource(if (l.autoDone) R.string.status_autoDone else R.string.status_done), IconName.CHECK)
        LessonStatus.CANCELLED -> Triple(BadgeTone.NEUTRAL, short(R.string.status_cancelled), null)
        LessonStatus.MISSED -> Triple(BadgeTone.DANGER, short(R.string.status_missed), null)
    }
}

fun blockState(
    l: Lesson,
    nextId: String?,
    pending: Boolean = false,
): BlockState =
    when {
        l.forecast -> BlockState.FORECAST
        l.status == LessonStatus.LIVE -> BlockState.LIVE
        pending -> BlockState.TO_CONFIRM
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

/**
 * Which classes are to confirm now, by Pedir confirmação and a clock read again each minute
 * (AC-014-1, 3). Forecast, done, cancelled, missed and live classes never are.
 */
@Composable
fun rememberToConfirm(): (Lesson) -> Boolean {
    val services = app.tutora.state.LocalServices.current
    val now by app.tutora.ui.live
        .rememberNow(services, MINUTE_MS)
    val settings by services.settings.collectAsState(initial = null)
    val whenTo = settings?.confirmWhen ?: ConfirmWhen.END
    return remember(now, whenTo) { { l: Lesson -> l.toConfirm(instantOf(now), services.zone, whenTo) } }
}

private const val MINUTE_MS = 60_000L
