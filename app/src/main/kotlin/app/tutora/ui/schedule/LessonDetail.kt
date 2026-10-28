package app.tutora.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.Card
import app.tutora.designsystem.components.HeaderAction
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.RowTrailing
import app.tutora.designsystem.components.Tag
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.Repeat
import app.tutora.domain.Who
import app.tutora.domain.Worth
import app.tutora.domain.initials
import app.tutora.domain.localOf
import app.tutora.domain.worth
import app.tutora.state.LocalServices
import app.tutora.ui.LessonEdit
import app.tutora.ui.ScreenScaffold
import app.tutora.ui.clockTime
import app.tutora.ui.durationText
import app.tutora.ui.longDate
import app.tutora.ui.placeName
import app.tutora.ui.tag
import app.tutora.ui.weekdayLong
import kotlinx.datetime.LocalTime

/**
 * A class in full: when, how long, what it is worth, who and where, and the notes. Changing it
 * is one deliberate step, the pencil in the header (docs/product.md, "Detail first, then edit").
 */
@Composable
fun LessonDetailScreen(
    nav: NavHostController,
    id: String,
) {
    val services = LocalServices.current
    val language = currentLanguage()
    val lesson by remember(id) { services.scheduleRepo.lesson(id) }.collectAsState(initial = null)
    var seen by remember { mutableStateOf(false) }
    // A class removed from its edit form leaves nothing to show here.
    LaunchedEffect(lesson) {
        if (lesson != null) {
            seen = true
        } else if (seen) {
            nav.popBackStack()
        }
    }
    val l = lesson ?: return
    val local = localOf(l.startAt, services.zone)
    val endMin = local.hour * 60 + local.minute + l.plannedMin
    val end = LocalTime((endMin / 60) % 24, endMin % 60)
    ScreenScaffold(
        stringResource(R.string.lesson_detail),
        "lesson-detail",
        variant = HeaderVariant.PUSH,
        onBack = { nav.popBackStack() },
        actions = listOf(HeaderAction(IconName.PENCIL, stringResource(R.string.catalog_edit)) { nav.navigate(LessonEdit(id)) }),
    ) {
        Card {
            Text(
                "${clockTime(local.time, language)} – ${clockTime(end, language)}",
                style = TutoraTheme.type.titleLg,
                color = TutoraTheme.colors.ink,
            )
            Text(longDate(local.date, language), style = TutoraTheme.type.body, color = TutoraTheme.colors.ink)
            l.repeat?.let { r ->
                Text(
                    stringResource(
                        R.string.lesson_series,
                        stringResource(if (r == Repeat.WEEKLY) R.string.lesson_repeats_weekly else R.string.lesson_repeats_biweekly),
                        weekdayLong(local.date, language),
                        clockTime(local.time, language),
                    ),
                    style = TutoraTheme.type.meta,
                    color = TutoraTheme.colors.inkMuted,
                )
            }
        }
        Card {
            Fact(stringResource(R.string.lesson_duration), durationText(l.plannedMin))
            val worth = l.worth()
            Fact(
                stringResource(R.string.lesson_value),
                worthText(worth),
                muted = worth == Worth.Unset,
            )
        }
        val (name, sub, avatar) =
            when (val w = l.who) {
                is Who.OfStudent -> Triple(w.student.name, w.student.subject, initials(w.student.name) to w.student.color.tag())
                is Who.OfGroup -> Triple(w.group.name, w.group.subject, initials(w.group.name) to Tag.TEAL)
            }
        ListGroup(
            listOf(
                { ListRow(name, subtitle = sub, avatar = avatar, trailing = RowTrailing.NONE) },
                {
                    ListRow(
                        placeName(l.place),
                        subtitle = stringResource(R.string.lesson_placeLabel),
                        icon = IconName.PIN,
                        trailing = RowTrailing.NONE,
                    )
                },
            ),
        )
        l.notes?.let { notes ->
            Card(label = stringResource(R.string.lesson_notes)) {
                Text(stringResource(R.string.lesson_notes), style = TutoraTheme.type.caption, color = TutoraTheme.colors.inkMuted)
                Text(notes, style = TutoraTheme.type.body, color = TutoraTheme.colors.ink)
            }
        }
    }
}

/** One labelled fact on a line: the label on the left, the value on the right. */
@Composable
private fun Fact(
    label: String,
    value: String,
    muted: Boolean = false,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s3), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = TutoraTheme.type.body, color = TutoraTheme.colors.inkMuted)
        Text(
            value,
            style = TutoraTheme.type.bodyStrong,
            color = if (muted) TutoraTheme.colors.inkMuted else TutoraTheme.colors.ink,
            textAlign = TextAlign.End,
        )
    }
}
