package com.vmleroy.tutora.domain

// Money is integer cents of reais (constitution rules 8 and 9).
// This is the only place in the app that rounds money.

/** An amount in centavos. */
typealias Cents = Long

/** Exact sum; an overflow fails loudly instead of wrapping. */
fun add(
    a: Cents,
    b: Cents,
): Cents = Math.addExact(a, b)

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
