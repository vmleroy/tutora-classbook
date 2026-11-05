package app.tutora.ui.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.tutora.R
import app.tutora.designsystem.components.AgendaCard
import app.tutora.designsystem.components.BusyBlock
import app.tutora.designsystem.components.DayHeading
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.tokens.TutoraTokens.Size
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.BusyEvent
import app.tutora.domain.Language
import app.tutora.domain.Lesson
import app.tutora.domain.busyFor
import app.tutora.domain.counted
import app.tutora.domain.dayRange
import app.tutora.domain.expected
import app.tutora.domain.instantOf
import app.tutora.domain.localOf
import app.tutora.domain.nextLesson
import app.tutora.domain.weekStart
import app.tutora.domain.worth
import app.tutora.ui.clockTime
import app.tutora.ui.countText
import app.tutora.ui.monthShort
import app.tutora.ui.weekdayLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus

// Semana as one endless list of days (Victor, 4 Nov 2026, after Teams' agenda, in Tutora's look):
// each day's heading stays on top while its classes scroll under it; the day being read is
// highlighted here and on the strip; more weeks load as the teacher nears either end, up to the
// 8-week horizon.

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
    language: Language,
) = weekdayLong(d, language).removeSuffix("-feira").replaceFirstChar { it.uppercase() }

private val WEEK = DatePeriod(days = 7)

/** How close to either end, in rows, the list loads another week. */
private const val LOAD_AHEAD = 6

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeekList(
    selected: LocalDate,
    today: LocalDate,
    zone: String,
    /** Now, so "Próxima" marks the next class, not the next one of the week being shown. */
    now: String,
    lessonsBetween: (LocalDate, LocalDate) -> Flow<List<Lesson>>,
    busyBetween: (Long, Long) -> Flow<List<BusyEvent>>,
    onSelectDay: (LocalDate) -> Unit,
    onOpen: (Lesson) -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = currentLanguage()
    val state = rememberLazyListState()

    // The days loaded, whole weeks from Monday; grows at either end (saved across rotation).
    var fromText by rememberSaveable { mutableStateOf(weekStart(selected).toString()) }
    var toText by rememberSaveable { mutableStateOf(weekStart(selected).plus(DatePeriod(days = 6)).toString()) }
    val from = LocalDate.parse(fromText)
    val to = LocalDate.parse(toText)
    val range = remember(from, to) { dayRange(from, to, zone) }
    val lessons by remember(from, to) { lessonsBetween(from, to) }.collectAsState(initial = emptyList())
    val busy by remember(range) {
        busyBetween(instantOf(range.first).toEpochMilliseconds(), instantOf(range.second).toEpochMilliseconds())
    }.collectAsState(initial = emptyList())
    val days = generateSequence(from) { it.plus(DatePeriod(days = 1)) }.takeWhile { it <= to }.toList()
    val byDay = lessons.groupBy { localOf(it.startAt, zone).date }
    val nextId = nextLesson(lessons.filter { it.status == app.tutora.domain.LessonStatus.SCHEDULED }, now) { it.startAt }?.id
    val entries =
        days.associateWith { d ->
            val classes =
                byDay[d].orEmpty().map { l ->
                    val t = localOf(l.startAt, zone).time
                    Entry.Class(l, t.hour * 60 + t.minute)
                }
            val others = busyFor(busy, d, zone).blocks.map { (title, start, end) -> Entry.Busy(title, start, end) }
            (classes + others).sortedBy { it.minute }
        }
    val bands = days.associateWith { busyFor(busy, it, zone).bands }
    // Where each day's heading sits in the list.
    val headerAt = mutableMapOf<LocalDate, Int>()
    var index = 0

    days.forEach { d ->
        headerAt[d] = index
        index += 1 + bands.getValue(d).size + entries.getValue(d).size
    }
    val currentHeaders by rememberUpdatedState(headerAt.toMap())
    // The day being read: the last heading at or above the top of the list.
    val shown by remember {
        derivedStateOf {
            val first = state.firstVisibleItemIndex
            currentHeaders.entries
                .filter { it.value <= first }
                .maxByOrNull { it.value }
                ?.key
        }
    }
    // A day chosen on the strip scrolls there, loading its week first; scrolling moves the strip.
    var fromScroll by remember { mutableStateOf(false) }
    var jumping by remember { mutableStateOf(false) }
    // Until the list has first reached the chosen day, its top says nothing about what is read.
    var ready by remember { mutableStateOf(false) }
    val select by rememberUpdatedState(onSelectDay)
    LaunchedEffect(selected) {
        if (fromScroll) {
            fromScroll = false
            return@LaunchedEffect
        }
        if (selected < from) fromText = weekStart(selected).toString()
        if (selected > to) toText = weekStart(selected).plus(DatePeriod(days = 6)).toString()
        // Waits for the day's week to be loaded and laid out.
        val at = snapshotFlow { currentHeaders[selected] }.filterNotNull().first()
        jumping = true
        try {
            if (ready) state.animateScrollToItem(at) else state.scrollToItem(at)
        } finally {
            jumping = false
            ready = true
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { shown }.distinctUntilChanged().collect { day ->
            if (day != null && ready && !jumping && day != selected) {
                fromScroll = true
                select(day)
            }
        }
    }
    // Endless both ways: near the bottom the next week loads, near the top the one before; past
    // the horizon its classes are forecasts from their series (AC-016-35).
    LaunchedEffect(Unit) {
        snapshotFlow {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            Triple(state.firstVisibleItemIndex, last, info.totalItemsCount)
        }.collect { (first, last, total) ->
            val end = LocalDate.parse(toText)
            if (total > 0 && last >= total - LOAD_AHEAD) toText = end.plus(WEEK).toString()
            if (first <= 1 && state.isScrollInProgress && !jumping) {
                fromText = LocalDate.parse(fromText).minus(WEEK).toString()
            }
        }
    }
    LazyColumn(
        modifier.fillMaxWidth().testTag("week-list"),
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
                        when {
                            list.isEmpty() -> stringResource(R.string.schedule_noClassesDay)
                            previsto.cents > 0 ->
                                stringResource(R.string.schedule_dayTotal, count, formatMoney(previsto.cents, language, compact = true))
                            else -> count
                        },
                    today = d == today,
                    current = d == shown,
                    month = monthShort(d, language),
                )
            }
            bands.getValue(d).forEach { band ->
                item(key = "band-$d-$band") { BusyBlock(band, Modifier.fillMaxWidth(), band = true) }
            }
            list.forEach { e ->
                when (e) {
                    is Entry.Class ->
                        item(key = "class-${e.lesson.id}") {
                            val l = e.lesson
                            val badge = statusBadge(l)
                            AgendaCard(
                                clockTime(hm(e.minute), language),
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
    }
}
