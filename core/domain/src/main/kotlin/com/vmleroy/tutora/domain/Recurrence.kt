package com.vmleroy.tutora.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

// Recurring classes: local calendar dates and wall-clock times, converted to UTC instants in
// the rule's own time zone (architecture, time rules).

enum class Repeat { WEEKLY, BIWEEKLY }

/** How far ahead repeating classes are kept filled: 8 weeks. */
const val HORIZON_DAYS = 56

/** The Monday of the week holding [date]; weeks start on Monday in both languages. */
fun weekStart(date: LocalDate): LocalDate = date.minus(DatePeriod(days = date.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber))

data class RuleDates(
    val startsOn: LocalDate,
    val endsOn: LocalDate?,
    val repeat: Repeat,
    val skips: Set<LocalDate> = emptySet(),
)

/** The rule's dates in `[from, to]`: every 7 or 14 days from its first date. */
fun occurrences(
    rule: RuleDates,
    from: LocalDate,
    to: LocalDate,
): List<LocalDate> {
    val step = if (rule.repeat == Repeat.WEEKLY) 7 else 14
    val last = if (rule.endsOn != null && rule.endsOn < to) rule.endsOn else to
    val behind = rule.startsOn.daysUntil(from)
    var date = if (behind > 0) rule.startsOn.plus(DatePeriod(days = (behind + step - 1) / step * step)) else rule.startsOn
    val dates = mutableListOf<LocalDate>()
    while (date <= last) {
        if (date !in rule.skips) dates += date
        date = date.plus(DatePeriod(days = step))
    }
    return dates
}

/** The UTC instant of a local date and time in [zone] (AC-005-9). */
fun localToInstant(
    date: LocalDate,
    time: LocalTime,
    zone: String,
): Instant = LocalDateTime(date, time).toInstant(TimeZone.of(zone))

/** UTC ISO 8601 with milliseconds and `Z`, fixed width, so text order is time order. */
fun Instant.isoText(): String {
    val t = toLocalDateTime(TimeZone.UTC)
    val ms = (t.nanosecond / 1_000_000).toString().padStart(3, '0')
    return "${t.date}T${t.hour.pad()}:${t.minute.pad()}:${t.second.pad()}.${ms}Z"
}

private fun Int.pad() = toString().padStart(2, '0')

fun instantOf(iso: String): Instant = Instant.parse(iso)

/** The local date and time of [iso] in [zone]. */
fun localOf(
    iso: String,
    zone: String,
): LocalDateTime = instantOf(iso).toLocalDateTime(TimeZone.of(zone))

/** The UTC `[from, to)` range of the local days `[first, last]`. */
fun dayRange(
    first: LocalDate,
    last: LocalDate,
    zone: String,
): Pair<String, String> =
    localToInstant(first, LocalTime(0, 0), zone).isoText() to
        localToInstant(last.plus(DatePeriod(days = 1)), LocalTime(0, 0), zone).isoText()
