package com.vmleroy.tutora.domain

// Durations are whole minutes, typed as hh:mm and filling from the right like money.

/** Longest duration a field accepts: 5 hours, the stepper's own limit. */
const val MAX_DURATION_MIN = 300

data class TypedDuration(
    val digits: String,
    val minutes: Int?,
)

/**
 * The digits of an hh:mm field after the teacher edits its text: typing appends on the
 * right, deleting drops the last one, at most four digits. `minutes` is null while the
 * minutes part is over 59 or the total is over [max].
 */
fun typedDuration(
    text: String,
    previous: String,
    max: Int = MAX_DURATION_MIN,
): TypedDuration {
    val digits = text.filter { it.isDigit() }.trimStart('0')
    if (digits.length > 4) return TypedDuration(previous, minutesOf(previous, max))
    return TypedDuration(digits, minutesOf(digits, max))
}

private fun minutesOf(
    digits: String,
    max: Int,
): Int? {
    val value = if (digits.isEmpty()) 0 else digits.toInt()
    val minutes = value % 100
    if (minutes > 59) return null
    val total = value / 100 * 60 + minutes
    return if (total > max) null else total
}

/** The digits that show [minutes] as hh:mm (90 → "130"). */
fun durationDigits(minutes: Int): String {
    val value = minutes / 60 * 100 + minutes % 60
    return if (value == 0) "" else value.toString()
}

/** "01:30" from the digits "130". */
fun hhmm(digits: String): String {
    val padded = digits.padStart(4, '0')
    return "${padded.dropLast(2)}:${padded.takeLast(2)}"
}
