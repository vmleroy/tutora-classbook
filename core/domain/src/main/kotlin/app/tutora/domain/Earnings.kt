package app.tutora.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus

// What the teacher earned, received and has to receive in a period (spec 009).

enum class PeriodKind(
    val months: Int,
) {
    /** One day, the anchor (Victor, 30 Oct 2026; AC-016-28). */
    DAY(1),
    MONTH(1),
    QUARTER(3),
    YEAR(12),
}

/** The first day of [date]'s month. */
fun monthOf(date: LocalDate): LocalDate = LocalDate(date.year, date.month.number, 1)

/**
 * Dia, Mês, Trimestre or Ano around [anchor] (AC-009-9, AC-016-28). A day is the anchor itself;
 * a month the calendar month; a quarter the three months ending at the anchor's month; a year
 * the calendar year.
 */
data class Period(
    val kind: PeriodKind,
    val anchor: LocalDate,
) {
    /** The first month in the period. */
    val firstMonth: LocalDate
        get() =
            when (kind) {
                PeriodKind.DAY, PeriodKind.MONTH -> monthOf(anchor)
                PeriodKind.QUARTER -> monthOf(anchor).minus(DatePeriod(months = 2))
                PeriodKind.YEAR -> LocalDate(anchor.year, 1, 1)
            }

    /** The last month in the period. */
    val lastMonth: LocalDate get() = firstMonth.plus(DatePeriod(months = kind.months - 1))

    /** First and last local day. */
    val first: LocalDate get() = if (kind == PeriodKind.DAY) anchor else firstMonth

    val last: LocalDate get() = if (kind == PeriodKind.DAY) anchor else lastMonth.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))

    private val step: DatePeriod get() = if (kind == PeriodKind.DAY) DatePeriod(days = 1) else DatePeriod(months = kind.months)

    fun previous() = copy(anchor = anchor.minus(step))

    fun next() = copy(anchor = anchor.plus(step))

    /** › stops at the period holding today (AC-009-9). */
    fun isCurrent(today: LocalDate) = today in first..last

    /** Each month of the period, up to [today]'s month; a day holds none, so a salary is not split into days. */
    fun months(today: LocalDate): List<LocalDate> =
        if (kind == PeriodKind.DAY) {
            emptyList()
        } else {
            (0 until kind.months).map { firstMonth.plus(DatePeriod(months = it)) }.filter { it <= monthOf(today) }
        }
}

/** Tudo, Particulares, Escola e emprego (AC-009-10). */
enum class TypeFilter {
    ALL,
    PRIVATE,
    JOB,
    ;

    fun matches(type: EarningsType) = this == ALL || (this == PRIVATE) == (type == EarningsType.PRIVATE)
}

/** A done or charged class as earnings read it. */
data class ClassRecord(
    val lessonId: String,
    val day: LocalDate,
    val status: LessonStatus,
    val minutes: Int,
    val valueCents: Cents?,
    val rateCents: Cents?,
    val rateType: RateType?,
    val travelCents: Cents?,
    val feeCents: Cents?,
    val payer: PayerRef?,
    val payerName: String,
    val placeKind: PlaceKind,
    val placePayer: Payer,
    val commissionBp: Int,
)

data class EarnedClass(
    val lessonId: String,
    val day: LocalDate,
    val type: EarningsType,
    val payer: PayerRef?,
    val payerName: String,
    val gross: Cents,
    val commission: Cents,
    val bonus: Cents,
    val minutes: Int = 0,
    /** Where it was given, for Evolução's place-kind filter (AC-014-35). */
    val placeKind: PlaceKind? = null,
) {
    /** What the teacher keeps: after the place's commission and what a bonus paid for. */
    val earned: Cents get() = (gross - commission - bonus).coerceAtLeast(0)
}

/**
 * What a class earns (AC-009-1 to 4). A done class is worth its typed value or its frozen price for
 * its minutes (a package's price per class), plus travel; a charged cancellation, its charge. Where
 * the student pays and the place keeps a commission, it comes off the class's value, never the
 * travel, rounded once. [bonus] is what bonuses covered on it. Null when it earns nothing.
 */
