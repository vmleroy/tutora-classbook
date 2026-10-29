package app.tutora.ui.schedule

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.AgendaBlock
import app.tutora.designsystem.components.IconButton
import app.tutora.designsystem.components.IconButtonVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.Tag
import app.tutora.designsystem.components.WeekDay
import app.tutora.designsystem.components.WeekStrip
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.tokens.TutoraTokens.Size
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.Lesson
import app.tutora.domain.Span
import app.tutora.domain.Who
import app.tutora.domain.layoutDay
import app.tutora.domain.localOf
import app.tutora.domain.weekStart
import app.tutora.ui.dayShortMonth
import app.tutora.ui.hourMark
import app.tutora.ui.placeName
import app.tutora.ui.tag
import app.tutora.ui.weekdayShort
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Groups have no color of their own; they all use one tag. */
private val GROUP_TAG = Tag.TEAL

val Lesson.whoName: String get() =
    when (val w = who) {
        is Who.OfStudent -> w.student.name
        is Who.OfGroup -> w.group.name
    }

val Lesson.whoTag: Tag get() =
    when (val w = who) {
        is Who.OfStudent -> w.student.color.tag()
        is Who.OfGroup -> GROUP_TAG
    }

val Lesson.whoSubject: String? get() =
    when (val w = who) {
        is Who.OfStudent -> w.student.subject
        is Who.OfGroup -> w.group.subject
    }

@Composable
fun lessonSubtitle(l: Lesson): String = listOfNotNull(l.whoSubject, placeName(l.place)).joinToString(" · ")

/** Classes by their local date, in time order. */
fun byLocalDay(
    lessons: List<Lesson>,
    zone: String,
): Map<LocalDate, List<Lesson>> = lessons.groupBy { localOf(it.startAt, zone).date }

/**
 * ‹ "Semana de 26 out a 1 nov" › and a Monday-to-Sunday strip; tapping a day picks it. The
 * month is the screen's title (AC-005-17, AC-016-13, 15); › stops at the [horizon] (AC-016-23).
 */
@Composable
fun WeekPicker(
    selected: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
    byDay: Map<LocalDate, List<Lesson>> = emptyMap(),
    horizon: LocalDate? = null,
) {
    val language = currentLanguage()
    val monday = weekStart(selected)
    val days = (0 until 7).map { monday.plus(DatePeriod(days = it)) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(IconName.CHEVRON_LEFT, stringResource(R.string.schedule_prevWeek), {
                onSelect(selected.minus(DatePeriod(days = 7)))
            }, variant = IconButtonVariant.PLAIN)
            Text(
                stringResource(R.string.schedule_weekOf, dayShortMonth(monday, language), dayShortMonth(days.last(), language)),
                style = TutoraTheme.type.bodyStrong,
                color = TutoraTheme.colors.ink,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(
                IconName.CHEVRON_RIGHT,
                stringResource(R.string.schedule_nextWeek),
                { onSelect(selected.plus(DatePeriod(days = 7))) },
                variant = IconButtonVariant.PLAIN,
                enabled = horizon == null || monday.plus(DatePeriod(days = 7)) <= horizon,
            )
        }
        // After a space as wide as the grid's hour labels, so each day sits over its column.
        WeekStrip(
            modifier = Modifier.padding(start = LABEL_WIDTH),
            days =
                days.map { d -> WeekDay(weekdayShort(d, language), d.day, byDay[d]?.firstOrNull()?.whoTag) },
            selected = days.indexOf(selected),
            onSelect = { onSelect(days[it]) },
            today = days.indexOf(today).takeIf { it >= 0 },
        )
    }
}

private val LABEL_WIDTH: Dp get() = Size.touch

/**
 * The 24-hour grid of one or more days side by side, with a block per class, opening at the
 * first class or the current hour (AC-005-18, 20, 21).
 */
@Composable
fun DayGrid(
    days: List<LocalDate>,
    byDay: Map<LocalDate, List<Lesson>>,
    zone: String,
    nextId: String?,
    compact: Boolean,
    startHour: Int,
    scroll: ScrollState,
    onOpen: (Lesson) -> Unit,
) {
    val language = currentLanguage()
    val hour = Size.agendaHour
    val density = LocalDensity.current
    LaunchedEffect(startHour, days) { scroll.scrollTo(with(density) { (hour * (startHour - 1).coerceAtLeast(0)).roundToPx() }) }
    Column(Modifier.verticalScroll(scroll).testTag("day-grid")) {
        Row(Modifier.height(hour * 24)) {
            Box(Modifier.width(LABEL_WIDTH)) {
                for (h in 0 until 24) {
                    Text(
                        hourMark(h, language),
                        style = TutoraTheme.type.meta,
                        color = TutoraTheme.colors.inkMuted,
                        maxLines = 1,
                        softWrap = false,
                        modifier =
                            Modifier.offset(
                                y =
                                    hour * h,
                            ),
                    )
                }
            }
            days.forEach { date ->
                val lessons = byDay[date].orEmpty()
                val spans =
                    lessons.map { l ->
                        val t = localOf(l.startAt, zone).time
                        Span(l.id, t.hour * 60 + t.minute, t.hour * 60 + t.minute + l.plannedMin)
                    }
                val slots = layoutDay(spans)
                BoxWithConstraints(Modifier.weight(1f).height(hour * 24).background(TutoraTheme.colors.background)) {
                    for (h in 0 until 24) {
                        Box(
                            Modifier
                                .offset(y = hour * h)
                                .fillMaxWidth()
                                .height(Size.border)
                                .background(TutoraTheme.colors.lineSoft),
                        )
                    }
                    lessons.forEachIndexed { i, l ->
                        val span = spans[i]
                        val slot = slots.getValue(l.id)
                        val width = maxWidth / slot.columns
                        AgendaBlock(
                            l.whoTag,
                            l.whoName,
                            subtitle = if (compact) null else lessonSubtitle(l),
                            state = blockState(l, nextId),
                            height = hour * l.plannedMin / 60,
                            onClick = { onOpen(l) },
                            modifier =
                                Modifier
                                    .offset(x = width * slot.column, y = hour * span.start / 60)
                                    .width(width)
                                    .padding(horizontal = Spacing.s0_5),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun worthText(w: app.tutora.domain.Worth): String =
    when (w) {
        is app.tutora.domain.Worth.Set ->
            app.tutora.designsystem.components
                .formatMoney(w.cents, currentLanguage())
        app.tutora.domain.Worth.Salary -> stringResource(R.string.groups_coveredBySalary)
        app.tutora.domain.Worth.Unset -> stringResource(R.string.places_noPrice)
    }
