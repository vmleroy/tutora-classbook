package app.tutora.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.ControlSize
import app.tutora.designsystem.components.DotTone
import app.tutora.designsystem.components.EmptyState
import app.tutora.designsystem.components.HeaderAction
import app.tutora.designsystem.components.IconButton
import app.tutora.designsystem.components.IconButtonVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.LessonRow
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.MonthDay
import app.tutora.designsystem.components.MonthGrid
import app.tutora.designsystem.components.MonthPicker
import app.tutora.designsystem.components.PickerMonth
import app.tutora.designsystem.components.ScreenHeader
import app.tutora.designsystem.components.SegmentedControl
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.DayKind
import app.tutora.domain.Lesson
import app.tutora.domain.LessonStatus
import app.tutora.domain.MAX_DAY_DOTS
import app.tutora.domain.agendaHorizon
import app.tutora.domain.canShowMonth
import app.tutora.domain.counted
import app.tutora.domain.dayDots
import app.tutora.domain.dayKind
import app.tutora.domain.dayRange
import app.tutora.domain.expected
import app.tutora.domain.localOf
import app.tutora.domain.monthCells
import app.tutora.domain.nextLesson
import app.tutora.domain.weekStart
import app.tutora.domain.worth
import app.tutora.state.LocalServices
import app.tutora.ui.LessonDetail
import app.tutora.ui.LessonNew
import app.tutora.ui.clockTime
import app.tutora.ui.countText
import app.tutora.ui.dayMonthLong
import app.tutora.ui.minutesText
import app.tutora.ui.monthShort
import app.tutora.ui.monthYear
import app.tutora.ui.shortDate
import app.tutora.ui.weekdayNarrow
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.plus
import kotlinx.datetime.plusMonth

enum class AgendaView { DAY, WEEK, MONTH }

private fun DayKind.tone() =
    when (this) {
        DayKind.SCHOOL -> DotTone.SECONDARY
        DayKind.DONE -> DotTone.SUCCESS
        DayKind.PRIVATE -> DotTone.ACCENT
        DayKind.GROUP -> DotTone.VIOLET
    }

/** The day to show in [month]: today when it is that month, else its first day. */
private fun dayIn(
    month: YearMonth,
    today: LocalDate,
): LocalDate = if (YearMonth(today.year, today.month) == month) today else month.firstDay

/**
 * Agenda: titled by the month and year (a button to the month picker), with Hoje, then Dia (one
 * day's grid), Semana (seven days) or Mês (the whole month), never past the 8-week horizon
 * (AC-005-17 to 21, AC-016-13 to 23).
 */
