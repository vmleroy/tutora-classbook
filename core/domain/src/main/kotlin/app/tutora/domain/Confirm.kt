package app.tutora.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

// Classes created ahead stay scheduled after they happen until the teacher confirms them
// (spec 014, AC-014-1, 2).

/** Ajustes › Aulas › Pedir confirmação: when a class that ended becomes "A confirmar" (AC-014-1). */
enum class ConfirmWhen(
    val key: String,
) {
    END("end"),
    HOUR("hour"),
    NEXT_DAY("next_day"),
    ;

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: END
    }
}

/** Ajustes › Aulas › Aulas passadas: confirm one by one, or mark done by themselves (AC-014-2). */
enum class PastClasses(
    val key: String,
) {
    ASK("ask"),
    AUTO("auto"),
    ;

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: ASK
    }
}

/** The moment a class starting at [startAt] and lasting [plannedMin] becomes "A confirmar". */
fun confirmFrom(
    startAt: String,
    plannedMin: Int,
    zone: String,
    whenTo: ConfirmWhen,
): Instant {
    val end = instantOf(startAt) + plannedMin.minutes
    return when (whenTo) {
        ConfirmWhen.END -> end
        ConfirmWhen.HOUR -> end + 1.hours
        ConfirmWhen.NEXT_DAY -> localToInstant(dateOf(end, zone).plus(DatePeriod(days = 1)), LocalTime(0, 0), zone)
    }
}

/**
 * Whether a class is waiting to be confirmed: still scheduled, not a forecast, and past the
 * moment chosen in Pedir confirmação. Done, cancelled, missed and live classes never are.
 */
fun isToConfirm(
    status: LessonStatus,
    forecast: Boolean,
    startAt: String,
    plannedMin: Int,
    now: Instant,
    zone: String,
    whenTo: ConfirmWhen,
): Boolean =
    status == LessonStatus.SCHEDULED &&
        !forecast &&
        confirmFrom(startAt, plannedMin, zone, whenTo) <= now

fun Lesson.toConfirm(
    now: Instant,
    zone: String,
    whenTo: ConfirmWhen,
): Boolean = isToConfirm(status, forecast, startAt, plannedMin, now, zone, whenTo)
