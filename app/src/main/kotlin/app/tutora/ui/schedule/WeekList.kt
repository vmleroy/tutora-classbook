package app.tutora.ui.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.tutora.R
import app.tutora.designsystem.components.AgendaCard
import app.tutora.designsystem.components.BusyBlock
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.ButtonVariant
import app.tutora.designsystem.components.DayHeading
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.tokens.TutoraTokens.Size
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.BusyDay
import app.tutora.domain.Lesson
import app.tutora.domain.counted
import app.tutora.domain.expected
import app.tutora.domain.localOf
import app.tutora.domain.worth
import app.tutora.ui.clockTime
import app.tutora.ui.countText
import app.tutora.ui.weekdayLong
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

// Semana as one list down the page (Victor, 4 Nov 2026, after Teams' agenda, in Tutora's look):
// each day's heading stays on top while its classes scroll under it, and the week strip follows.

/** One thing in a day: a class, or an appointment from the phone's other calendars. */
private sealed interface Entry {
    val minute: Int

    data class Class(
        val lesson: Lesson,
        override val minute: Int,
    ) : Entry

    data class Busy(
        val title: String,
        override val minute: Int,
        val end: Int,
    ) : Entry
}

/** A minute of the day as a time; a class ending at midnight ends at 00:00. */
private fun hm(m: Int) = (m % (24 * 60)).let { LocalTime(it / 60, it % 60) }

/** "Segunda" / "Monday": the weekday as Hoje writes it. */
private fun dayName(
    d: LocalDate,
    language: app.tutora.domain.Language,
) = weekdayLong(d, language).removeSuffix("-feira").replaceFirstChar { it.uppercase() }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeekList(
    days: List<LocalDate>,
    byDay: Map<LocalDate, List<Lesson>>,
    zone: String,
    today: LocalDate,
    selected: LocalDate,
    nextId: String?,
    onSelectDay: (LocalDate) -> Unit,
    onOpen: (Lesson) -> Unit,
    busy: (LocalDate) -> BusyDay?,
    onNextWeek: (() -> Unit)?,
) {
    val language = currentLanguage()
    val state = rememberLazyListState()
    // Where each day's heading sits in the list.
    val entries =
        days.associateWith { d ->
            val classes =
                byDay[d].orEmpty().map { l ->
                    val t = localOf(l.startAt, zone).time
                    Entry.Class(l, t.hour * 60 + t.minute)
                }
            val others = busy(d)?.blocks.orEmpty().map { (title, from, to) -> Entry.Busy(title, from, to) }
            (classes + others).sortedBy { it.minute }
        }
    val headerAt = mutableMapOf<LocalDate, Int>()
    var index = 0
    days.forEach { d ->
        headerAt[d] = index
        index += 1 + busy(d)?.bands.orEmpty().size + entries.getValue(d).size
    }
    // A day chosen on the strip scrolls there; scrolling moves the strip, without scrolling back.
    var fromScroll by remember { mutableStateOf(false) }
    // While the list slides to a chosen day, its passing days must not move the strip.
    var jumping by remember { mutableStateOf(false) }
    val currentSelect by rememberUpdatedState(onSelectDay)
    LaunchedEffect(selected, days) {
        if (fromScroll) {
            fromScroll = false
        } else {
            headerAt[selected]?.let {
                jumping = true
                try {
                    state.animateScrollToItem(it)
                } finally {
                    jumping = false
                }
            }
        }
    }
    LaunchedEffect(days) {
        snapshotFlow { state.firstVisibleItemIndex }.distinctUntilChanged().collect { first ->
            if (state.isScrollInProgress && !jumping) {
                val day = days.lastOrNull { (headerAt[it] ?: 0) <= first }
                if (day != null && day != selected) {
                    fromScroll = true
                    currentSelect(day)
                }
            }
        }
    }
    LazyColumn(
        Modifier.fillMaxWidth().testTag("week-list"),
        state = state,
        contentPadding = PaddingValues(start = Spacing.s5, end = Spacing.s5, bottom = Size.fab + Spacing.s7),
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        days.forEach { d ->
            val list = entries.getValue(d)
            val classes = list.filterIsInstance<Entry.Class>().map { it.lesson }
            stickyHeader(key = "day-$d") {
                val previsto = expected(classes.mapNotNull { it.counted() })
                val count = countText(R.string.schedule_count_zero, R.plurals.schedule_count, classes.size)
                // The tile has the day and the strip the month: the heading is the weekday, or Hoje.
                DayHeading(
                    d.day,
                    if (d == today) stringResource(R.string.schedule_today) else dayName(d, language),
                    Modifier.testTag("week-day-$d"),
                    aside =
                        if (list.isEmpty()) {
                            stringResource(R.string.schedule_noClassesDay)
                        } else if (previsto.cents > 0) {
                            stringResource(R.string.schedule_dayTotal, count, formatMoney(previsto.cents, language, compact = true))
                        } else {
                            count
                        },
                    today = d == today,
                )
            }
            busy(d)?.bands.orEmpty().forEach { band ->
                item(key = "band-$d-$band") { BusyBlock(band, Modifier.fillMaxWidth(), band = true) }
            }
            list.forEach { e ->
                when (e) {
                    is Entry.Class ->
                        item(key = "class-${e.lesson.id}") {
                            val l = e.lesson
                            val badge = statusBadge(l)
                            val start = localOf(l.startAt, zone).time
                            AgendaCard(
                                clockTime(start, language),
                                clockTime(hm(e.minute + l.plannedMin), language),
                                l.whoTag,
                                l.whoName,
                                subtitle = lessonSubtitle(l),
                                trailing = worthText(l.worth()),
                                status = badge?.let { it.first to it.second },
                                state = blockState(l, nextId),
                                onClick = { onOpen(l) },
                            )
                        }
                    is Entry.Busy ->
                        item(key = "busy-$d-${e.minute}-${e.title}") {
                            BusyBlock(
                                e.title,
                                Modifier.fillMaxWidth().testTag("busy-block"),
                                time = "${clockTime(hm(e.minute), language)} – ${clockTime(hm(e.end), language)}",
                            )
                        }
                }
            }
        }
        onNextWeek?.let { next ->
            item(key = "next-week") {
                Button(
                    stringResource(R.string.schedule_nextWeek),
                    next,
                    fullWidth = true,
                    variant = ButtonVariant.GHOST,
                    icon = IconName.CHEVRON_RIGHT,
                )
            }
        }
    }
}