@Composable
fun AgendaScreen(nav: NavHostController) {
    val services = LocalServices.current
    val language = currentLanguage()
    val zone = services.zone
    val now = remember { services.now() }
    val today = remember { localOf(now, zone).date }
    val horizon = remember(today) { agendaHorizon(today) }
    var selectedText by rememberSaveable { mutableStateOf(today.toString()) }
    var viewName by rememberSaveable { mutableStateOf(AgendaView.DAY.name) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val view = AgendaView.valueOf(viewName)
    val selected = LocalDate.parse(selectedText)
    val month = YearMonth(selected.year, selected.month)
    val cells = remember(month) { monthCells(month) }
    val monday = weekStart(selected)
    val first = if (view == AgendaView.MONTH) cells.first().date else monday
    val last = if (view == AgendaView.MONTH) cells.last().date else monday.plus(DatePeriod(days = 6))
    val range = remember(first, last) { dayRange(first, last, zone) }
    val lessons by remember(range) { services.scheduleRepo.between(range.first, range.second) }.collectAsState(initial = emptyList())
    val byDay = byLocalDay(lessons, zone)
    val dayLessons = byDay[selected].orEmpty()
    val next = nextLesson(lessons.filter { it.status == LessonStatus.SCHEDULED }, now) { it.startAt }
    val firstLesson = (if (view == AgendaView.WEEK) lessons else dayLessons).firstOrNull()
    val startHour = firstLesson?.let { localOf(it.startAt, zone).hour } ?: localOf(now, zone).hour
    val scroll = rememberScrollState()
    val title = monthYear(selected, language)
    Column(
        Modifier
            .fillMaxSize()
            .background(TutoraTheme.colors.background)
            .safeDrawingPadding()
            .testTag("schedule-screen"),
    ) {
        ScreenHeader(
            title,
            eyebrow = stringResource(R.string.tab_schedule),
            onTitle = { picking = true },
            titleLabel = stringResource(R.string.schedule_pickMonth, title),
            actions =
                listOf(
                    HeaderAction(IconName.CALENDAR, stringResource(R.string.schedule_today), text = true) {
                        selectedText = today.toString()
                    },
                    HeaderAction(IconName.PLUS, stringResource(R.string.lesson_new), IconButtonVariant.ACCENT) {
                        nav.navigate(LessonNew(selected.toString()))
                    },
                ),
        )
        Column(Modifier.padding(horizontal = Spacing.s5), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            SegmentedControl(
                listOf(
                    AgendaView.DAY to stringResource(R.string.schedule_day),
                    AgendaView.WEEK to stringResource(R.string.schedule_week),
                    AgendaView.MONTH to stringResource(R.string.schedule_month),
                ),
                view,
                { viewName = it.name },
                stringResource(R.string.tab_schedule),
                size = ControlSize.SM,
            )
            if (view != AgendaView.MONTH) {
                WeekPicker(selected, today, { selectedText = it.toString() }, byDay, horizon)
                if (view == AgendaView.DAY) DayHeader(selected, today, dayLessons)
            }
        }
        when {
            view == AgendaView.MONTH ->
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Spacing.s5, vertical = Spacing.s3),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s3),
                ) {
                    MonthView(month, cells, selected, today, byDay, { selectedText = it.toString() }) { nav.navigate(LessonDetail(it.id)) }
                }
            lessons.isEmpty() ->
                Column(Modifier.padding(horizontal = Spacing.s5)) {
                    EmptyState(
                        stringResource(R.string.empty_schedule_title),
                        icon = IconName.CALENDAR,
                        body = stringResource(R.string.empty_schedule_body),
                    )
                }
            else ->
                Column(Modifier.padding(horizontal = Spacing.s5, vertical = Spacing.s2)) {
                    DayGrid(
                        days = if (view == AgendaView.WEEK) (0 until 7).map { monday.plus(DatePeriod(days = it)) } else listOf(selected),
                        byDay = byDay,
                        zone = zone,
                        nextId = next?.id,
                        compact = view == AgendaView.WEEK,
                        startHour = startHour,
                        scroll = scroll,
                        onOpen = { nav.navigate(LessonDetail(it.id)) },
                    )
                }
        }
    }
    if (picking) {
        MonthPickerSheet(month, today, { picking = false }, onToday = {
            selectedText = today.toString()
            picking = false
        }) { chosen ->
            selectedText = chosen.toString()
            viewName = AgendaView.MONTH.name
            picking = false
        }
    }
}

/** "Hoje · 2 aulas" and "Previsto R$ 380 + escola" over a day (AC-005-19). */
@Composable
private fun DayHeader(
    day: LocalDate,
    today: LocalDate,
    dayLessons: List<Lesson>,
) {
    val language = currentLanguage()
    val previsto = expected(dayLessons.mapNotNull { it.counted() })
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val title = if (day == today) stringResource(R.string.schedule_today) else shortDate(day, language)
        Text(
            "$title · ${countText(R.string.schedule_count_zero, R.plurals.schedule_count, dayLessons.size)}",
            style = TutoraTheme.type.section,
            color = TutoraTheme.colors.ink,
            modifier = Modifier.weight(1f),
        )
        val parts =
            listOfNotNull(
                if (previsto.cents >
                    0
                ) {
                    stringResource(R.string.schedule_expected, formatMoney(previsto.cents, language, compact = true))
                } else {
                    null
                },
                if (previsto.salary) stringResource(R.string.schedule_plusSchool) else null,
            )
        if (parts.isNotEmpty()) {
            Text(
                parts.joinToString(" "),
                style = TutoraTheme.type.caption.copy(fontFeatureSettings = "tnum"),
                color = TutoraTheme.colors.inkMuted,
            )
        }
    }
}

/**
 * Mês: ‹ month ›, the grid with a dot per class, and the chosen day's classes under it
 * (AC-016-16 to 19, 23).
 */
