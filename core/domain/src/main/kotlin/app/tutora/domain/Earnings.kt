package app.tutora.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
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
    return EarnedClass(c.lessonId, c.day, type, c.payer, c.payerName, gross, commission, minOf(bonus, gross), minutes)
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
)

/** What a payment gave on top of the classes it paid: earned and received on its day (AC-016-27). */
data class Tip(
    val day: LocalDate,
    val cents: Cents,
    val type: EarningsType,
    val payer: PayerRef,
    val payerName: String,
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

/** Six months ending at [last]'s month: earned and received in each (AC-009-12). */
fun trend(
    last: LocalDate,
    classes: List<EarnedClass>,
    received: List<Received>,
    salaries: List<Salary>,
    filter: TypeFilter,
    today: LocalDate,
    tips: List<Tip> = emptyList(),
): List<MonthPoint> =
    (5 downTo 0).map { back ->
        val month = monthOf(last).minus(DatePeriod(months = back))
        val inMonth = { d: LocalDate -> monthOf(d) == month }
        val s =
            summarize(
                classes.filter { inMonth(it.day) },
                received.filter { inMonth(it.day) },
                salaries,
                listOf(month).filter { it <= monthOf(today) },
                emptyMap(),
                filter,
                tips.filter { inMonth(it.day) },
            )
        MonthPoint(month, s.earned, s.received)
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
