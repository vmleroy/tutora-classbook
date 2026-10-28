package app.tutora.domain

import kotlinx.datetime.LocalDate

/** Who a class is for. */
sealed interface Who {
    data class OfStudent(
        val student: Student,
        val link: PlaceLink?,
    ) : Who

    data class OfGroup(
        val group: Group,
    ) : Who
}

data class Lesson(
    val id: String,
    val ruleId: String?,
    val occurrenceDate: LocalDate?,
    val repeat: Repeat?,
    val startAt: String,
    val plannedMin: Int,
    /** A value the teacher typed for this class; null = follow the price. */
    val valueCents: Cents?,
    /** Travel for this class at Casa do Aluno when travel "varies". */
    val travelFeeCents: Cents?,
    val isDetached: Boolean,
    val notes: String?,
    val who: Who,
    val place: Place,
    val status: LessonStatus = LessonStatus.SCHEDULED,
    /** The price frozen when the class was marked done; null while it follows the price. */
    val frozenRate: Price.Set? = null,
    /** A kept cancellation or no-show fee. */
    val feeCents: Cents? = null,
    val feeWaived: Boolean = false,
    val noticeAt: String? = null,
)

/** The travel fee a class adds: only at Casa do Aluno. */
fun travelFor(
    place: Place,
    lessonTravel: Cents?,
): Cents =
    when {
        place.kind != PlaceKind.STUDENT_HOME -> 0
        place.travelMode == TravelMode.FIXED -> place.travelFeeCents
        place.travelMode == TravelMode.VARIES -> lessonTravel ?: 0
        else -> 0
    }

sealed interface Worth {
    data class Set(
        val cents: Cents,
        val source: PriceSource,
    ) : Worth

    data object Unset : Worth

    data object Salary : Worth
}

/**
 * What a scheduled class is expected to earn. A hand-typed value wins; otherwise the current
 * price for its duration, so price changes reach every scheduled class (AC-005-16). Travel
 * is added on top.
 */
fun lessonWorth(
    place: Place,
    who: Who,
    plannedMin: Int,
    valueCents: Cents?,
    travelFeeCents: Cents?,
): Worth {
    val travel = travelFor(place, travelFeeCents)
    if (valueCents != null) return Worth.Set(valueCents + travel, PriceSource.HAND)
    val price =
        when (who) {
            is Who.OfStudent -> resolvePrice(place, who.student, who.link)
            is Who.OfGroup -> resolvePrice(place, group = who.group)
        }
    return when (price) {
        is Price.Set -> Worth.Set(lessonValue(price.cents, price.rateType, plannedMin) + travel, price.source)
        Price.Unset -> Worth.Unset
        Price.Salary -> Worth.Salary
    }
}

/** The price a class charges now, to freeze when it is done; null when it has none. */
fun currentPrice(
    place: Place,
    who: Who,
): Price.Set? =
    when (who) {
        is Who.OfStudent -> resolvePrice(place, who.student, who.link)
        is Who.OfGroup -> resolvePrice(place, group = who.group)
    } as? Price.Set

/**
 * What a class is worth. A done class keeps its frozen price, applied to its duration, and the
 * travel stored on it, so price changes no longer reach it (AC-006-2, 3, 12).
 */
fun Lesson.worth(): Worth {
    if (status != LessonStatus.DONE) return lessonWorth(place, who, plannedMin, valueCents, travelFeeCents)
    val travel = travelFeeCents ?: 0
    val base =
        valueCents ?: frozenRate?.let { lessonValue(it.cents, it.rateType, plannedMin) }
            ?: return if (place.kind == PlaceKind.SCHOOL) Worth.Salary else Worth.Unset
    return Worth.Set(base + travel, if (valueCents != null) PriceSource.HAND else frozenRate?.source ?: PriceSource.HAND)
}

/** The class's value without travel, which a percentage fee is taken from (AC-006-24). */
fun Lesson.valueWithoutTravel(): Cents? {
    val w = worth() as? Worth.Set ?: return null
    val travel = if (status == LessonStatus.DONE) travelFeeCents ?: 0 else travelFor(place, travelFeeCents)
    return w.cents - travel
}

/** What Previsto adds for a class: its worth while scheduled or done, a kept fee otherwise (AC-006-16). */
fun Lesson.counted(): Worth? =
    when (status) {
        LessonStatus.SCHEDULED, LessonStatus.DONE -> worth()
        LessonStatus.CANCELLED, LessonStatus.MISSED -> feeCents?.let { Worth.Set(it, PriceSource.HAND) }
    }

data class Expected(
    val cents: Cents,
    val salary: Boolean,
)

/** Previsto: the priced classes, and whether a salary covers others (AC-005-19). */
fun expected(worths: List<Worth>): Expected =
    Expected(
        sum(worths.filterIsInstance<Worth.Set>().map { it.cents }),
        worths.any { it == Worth.Salary },
    )

data class Span(
    val id: String,
    val start: Int,
    val end: Int,
)

data class Slot(
    val column: Int,
    val columns: Int,
)

/** Side-by-side columns for overlapping classes; a cluster of overlaps shares its column count. */
fun layoutDay(spans: List<Span>): Map<String, Slot> {
    val result = mutableMapOf<String, Slot>()
    val cluster = mutableListOf<Pair<String, Int>>()
    val ends = mutableListOf<Int>()
    var clusterEnd = Int.MIN_VALUE

    fun close() {
        cluster.forEach { (id, column) -> result[id] = Slot(column, ends.size) }
        cluster.clear()
        ends.clear()
    }
    for (s in spans.sortedWith(compareBy({ it.start }, { it.end }))) {
        if (s.start >= clusterEnd) close()
        var column = ends.indexOfFirst { it <= s.start }
        if (column == -1) {
            ends += s.end
            column = ends.lastIndex
        } else {
            ends[column] = s.end
        }
        cluster += s.id to column
        clusterEnd = maxOf(clusterEnd, s.end)
    }
    close()
    return result
}

enum class Greeting { MORNING, AFTERNOON, EVENING }

fun greeting(hour: Int): Greeting =
    when (hour) {
        in 5..11 -> Greeting.MORNING
        in 12..17 -> Greeting.AFTERNOON
        else -> Greeting.EVENING
    }

/** The first class that has not started yet. */
fun <T> nextLesson(
    lessons: List<T>,
    now: String,
    startAt: (T) -> String,
): T? = lessons.sortedBy(startAt).firstOrNull { startAt(it) > now }