fun earnedOf(
    c: ClassRecord,
    bonus: Cents = 0,
): EarnedClass? {
    val type = earningsType(c.placeKind)
    val (gross, base) =
        when (c.status) {
            LessonStatus.DONE -> {
                val base =
                    c.valueCents ?: c.rateCents?.let { lessonValue(it, c.rateType ?: RateType.HOUR, c.minutes) } ?: return null
                (base + (c.travelCents ?: 0)) to base
            }
            LessonStatus.CANCELLED, LessonStatus.MISSED -> (c.feeCents?.takeIf { it > 0 } ?: return null) to 0L
            else -> return null
        }
    val commission =
        if (c.placePayer == Payer.STUDENT && c.commissionBp > 0 && base > 0) base - afterCommission(base, c.commissionBp) else 0L
    val minutes = if (c.status == LessonStatus.DONE) c.minutes else 0
    return EarnedClass(c.lessonId, c.day, type, c.payer, c.payerName, gross, commission, minOf(bonus, gross), minutes, c.placeKind)
}

/** A credit row as the bonus attribution reads it: oldest first. */
data class CreditEntry(
    val lessonId: String?,
    val cents: Cents,
    val minutes: Int,
    val isBonus: Boolean,
)

/**
 * What bonuses covered on each class of one payer (AC-009-4). Minutes always come from a bonus;
 * money put on classes counts as bonus while the money bonuses given last, oldest use first, and
 * the rest came from credit that was paid.
 */
fun bonusCover(entries: List<CreditEntry>): Map<String, Cents> {
    var bonusMoney = sum(entries.filter { it.isBonus }.map { it.cents })
    val cover = mutableMapOf<String, Cents>()
    for (e in entries.filter { !it.isBonus && it.lessonId != null }) {
        val id = e.lessonId ?: continue
        val part =
            if (e.minutes > 0) {
                e.cents
            } else {
                minOf(e.cents, bonusMoney).also { bonusMoney -= it }
            }
        if (part > 0) cover[id] = (cover[id] ?: 0) + part
    }
    return cover
}

/** A school's monthly salary and the months it exists in. */
data class Salary(
    val placeId: String,
    val name: String,
    val cents: Cents,
    val since: LocalDate,
    val until: LocalDate?,
)

/** A school's salary counts once in each month from the one it was added to the one it was archived in (AC-009-5). */
fun salaryMonths(
    s: Salary,
    months: List<LocalDate>,
): List<LocalDate> = months.filter { it >= monthOf(s.since) && (s.until == null || it <= monthOf(s.until)) }

/** A payment as earnings read it: its day, amount and what kind of work it pays for. */
data class Received(
    val day: LocalDate,
    val cents: Cents,
    val type: EarningsType,
    /** Who paid and the kind of place, for Evolução's filters (AC-014-35). */
    val payer: PayerRef? = null,
    val placeKind: PlaceKind? = null,
)

/** What a payment gave on top of the classes it paid: earned and received on its day (AC-016-27). */
data class Tip(
    val day: LocalDate,
    val cents: Cents,
    val type: EarningsType,
    val payer: PayerRef,
    val payerName: String,
    val placeKind: PlaceKind? = null,
)

data class Summary(
    val earned: Cents,
    val received: Cents,
    val toReceive: Cents,
    val commission: Cents,
    val bonus: Cents,
)

data class MonthPoint(
    val month: LocalDate,
    val earned: Cents,
    val received: Cents,
)

/** A row of "De onde veio": a payer or a school, with what it earned (AC-009-13). */
data class Source(
    val payer: PayerRef,
    val name: String,
    val cents: Cents,
)

/** Everything Ganhos shows for a period and filter. */
data class EarningsView(
    val summary: Summary,
    val trend: List<MonthPoint>,
    val sources: List<Source>,
    /** The period's classes and payments, for the earnings report (spec 010). */
    val classes: List<EarnedClass> = emptyList(),
    val received: List<Received> = emptyList(),
    /** The period's bars: weeks of a month, months of a quarter or a year (AC-014-30). */
    val bars: List<Bar> = emptyList(),
)

/**
 * Earned, received and to receive for [months] of classes and payments (AC-009-5 to 7, 11). A
 * school's salary is earned and received in each of its months; [left] is what is still owed on
 * each class.
 */
fun summarize(
    classes: List<EarnedClass>,
    received: List<Received>,
    salaries: List<Salary>,
    months: List<LocalDate>,
    left: Map<String, Cents>,
    filter: TypeFilter,
    tips: List<Tip> = emptyList(),
): Summary {
    val shown = classes.filter { filter.matches(it.type) }
    val salary =
        if (filter.matches(EarningsType.JOB)) sum(salaries.map { s -> s.cents * salaryMonths(s, months).size }) else 0L
    // A tip is earned as well as received (AC-016-27).
    val tipped = sum(tips.filter { filter.matches(it.type) }.map { it.cents })
    return Summary(
        earned = sum(shown.map { it.earned }) + salary + tipped,
        received = sum(received.filter { filter.matches(it.type) }.map { it.cents }) + salary,
        toReceive = sum(shown.map { left[it.lessonId] ?: 0 }),
        commission = sum(shown.map { it.commission }),
        bonus = sum(shown.map { it.bonus }),
    )
}

