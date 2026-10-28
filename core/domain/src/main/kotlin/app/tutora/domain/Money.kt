package app.tutora.domain

// Money is integer cents of reais (constitution rules 8 and 9).
// This is the only place in the app that rounds money.

/** An amount in centavos. */
typealias Cents = Long

/** Largest amount a money field accepts: R$ 99.999,99. */
const val MAX_FIELD_CENTS: Cents = 9_999_999

/** Exact sum; an overflow fails loudly instead of wrapping. */
fun add(
    a: Cents,
    b: Cents,
): Cents = Math.addExact(a, b)

/** Exact sum of stored amounts; never rounds, so totals never drift (AC-003-4). */
fun sum(amounts: Iterable<Cents>): Cents = amounts.fold(0L) { total, a -> add(total, a) }

/** `numerator / denominator` rounded half away from zero, computed exactly. */
fun roundHalfAwayFromZero(
    numerator: Long,
    denominator: Long,
): Long {
    require(denominator > 0) { "roundHalfAwayFromZero needs a positive denominator" }
    val abs = Math.absExact(numerator)
    // floor(abs / d + 1/2) without leaving integers: (2·abs + d) / (2·d)
    val q = Math.addExact(Math.multiplyExact(2L, abs), denominator) / Math.multiplyExact(2L, denominator)
    return if (numerator < 0) -q else q
}

/** Multiplies by the ratio `numerator / denominator` and rounds once. */
fun multiplyRate(
    amount: Cents,
    numerator: Long,
    denominator: Long,
): Cents = roundHalfAwayFromZero(Math.multiplyExact(amount, numerator), denominator)

enum class RateType { HOUR, LESSON }

/** What one lesson is worth: hourly prices scale by minutes ÷ 60, per-lesson prices do not. */
fun lessonValue(
    price: Cents,
    rateType: RateType,
    minutes: Int,
): Cents = if (rateType == RateType.HOUR) multiplyRate(price, minutes.toLong(), 60) else price

/** What the teacher keeps after a commission in basis points (30% = 3000). */
fun afterCommission(
    amount: Cents,
    commissionBp: Int,
): Cents = multiplyRate(amount, (10_000 - commissionBp).toLong(), 10_000)

/** Whole reais for compact summaries, rounded half away from zero. */
fun wholeReais(amount: Cents): Long = roundHalfAwayFromZero(amount, 100)

/**
 * The amount a money field holds after the teacher edits its text. The digits of the
 * formatted text are the cents, so typing a digit appends it on the right and deleting
 * drops the last one. Anything past [max] keeps the previous amount.
 */
fun typedCents(
    text: String,
    previous: Cents,
    max: Cents = MAX_FIELD_CENTS,
): Cents {
    val digits = text.filter { it.isDigit() }.trimStart('0')
    if (digits.length > max.toString().length) return previous
    val value = if (digits.isEmpty()) 0L else digits.toLong()
    return if (value > max) previous else value
}
