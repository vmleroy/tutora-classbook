package app.tutora.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.ButtonVariant
import app.tutora.designsystem.components.ControlSize
import app.tutora.designsystem.components.DurationField
import app.tutora.designsystem.components.FieldLabel
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.StatTile
import app.tutora.designsystem.components.StatTone
import app.tutora.designsystem.components.Stepper
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.Lesson
import app.tutora.domain.LessonStatus
import app.tutora.domain.MAX_DURATION_MIN
import app.tutora.domain.dayRange
import app.tutora.domain.localOf
import app.tutora.state.LocalServices
import app.tutora.ui.countText
import app.tutora.ui.durationText
import app.tutora.ui.localeOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter

private const val PRICE_STEP = 500L // R$ 5

/** A label over a control, as every form field. */
@Composable
fun Field(
    label: String,
    required: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1_5)) {
        FieldLabel(label, required = required)
        content()
    }
}

/** Said once at the top of a form: what the red asterisk on a label means. */
@Composable
fun RequiredLegend() {
    Text(stringResource(R.string.form_requiredLegend), style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
}

/** A duration stepper in 5-minute steps; tapping the time opens hh:mm entry (AC-004-27). */
@Composable
fun DurationStepper(
    label: String,
    minutes: Int,
    onChange: (Int) -> Unit,
    min: Int = 15,
    max: Int = MAX_DURATION_MIN,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    var exact by rememberSaveable { mutableStateOf<Int?>(minutes) }
    val shown = durationText(minutes)
    Stepper(minutes.toLong(), {
        onChange(
            it.toInt(),
        )
    }, step = 5, min = min.toLong(), max = max.toLong(), format = { shown }, onPressValue = {
        exact = minutes
        open = true
    }, valueLabel = label)
    if (open) {
        val value = exact
        BottomSheet(
            label,
            { open = false },
            subtitle = stringResource(R.string.catalog_exactDuration),
            actions =
                listOf { m ->
                    Button(stringResource(R.string.catalog_done), {
                        if (value != null && value >= min) onChange(value)
                        open = false
                    }, m, enabled = value != null && value >= min)
                },
        ) { DurationField(label, minutes, { exact = it }, maxMinutes = max) }
    }
}

/**
 * Arquivar for an active item; Restaurar and Remover para sempre once archived. Removing asks
 * first and names the item (constitution rule 3, AC-004-24).
 */
@Composable
fun ArchiveActions(
    name: String,
    archived: Boolean,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onRemove: () -> Unit,
    removeBody: String = stringResource(R.string.catalog_removeBody),
) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    if (!archived) {
        Button(stringResource(R.string.catalog_archive), onArchive, variant = ButtonVariant.GHOST, icon = IconName.ARCHIVE)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Button(stringResource(R.string.catalog_restore), onRestore, variant = ButtonVariant.OUTLINE, fullWidth = true)
        Button(stringResource(R.string.catalog_remove), { confirming = true }, variant = ButtonVariant.DANGER, fullWidth = true)
    }
    if (confirming) {
        BottomSheet(
            stringResource(R.string.catalog_removeTitle, name),
            { confirming = false },
            subtitle = removeBody,
            actions =
                listOf(
                    { m -> Button(stringResource(R.string.catalog_cancel), { confirming = false }, m, variant = ButtonVariant.OUTLINE) },
                    { m ->
                        Button(stringResource(R.string.catalog_remove), {
                            confirming = false
                            onRemove()
                        }, m, variant = ButtonVariant.DANGER)
                    },
                ),
        )
    }
}

/** One of the three figures at the top of a student, group or place. */
data class DetailStat(
    val label: String,
    val value: String,
    val tone: StatTone = StatTone.PLAIN,
)

/**
 * The top of a student, group or place, as on the StudentDetail and PlaceDetail mockups
 * (Victor, 29 Oct 2026): its [lead] picture, name and details.
 */
@Composable
fun DetailIdentity(
    title: String,
    subtitle: String?,
    lead: @Composable () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3_5), verticalAlignment = Alignment.CenterVertically) {
        lead()
        Column(Modifier.weight(1f)) {
            Text(title, style = TutoraTheme.type.titleLg, color = TutoraTheme.colors.ink)
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = TutoraTheme.type.caption, color = TutoraTheme.colors.inkMuted)
        }
    }
}

/**
 * Three figures side by side, then the actions: the main one ([main], Registrar pagamento or
 * Receber da salinha) filled on its own line, Relatório and Nova aula outlined under it.
 */
@Composable
fun DetailOverview(
    stats: List<DetailStat>,
    main: Pair<String, () -> Unit>?,
    onReport: () -> Unit,
    onNewLesson: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            stats.forEach { StatTile(it.label, it.value, it.tone) }
        }
        // The main action on its own line, so no label wraps on a narrow phone.
        main?.let { (label, onClick) -> Button(label, onClick, Modifier.named(label), fullWidth = true) }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            val report = stringResource(R.string.rep_screen)
            Button(report, onReport, Modifier.weight(1f).named(report), variant = ButtonVariant.OUTLINE, icon = IconName.FILE)
            val new = stringResource(R.string.lesson_new)
            Button(new, onNewLesson, Modifier.weight(1f).named(new), variant = ButtonVariant.OUTLINE, icon = IconName.PLUS)
        }
    }
}

/** Read aloud by its label, as the header's icon buttons were. */
private fun Modifier.named(label: String) = semantics { contentDescription = label }

/** Editar, as an outlined button at the end of a detail's header. */
@Composable
fun EditButton(onClick: () -> Unit) {
    val label = stringResource(R.string.catalog_edit)
    Button(label, onClick, Modifier.named(label), variant = ButtonVariant.OUTLINE, size = ControlSize.SM, icon = IconName.PENCIL)
}

/** "Outubro · 6 aulas": this month's classes that [match], cancelled and missed ones left out. */
@Composable
fun monthStat(match: (Lesson) -> Boolean): DetailStat {
    val services = LocalServices.current
    val language = currentLanguage()
    val today = remember { localOf(services.now(), services.zone).date }
    val range =
        remember(today) {
            val first = LocalDate(today.year, today.month, 1)
            dayRange(first, YearMonth(today.year, today.month).lastDay, services.zone)
        }
    val lessons by remember(range) { services.scheduleRepo.between(range.first, range.second) }.collectAsState(initial = emptyList())
    val count = lessons.count { match(it) && it.status != LessonStatus.CANCELLED && it.status != LessonStatus.MISSED }
    val month =
        DateTimeFormatter
            .ofPattern("LLLL", localeOf(language))
            .format(today.toJavaLocalDate())
            .replaceFirstChar { it.uppercase() }
    return DetailStat(month, countText(R.string.schedule_count_zero, R.plurals.schedule_count, count))
}