/** One bar: earned and received from [first] to [last] (local days). */
data class Bar(
    val first: LocalDate,
    val last: LocalDate,
    val earned: Cents,
    val received: Cents,
)

/**
 * The spans a period's bars cover (AC-014-30): none for Dia, the weeks of a month (Monday to
 * Sunday, cut at the month's ends), and the months of a quarter or a year.
 */
fun periodSpans(period: Period): List<Pair<LocalDate, LocalDate>> =
    when (period.kind) {
        PeriodKind.DAY -> emptyList()
        PeriodKind.MONTH -> {
            val spans = mutableListOf<Pair<LocalDate, LocalDate>>()
            var start = period.first
            while (start <= period.last) {
                val toSunday = (7 - start.dayOfWeek.isoDayNumber)
                val end = minOf(start.plus(DatePeriod(days = toSunday)), period.last)
                spans += start to end
                start = end.plus(DatePeriod(days = 1))
            }
            spans
        }
        else -> monthSpans(period.firstMonth, period.kind.months)
    }

/** [count] whole months from [firstMonth], for the quarter, the year and Evolução (AC-014-31). */
fun monthSpans(
    firstMonth: LocalDate,
    count: Int,
): List<Pair<LocalDate, LocalDate>> =
    (0 until count).map { i ->
        val m = monthOf(firstMonth).plus(DatePeriod(months = i))
        m to m.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
    }

/** The longest period Evolução shows (AC-019-2). */
const val EVOLUTION_MAX_YEARS = 5

/**
 * The months of a gap of dates, from [from] to [to] (both included), the first and last cut to
 * it: Evolução shows only what falls inside the gap, month by month (AC-019-3).
 */
fun rangeSpans(
    from: LocalDate,
    to: LocalDate,
): List<Pair<LocalDate, LocalDate>> =
    generateSequence(monthOf(from)) { it.plus(DatePeriod(months = 1)) }
        .takeWhile { it <= to }
        .map { m -> maxOf(m, from) to minOf(m.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1)), to) }
        .toList()

/** De never after Até, Até never after [today], and at most [EVOLUTION_MAX_YEARS] (AC-019-2). */
fun evolutionPeriodOk(
    from: LocalDate,
    to: LocalDate,
    today: LocalDate,
): Boolean = from <= to && to <= today && to < from.plus(DatePeriod(years = EVOLUTION_MAX_YEARS))

/**
 * Earned and received in each span, following [filter]. A school's salary counts in the span
 * holding its month's first day, once that month has begun (AC-009-5, AC-014-30).
 */
fun bars(
    spans: List<Pair<LocalDate, LocalDate>>,
    classes: List<EarnedClass>,
    received: List<Received>,
    salaries: List<Salary>,
    filter: TypeFilter,
    today: LocalDate,
    tips: List<Tip> = emptyList(),
): List<Bar> =
    spans.map { (first, last) ->
        val inSpan = { d: LocalDate -> d >= first && d <= last }
        val months = listOf(monthOf(last)).filter { inSpan(it) && it <= monthOf(today) }
        val s =
            summarize(
                classes.filter { inSpan(it.day) },
                received.filter { inSpan(it.day) },
                salaries,
                months,
                emptyMap(),
                filter,
                tips.filter { inSpan(it.day) },
            )
        Bar(first, last, s.earned, s.received)
    }

/** Six months ending at [last]'s month, for the earnings report (spec 010). */
fun trend(
    last: LocalDate,
    classes: List<EarnedClass>,
    received: List<Received>,
    salaries: List<Salary>,
    filter: TypeFilter,
    today: LocalDate,
    tips: List<Tip> = emptyList(),
): List<MonthPoint> =
    bars(monthSpans(monthOf(last).minus(DatePeriod(months = 5)), 6), classes, received, salaries, filter, today, tips)
        .map { MonthPoint(it.first, it.earned, it.received) }

/** Two months side by side in Evolução: the later minus the earlier, and that as a percent of the earlier (AC-014-31). */
data class Comparison(
    val earlier: Bar,
    val later: Bar,
    val difference: Cents,
    /** Whole percent, rounded half away from zero; null when the earlier month earned nothing. */
    val percent: Long?,
)