@Composable
private fun MonthView(
    month: YearMonth,
    cells: List<app.tutora.domain.MonthCell>,
    selected: LocalDate,
    today: LocalDate,
    byDay: Map<LocalDate, List<Lesson>>,
    onSelect: (LocalDate) -> Unit,
    onOpen: (Lesson) -> Unit,
) {
    val language = currentLanguage()
    val zone = LocalServices.current.zone
    val dots = dayDots(byDay, today)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(IconName.CHEVRON_LEFT, stringResource(R.string.schedule_prevMonth), {
            onSelect(dayIn(month.minusMonth(), today))
        }, variant = IconButtonVariant.PLAIN)
        Text(
            monthYear(month.firstDay, language),
            style = TutoraTheme.type.bodyStrong,
            color = TutoraTheme.colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            IconName.CHEVRON_RIGHT,
            stringResource(R.string.schedule_nextMonth),
            { onSelect(dayIn(month.plusMonth(), today)) },
            variant = IconButtonVariant.PLAIN,
            enabled = canShowMonth(month.plusMonth(), today),
        )
    }
    val days =
        cells.map { cell ->
            val kinds = dots[cell.date].orEmpty()
            MonthDay(
                cell.date.day,
                cell.inMonth,
                kinds.take(MAX_DAY_DOTS).map { it.tone() },
                (kinds.size - MAX_DAY_DOTS).coerceAtLeast(0),
                stringResource(
                    R.string.schedule_dayA11y,
                    dayMonthLong(cell.date, language),
                    countText(R.string.schedule_count_zero, R.plurals.schedule_count, kinds.size),
                ),
                today = cell.date == today,
            )
        }
    MonthGrid(
        days,
        cells.indexOfFirst { it.date == selected && it.inMonth }.takeIf { it >= 0 },
        { onSelect(cells[it].date) },
        cells.take(7).map { weekdayNarrow(it.date, language) },
        monthYear(month.firstDay, language),
        legend =
            listOf(
                DotTone.SECONDARY to stringResource(R.string.schedule_kindSchool),
                DotTone.SUCCESS to stringResource(R.string.schedule_kindDone),
                DotTone.ACCENT to stringResource(R.string.schedule_kindPrivate),
                DotTone.VIOLET to stringResource(R.string.schedule_kindGroup),
            ),
    )
    // The chosen day's classes, cancelled ones included, with their count and expected value.
    val dayLessons = if (selected <= agendaHorizon(today)) byDay[selected].orEmpty() else emptyList()
    val previsto = expected(dayLessons.mapNotNull { it.counted() })
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (selected == today) stringResource(R.string.schedule_today) else shortDate(selected, language),
            style = TutoraTheme.type.section,
            color = TutoraTheme.colors.ink,
            modifier = Modifier.weight(1f),
        )
        val count = countText(R.string.schedule_count_zero, R.plurals.schedule_count, dayLessons.size)
        Text(
            if (previsto.cents >
                0
            ) {
                stringResource(R.string.schedule_dayTotal, count, formatMoney(previsto.cents, language, compact = true))
            } else {
                count
            },
            style = TutoraTheme.type.caption.copy(fontFeatureSettings = "tnum"),
            color = TutoraTheme.colors.inkMuted,
            modifier = Modifier.testTag("month-day-total"),
        )
    }
    if (dayLessons.isNotEmpty()) {
        ListGroup(
            dayLessons.map<Lesson, @Composable () -> Unit> { l ->
                {
                    val badge = statusBadge(l)
                    LessonRow(
                        clockTime(localOf(l.startAt, zone).time, language),
                        l.whoTag,
                        l.whoName,
                        subtitle = "${lessonSubtitle(l)} · ${minutesText(l.plannedMin)}",
                        status = badge?.let { it.first to it.second },
                        trailing = worthText(l.worth()),
                        onClick = { onOpen(l) },
                        statusIcon = badge?.third,
                    )
                }
            },
        )
    }
}

/** The year and its twelve months with their classes; later than the horizon cannot be chosen (AC-016-20, 21, 23). */
@Composable
private fun MonthPickerSheet(
    shown: YearMonth,
    today: LocalDate,
    onDismiss: () -> Unit,
    onToday: () -> Unit,
    onPick: (LocalDate) -> Unit,
) {
    val services = LocalServices.current
    val language = currentLanguage()
    val zone = services.zone
    var year by rememberSaveable { mutableIntStateOf(shown.year) }
    val range = remember(year) { dayRange(LocalDate(year, 1, 1), LocalDate(year, 12, 31), zone) }
    val lessons by remember(range) { services.scheduleRepo.between(range.first, range.second) }.collectAsState(initial = emptyList())
    val horizon = agendaHorizon(today)
    val counts =
        lessons
            .filter { it.dayKind() != null }
            .map { localOf(it.startAt, zone).date }
            .filter { it <= horizon }
            .groupingBy { it.month }
            .eachCount()
    BottomSheet(stringResource(R.string.schedule_pickTitle), onDismiss) {
        MonthPicker(
            year.toString(),
            Month.entries.map { m ->
                val ym = YearMonth(year, m)
                val count = countText(R.string.schedule_count_zero, R.plurals.schedule_count, counts[m] ?: 0)
                PickerMonth(
                    monthShort(ym.firstDay, language),
                    count,
                    "${monthYear(ym.firstDay, language)}, $count",
                    canShowMonth(ym, today),
                )
            },
            if (shown.year == year) shown.month.ordinal else null,
            { onPick(dayIn(YearMonth(year, Month.entries[it]), today)) },
            stringResource(R.string.schedule_prevYear),
            { year -= 1 },
            stringResource(R.string.schedule_nextYear),
            { year += 1 },
            stringResource(R.string.schedule_backToday),
            onToday,
            nextEnabled = canShowMonth(YearMonth(year + 1, Month.JANUARY), today),
        )
    }
}
