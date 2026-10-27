package com.vmleroy.tutora.app.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.vmleroy.tutora.app.R
import com.vmleroy.tutora.app.state.LocalServices
import com.vmleroy.tutora.app.ui.LessonEdit
import com.vmleroy.tutora.app.ui.LessonNew
import com.vmleroy.tutora.app.ui.ScreenScaffold
import com.vmleroy.tutora.app.ui.clockTime
import com.vmleroy.tutora.app.ui.countText
import com.vmleroy.tutora.app.ui.durationText
import com.vmleroy.tutora.app.ui.longDate
import com.vmleroy.tutora.app.ui.minutesText
import com.vmleroy.tutora.app.ui.rateText
import com.vmleroy.tutora.app.ui.shortDate
import com.vmleroy.tutora.designsystem.TutoraTheme
import com.vmleroy.tutora.designsystem.components.Avatar
import com.vmleroy.tutora.designsystem.components.Card
import com.vmleroy.tutora.designsystem.components.EmptyState
import com.vmleroy.tutora.designsystem.components.HeaderAction
import com.vmleroy.tutora.designsystem.components.IconButtonVariant
import com.vmleroy.tutora.designsystem.components.IconName
import com.vmleroy.tutora.designsystem.components.LessonRow
import com.vmleroy.tutora.designsystem.components.SectionHeader
import com.vmleroy.tutora.designsystem.components.currentLanguage
import com.vmleroy.tutora.designsystem.tokens.TutoraTokens.Spacing
import com.vmleroy.tutora.domain.Greeting
import com.vmleroy.tutora.domain.Lesson
import com.vmleroy.tutora.domain.Price
import com.vmleroy.tutora.domain.Who
import com.vmleroy.tutora.domain.dayRange
import com.vmleroy.tutora.domain.greeting
import com.vmleroy.tutora.domain.initials
import com.vmleroy.tutora.domain.instantOf
import com.vmleroy.tutora.domain.localOf
import com.vmleroy.tutora.domain.resolvePrice
import com.vmleroy.tutora.domain.worth

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

    ScreenScaffold(
        stringResource(R.string.tab_today),
        "today-screen",
        actions =
            listOf(
                HeaderAction(IconName.PLUS, stringResource(R.string.lesson_new), IconButtonVariant.ACCENT) { nav.navigate(LessonNew()) },
            ),
    ) {
        Column {
            Text(longDate(local.date, language), style = TutoraTheme.type.caption, color = TutoraTheme.colors.inkMuted)
            val g =
                when (greeting(local.hour)) {
                    Greeting.MORNING -> R.string.home_greeting_morning
                    Greeting.AFTERNOON -> R.string.home_greeting_afternoon
                    Greeting.EVENING -> R.string.home_greeting_evening
                }
            Text(stringResource(g), style = TutoraTheme.type.titleXl, color = TutoraTheme.colors.ink)
        }
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
            }
        }
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
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                lessons.forEach { l ->
                    LessonRow(
                        clockTime(localOf(l.startAt, zone).time, language),
                        l.whoTag,
                        l.whoName,
                        subtitle = "${lessonSubtitle(l)} · ${minutesText(l.plannedMin)}",
                        trailing = worthText(l.worth()),
                        onClick = { nav.navigate(LessonEdit(l.id)) },
                    )
                }
            }
        }
    }
}
