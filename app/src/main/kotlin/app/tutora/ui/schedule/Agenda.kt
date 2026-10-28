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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.ControlSize
import app.tutora.designsystem.components.EmptyState
import app.tutora.designsystem.components.HeaderAction
import app.tutora.designsystem.components.IconButtonVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ScreenHeader
import app.tutora.designsystem.components.SegmentedControl
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.dayRange
import app.tutora.domain.expected
import app.tutora.domain.localOf
import app.tutora.domain.nextLesson
import app.tutora.domain.weekStart
import app.tutora.domain.worth
import app.tutora.state.LocalServices
import app.tutora.ui.LessonEdit
import app.tutora.ui.LessonNew
import app.tutora.ui.countText
import app.tutora.ui.shortDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** Agenda: the week, then Dia (one day's grid) or Semana (seven days), with Previsto (AC-005-17 to 21). */
@Composable
fun AgendaScreen(nav: NavHostController) {
    val services = LocalServices.current
    val language = currentLanguage()
    val zone = services.zone
    val now = remember { services.now() }
    val today = remember { localOf(now, zone).date }
    var selectedText by rememberSaveable { mutableStateOf(today.toString()) }
    var week by rememberSaveable { mutableStateOf(false) }
    val selected = LocalDate.parse(selectedText)
    val monday = weekStart(selected)
    val range = remember(monday) { dayRange(monday, monday.plus(DatePeriod(days = 6)), zone) }
    val lessons by remember(range) { services.scheduleRepo.between(range.first, range.second) }.collectAsState(initial = emptyList())
    val byDay = byLocalDay(lessons, zone)
    val dayLessons = byDay[selected].orEmpty()
    val next = nextLesson(lessons, now) { it.startAt }
    val previsto = expected(dayLessons.map { it.worth() })
    val first = (if (week) lessons else dayLessons).firstOrNull()
    val startHour = first?.let { localOf(it.startAt, zone).hour } ?: localOf(now, zone).hour
    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .background(TutoraTheme.colors.background)
            .safeDrawingPadding()
            .testTag("schedule-screen"),
    ) {
        ScreenHeader(
            stringResource(R.string.tab_schedule),
            actions =
                listOf(
                    HeaderAction(IconName.PLUS, stringResource(R.string.lesson_new), IconButtonVariant.ACCENT) {
                        nav.navigate(LessonNew(selected.toString()))
                    },
                ),
        )
        Column(Modifier.padding(horizontal = Spacing.s5), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            WeekPicker(selected, today, { selectedText = it.toString() }, byDay)
            SegmentedControl(
                listOf(false to stringResource(R.string.schedule_day), true to stringResource(R.string.schedule_week)),
                week,
                { week = it },
                stringResource(R.string.tab_schedule),
                size = ControlSize.SM,
            )
            if (!week) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val title = if (selected == today) stringResource(R.string.schedule_today) else shortDate(selected, language)
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
        }
        if (lessons.isEmpty()) {
            Column(Modifier.padding(horizontal = Spacing.s5)) {
                EmptyState(
                    stringResource(R.string.empty_schedule_title),
                    icon = IconName.CALENDAR,
                    body = stringResource(R.string.empty_schedule_body),
                )
            }
        } else {
            Column(Modifier.padding(horizontal = Spacing.s5, vertical = Spacing.s2)) {
                DayGrid(
                    days = if (week) (0 until 7).map { monday.plus(DatePeriod(days = it)) } else listOf(selected),
                    byDay = byDay,
                    zone = zone,
                    nextId = next?.id,
                    compact = week,
                    startHour = startHour,
                    scroll = scroll,
                    onOpen = { nav.navigate(LessonEdit(it.id)) },
                )
            }
        }
    }
}