/**
 * Evolução's filters: the type, any number of payers (students, groups, places) and of place
 * kinds; an empty set is any (AC-014-35, AC-019-8, AC-019-12).
 */
data class EvolutionFilter(
    val type: TypeFilter = TypeFilter.ALL,
    val payers: Set<PayerRef> = emptySet(),
    val kinds: Set<PlaceKind> = emptySet(),
) {
    private fun fits(
        p: PayerRef?,
        k: PlaceKind?,
    ) = (payers.isEmpty() || p in payers) && (kinds.isEmpty() || k in kinds)

    fun classes(list: List<EarnedClass>) = list.filter { fits(it.payer, it.placeKind) }

    fun received(list: List<Received>) = list.filter { fits(it.payer, it.placeKind) }

    fun tips(list: List<Tip>) = list.filter { fits(it.payer, it.placeKind) }

    /** A school's salary is that school's, and a Escola's. */
    fun salaries(list: List<Salary>) = list.filter { fits(PayerRef(PayerKind.PLACE, it.placeId), PlaceKind.SCHOOL) }
}

/** One month of Evolução: its totals, its classes and hours, and the change from the month before (AC-014-34). */
data class MonthDetail(
    val bar: Bar,
    val toReceive: Cents,
    val classes: Int,
    val minutes: Int,
    /** Against the month before; null for the first month of the range. */
    val change: Comparison?,
)

fun monthDetails(
    spans: List<Pair<LocalDate, LocalDate>>,
    classes: List<EarnedClass>,
    received: List<Received>,
    salaries: List<Salary>,
    filter: EvolutionFilter,
    today: LocalDate,
    left: Map<String, Cents>,
    tips: List<Tip> = emptyList(),
): List<MonthDetail> {
    val shownClasses = filter.classes(classes).filter { filter.type.matches(it.type) }
    val b =
        bars(spans, filter.classes(classes), filter.received(received), filter.salaries(salaries), filter.type, today, filter.tips(tips))
    return b.mapIndexed { i, bar ->
        val inMonth = shownClasses.filter { it.day >= bar.first && it.day <= bar.last }
        MonthDetail(
            bar,
            sum(inMonth.map { left[it.lessonId] ?: 0 }),
            inMonth.size,
            inMonth.sumOf { it.minutes },
            if (i > 0) compareBars(b[i - 1], bar) else null,
        )
    }
}

fun compareBars(
    a: Bar,
    b: Bar,
): Comparison {
    val (earlier, later) = if (a.first <= b.first) a to b else b to a
    val diff = later.earned - earlier.earned
    val percent = if (earlier.earned > 0) roundHalfAwayFromZero(diff * 100, earlier.earned) else null
    return Comparison(earlier, later, diff, percent)
}

/** "De onde veio": what each payer and each school's salary earned, largest first (AC-009-13). */
fun sources(
    classes: List<EarnedClass>,
    salaries: List<Salary>,
    months: List<LocalDate>,
    filter: TypeFilter,
    tips: List<Tip> = emptyList(),
): List<Source> {
    val byPayer =
        classes
            .filter { filter.matches(it.type) && it.payer != null }
            .groupBy { it.payer }
            .mapNotNull { (payer, list) -> payer?.let { Source(it, list.first().payerName, sum(list.map { c -> c.earned })) } }
    val schools =
        if (filter.matches(EarningsType.JOB)) {
            salaries.map { s -> Source(PayerRef(PayerKind.PLACE, s.placeId), s.name, s.cents * salaryMonths(s, months).size) }
        } else {
            emptyList()
        }
    val tipped = tips.filter { filter.matches(it.type) }.map { Source(it.payer, it.payerName, it.cents) }
    return (byPayer + schools + tipped)
        .groupBy { it.payer }
        .map { (payer, list) -> Source(payer, list.first().name, sum(list.map { it.cents })) }
        .filter { it.cents > 0 }
        .sortedByDescending { it.cents }
}

/** What a class still on the schedule or done is expected to bring, after the place's commission, for Hoje's previstos (AC-009-8). */
fun Lesson.keptValue(): Cents? {
    val w = counted() as? Worth.Set ?: return null
    val travel = if (status == LessonStatus.DONE) travelFeeCents ?: 0 else travelFor(place, travelFeeCents)
    val charged = status == LessonStatus.CANCELLED || status == LessonStatus.MISSED
    val base = if (charged) 0 else w.cents - travel
    val cut =
        if (place.payer == Payer.STUDENT &&
            place.commissionBp > 0 &&
            base > 0
        ) {
            base - afterCommission(base, place.commissionBp)
        } else {
            0
        }
    return w.cents - cut
}
