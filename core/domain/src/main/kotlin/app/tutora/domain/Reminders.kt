package app.tutora.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

// Reminders are made on the phone: one alarm for the next class, one weekly, one daily (spec 014).

/** Aviso antes da aula: Desligado (0) or minutes before (AC-014-12). */
val REMIND_BEFORE_CHOICES = listOf(0, 5, 15, 30, 60)
const val REMIND_BEFORE_DEFAULT = 15

data class ClassReminder(
    val lessonId: String,
    val at: Instant,
)

/**
 * The next class reminder still to come: the earliest scheduled class whose reminder time is
 * after [now]. Cancelled, done and forecast classes get none; off (0 minutes) gives none.
 */
fun nextClassReminder(
    lessons: List<Lesson>,
    now: Instant,
    minutes: Int,
): ClassReminder? {
    if (minutes <= 0) return null
    return lessons
        .filter { it.status == LessonStatus.SCHEDULED && !it.forecast }
        .map { ClassReminder(it.id, instantOf(it.startAt) - minutes.minutes) }
        .filter { it.at > now }
        .minByOrNull { it.at }
}

/** The next [day] at [hour]:[minute] strictly after [now], for "Quem te deve" (AC-014-13). */
fun nextWeekly(
    now: Instant,
    day: DayOfWeek,
    hour: Int,
    zone: String,
    minute: Int = 0,
): Instant {
    var date = dateOf(now, zone)
    while (true) {
        val at = localToInstant(date, LocalTime(hour, minute), zone)
        if (date.dayOfWeek == day && at > now) return at
        date = date.plus(DatePeriod(days = 1))
    }
}

/** The next [hour]:[minute] strictly after [now], for "Aulas para confirmar" (AC-014-14). */
fun nextDaily(
    now: Instant,
    hour: Int,
    zone: String,
    minute: Int = 0,
): Instant {
    val time = LocalTime(hour, minute)
    val today = localToInstant(dateOf(now, zone), time, zone)
    return if (today > now) today else localToInstant(dateOf(now, zone).plus(DatePeriod(days = 1)), time, zone)
}

/** The custom reminder's range: 5 minutes to a day (AC-014-36). */
const val REMIND_MIN = 5
const val REMIND_MAX = 24 * 60

data class OwedSummary(
    val count: Int,
    val cents: Cents,
)

/** "3 alunos te devem R$ 480,00": who owes and the total; null when no one owes (AC-014-13). */
fun owedSummary(balances: List<Balance>): OwedSummary? {
    val owes = balances.filterIsInstance<Balance.Owes>()
    return if (owes.isEmpty()) null else OwedSummary(owes.size, sum(owes.map { it.cents }))
}
