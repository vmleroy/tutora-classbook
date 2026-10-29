package app.tutora.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.Avatar
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.ButtonVariant
import app.tutora.designsystem.components.Card
import app.tutora.designsystem.components.EmptyState
import app.tutora.designsystem.components.HeaderAction
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.IconButtonVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.LessonRow
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.SectionHeader
import app.tutora.designsystem.components.SplitButtons
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.Greeting
import app.tutora.domain.Lesson
import app.tutora.domain.Price
import app.tutora.domain.Who
import app.tutora.domain.dayRange
import app.tutora.domain.greeting
import app.tutora.domain.initials
import app.tutora.domain.instantOf
import app.tutora.domain.localOf
import app.tutora.domain.resolvePrice
import app.tutora.domain.worth
import app.tutora.state.LocalServices
import app.tutora.ui.LessonDetail
import app.tutora.ui.LessonNew
import app.tutora.ui.ScreenScaffold
import app.tutora.ui.clockTime
import app.tutora.ui.countText
import app.tutora.ui.durationText
import app.tutora.ui.earnings.TodayEarnings
import app.tutora.ui.live.LiveBanner
import app.tutora.ui.live.rememberStartLesson
import app.tutora.ui.longDate
import app.tutora.ui.minutesText
import app.tutora.ui.payments.PendingAlert
import app.tutora.ui.rateText
import app.tutora.ui.shortDate
import kotlinx.coroutines.launch

/** Hoje: the date and a greeting, the next class, and today's classes (AC-005-22 to 25). */
@Composable
fun TodayScreen(nav: NavHostController) {
    val services = LocalServices.current
    val language = currentLanguage()
    val zone = services.zone
    val now = remember { services.now() }
    val local = remember { localOf(now, zone) }
    val range = remember { dayRange(local.date, local.date, zone) }
    val lessons by remember { services.scheduleRepo.between(range.first, range.second) }.collectAsState(initial = emptyList())
    val next by remember { services.scheduleRepo.firstAfter(now) }.collectAsState(initial = null)
    val settings by services.settings.collectAsState(initial = null)
    val startLesson = rememberStartLesson(nav)
    val scope = rememberCoroutineScope()
    var marking by remember { mutableStateOf<Lesson?>(null) }

    @Composable
    fun whenText(l: Lesson): String {
        val at = localOf(l.startAt, zone)
        if (at.date != local.date) return "${shortDate(at.date, language)}, ${clockTime(at.time, language)}"
        val minutes = ((instantOf(l.startAt) - instantOf(now)).inWholeMinutes).toInt()
        if (minutes < 60) return stringResource(R.string.home_inMinutes, minutes.toString())
        val rest = minutes % 60
        return stringResource(R.string.home_inHours, "${minutes / 60}h${if (rest > 0) rest.toString().padStart(2, '0') else ""}")
    }

    @Composable
    fun rate(l: Lesson): String? {
        val price =
            when (val w = l.who) {
                is Who.OfStudent -> resolvePrice(l.place, w.student, w.link)
                is Who.OfGroup -> resolvePrice(l.place, group = w.group)
            }
        return (price as? Price.Set)?.let { rateText(it.cents, it.rateType) }
    }

    val g =
        when (greeting(local.hour)) {
            Greeting.MORNING -> R.string.home_greeting_morning
            Greeting.AFTERNOON -> R.string.home_greeting_afternoon
            Greeting.EVENING -> R.string.home_greeting_evening
        }
    // The date over the greeting, on the background like every header (AC-005-22, AC-016-1).
    ScreenScaffold(
        stringResource(g),
        "today-screen",
        variant = HeaderVariant.HERO,
        eyebrow = longDate(local.date, language),
        actions =
            listOf(
                HeaderAction(IconName.PLUS, stringResource(R.string.lesson_new), IconButtonVariant.ACCENT) { nav.navigate(LessonNew()) },
            ),
    ) {
        LiveBanner(nav)
        TodayEarnings(nav, lessons, local.date)
        next?.let { n ->
            Card(label = stringResource(R.string.home_next)) {
                Text(
                    "${stringResource(R.string.home_next)} · ${whenText(n)}",
                    style = TutoraTheme.type.caption,
                    color = TutoraTheme.colors.inkMuted,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(initials(n.whoName), n.whoTag)
                    Column(Modifier.weight(1f)) {
                        Text(n.whoName, style = TutoraTheme.type.bodyStrong, color = TutoraTheme.colors.ink)
                        Text(
                            listOfNotNull(lessonSubtitle(n), rate(n)).joinToString(" · "),
                            style = TutoraTheme.type.meta,
                            color = TutoraTheme.colors.inkMuted,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            clockTime(localOf(n.startAt, zone).time, language),
                            style = TutoraTheme.type.section.copy(fontFeatureSettings = "tnum"),
                            color = TutoraTheme.colors.ink,
                            maxLines = 1,
                            softWrap = false,
                        )
                        Text(durationText(n.plannedMin), style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
                    }
                }
                // Iniciar aula when the next class is today, while the timer is on (AC-007-1, 3); with
                // Marcar como dada antes do horário it is joined to Marcar como dada (AC-006-25, AC-016-5).
                val canStart = localOf(n.startAt, zone).date == local.date && settings?.liveTimer != false
                val canMark = settings?.earlyDone == true
                when {
                    canStart && canMark ->
                        SplitButtons(
                            stringResource(R.string.lesson_markDone),
                            { marking = n },
                            stringResource(R.string.live_start),
                            { startLesson(n.id) },
                            secondIcon = IconName.PLAY,
                        )
                    canStart -> Button(stringResource(R.string.live_start), { startLesson(n.id) }, fullWidth = true, icon = IconName.PLAY)
                    canMark ->
                        Button(
                            stringResource(R.string.lesson_markDone),
                            { marking = n },
                            fullWidth = true,
                            variant = ButtonVariant.OUTLINE,
                        )
                }
            }
        }
        PendingAlert(nav)
        if (lessons.isEmpty()) {
            EmptyState(
                stringResource(R.string.empty_today_title),
                icon = IconName.CALENDAR,
                body = stringResource(R.string.empty_today_body),
            )
        } else {
            SectionHeader(
                stringResource(R.string.home_lessonsToday),
                aside = countText(R.string.schedule_count_zero, R.plurals.schedule_count, lessons.size),
            )
            ListGroup(
                lessons.map<Lesson, @Composable () -> Unit> { l ->
                    {
                        // Done, cancelled and missed classes say so; a scheduled one shows its value (AC-006-15).
                        val badge = statusBadge(l)
                        LessonRow(
                            clockTime(localOf(l.startAt, zone).time, language),
                            l.whoTag,
                            l.whoName,
                            subtitle = "${lessonSubtitle(l)} · ${minutesText(l.plannedMin)}",
                            status = badge?.let { it.first to it.second },
                            trailing = worthText(l.worth()),
                            onClick = { nav.navigate(LessonDetail(l.id)) },
                            statusIcon = badge?.third,
                        )
                    }
                },
            )
        }
    }
    marking?.let { l ->
        DoneSheet(l, l.plannedMin, stringResource(R.string.lesson_markDone), { marking = null }) { minutes, bonus ->
            scope.launch {
                services.lessons.markDone(l.id, minutes)
                // The bonus the teacher chose comes off the class once it is done (AC-008-39).
                bonus?.let { services.payments.useCredit(it.payer, listOf(l.id), it.minutes, it.amount) }
            }
            marking = null
        }
    }
}
