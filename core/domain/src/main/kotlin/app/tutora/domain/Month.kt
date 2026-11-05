package app.tutora.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.plus

// The Agenda's month view (spec 016): the grid of a month, how far ahead it may look, and the
// dot each class puts under its day.

/** At least how far ahead the schedule is kept filled (AC-005-8), and so how far the Agenda looks. */
const val HORIZON_WEEKS = 8

/** One square of the month grid; [inMonth] is false for the neighbors' days that fill its weeks. */
data class MonthCell(
    val date: LocalDate,
    val inMonth: Boolean,
)

/**
 * Six weeks around [month], Monday to Sunday, padded with the days of the months around it
 * (AC-016-16). Always six, so the grid keeps its height from month to month (Victor, 4 Nov 2026).
 */
fun monthCells(month: YearMonth): List<MonthCell> {
    val first = month.firstDay
    val start = weekStart(first)
    val end = start.plus(DatePeriod(days = 6 * 7 - 1))
    return generateSequence(start) { it.plus(DatePeriod(days = 1)) }
        .takeWhile { it <= end }
        .map { MonthCell(it, it.month == first.month && it.year == first.year) }
        .toList()
}

/**
 * The last day classes are filled and the Agenda shows: 8 weeks from today, then on to the end of
 * that month, so the calendar and the lists end on a whole month (Victor, 4 Nov 2026; AC-016-23).
 */
fun agendaHorizon(today: LocalDate): LocalDate {
    val eight = today.plus(DatePeriod(days = HORIZON_WEEKS * 7))
    return LocalDate(eight.year, eight.month, 1).plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
}

/** The kind of dot a class puts under its day; cancelled and missed classes put none (AC-016-17). */
enum class DayKind { SCHOOL, DONE, PRIVATE, GROUP }

fun Lesson.dayKind(): DayKind? =
    when {
        status == LessonStatus.CANCELLED || status == LessonStatus.MISSED -> null
        status == LessonStatus.DONE -> DayKind.DONE
        who is Who.OfGroup -> DayKind.GROUP
        place.kind == PlaceKind.SCHOOL -> DayKind.SCHOOL
        else -> DayKind.PRIVATE
    }

/** Dots shown under one day; more than this reads "+N". */
const val MAX_DAY_DOTS = 2

/** The dots of each day, in time order, after the horizon none (AC-016-17, 23). */
fun dayDots(
    byDay: Map<LocalDate, List<Lesson>>,
    today: LocalDate,
): Map<LocalDate, List<DayKind>> {
    // Past the horizon the classes are forecasts from their series, and they get their dots too (AC-016-35).
    return byDay
        .mapValues { (_, lessons) -> lessons.mapNotNull { it.dayKind() } }
        .filterValues { it.isNotEmpty() }
}
