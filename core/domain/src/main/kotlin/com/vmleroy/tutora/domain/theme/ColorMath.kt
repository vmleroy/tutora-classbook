package com.vmleroy.tutora.domain.theme

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// sRGB, OKLab and OKLCH (Björn Ottosson, https://bottosson.github.io/posts/oklab/), the
// conversions the first version took from culori.

/** sRGB channels in 0..1 (gamma-encoded). */
data class Rgb(
    val r: Double,
    val g: Double,
    val b: Double,
)

data class Oklch(
    val l: Double,
    val c: Double,
    val h: Double,
)

fun parseHex(hex: String): Rgb {
    val h = hex.removePrefix("#")
    require(h.length == 6 || h.length == 8) { "Not a #rrggbb color: $hex" }
    val n = h.substring(0, 6).toInt(16)
    return Rgb(((n shr 16) and 255) / 255.0, ((n shr 8) and 255) / 255.0, (n and 255) / 255.0)
}

fun Rgb.toHex(): String {
    fun ch(v: Double) = (v.coerceIn(0.0, 1.0) * 255).roundToInt().toString(16).padStart(2, '0')
    return "#${ch(r)}${ch(g)}${ch(b)}"
}

private fun toLinear(c: Double) = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

private fun toGamma(c: Double) =
    if (abs(c) <=
        0.0031308
    ) {
        c * 12.92
    } else {
        1.055 * abs(c).pow(1 / 2.4) * kotlin.math.sign(c) - 0.055 * kotlin.math.sign(c)
    }

fun Rgb.toOklch(): Oklch {
    val lr = toLinear(r)
    val lg = toLinear(g)
    val lb = toLinear(b)
    val l = cbrt(0.4122214708 * lr + 0.5363325363 * lg + 0.0514459929 * lb)
    val m = cbrt(0.2119034982 * lr + 0.6806995451 * lg + 0.1073969566 * lb)
    val s = cbrt(0.0883024619 * lr + 0.2817188376 * lg + 0.6299787005 * lb)
    val okL = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
    val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    val bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    val h = Math.toDegrees(atan2(bb, a)).let { if (it < 0) it + 360 else it }
    return Oklch(okL, sqrt(a * a + bb * bb), h)
}

/** OKLCH to sRGB without clamping: channels may fall outside 0..1. */
fun Oklch.toRgbUnclamped(): Rgb {
    val a = c * cos(Math.toRadians(h))
    val b = c * sin(Math.toRadians(h))
    val l1 = (l + 0.3963377774 * a + 0.2158037573 * b).pow(3)
    val m1 = (l - 0.1055613458 * a - 0.0638541728 * b).pow(3)
    val s1 = (l - 0.0894841775 * a - 1.2914855480 * b).pow(3)
    return Rgb(
        toGamma(4.0767416621 * l1 - 3.3077115913 * m1 + 0.2309699292 * s1),
        toGamma(-1.2684380046 * l1 + 2.6097574011 * m1 - 0.3413193965 * s1),
        toGamma(-0.0041960863 * l1 - 0.7034186147 * m1 + 1.7076147010 * s1),
    )
}

private fun Rgb.inGamut(eps: Double = 1e-6) = listOf(r, g, b).all { it in -eps..1 + eps }

/** Reduces chroma until the color fits sRGB, keeping lightness and hue (culori's clampChroma). */
fun Oklch.clampChroma(): Rgb {
    if (toRgbUnclamped().inGamut()) return toRgbUnclamped()
    var lo = 0.0
    var hi = c
    repeat(40) {
        val mid = (lo + hi) / 2
        if (copy(c = mid).toRgbUnclamped().inGamut()) lo = mid else hi = mid
    }
    return copy(c = lo).toRgbUnclamped()
}

/** a → b by [t] per sRGB channel (0 = a, 1 = b), the way the theme tokens were derived. */
fun mix(
    a: String,
    b: String,
    t: Double,
): String {
    val ca = parseHex(a)
    val cb = parseHex(b)
    return Rgb(ca.r + (cb.r - ca.r) * t, ca.g + (cb.g - ca.g) * t, ca.b + (cb.b - ca.b) * t).toHex()
}

fun oklchLightness(hex: String): Double = parseHex(hex).toOklch().l

/** Moves OKLCH lightness [amount] of the way toward white, keeping hue and chroma. */
fun lighten(
    hex: String,
    amount: Double,
): String {
    val c = parseHex(hex).toOklch()
    return c.copy(l = c.l + (1 - c.l) * amount).clampChroma().toHex()
}

/** Same hue, with the lightness of [reference] and at most [maxChroma] chroma. */
fun atLightnessOf(
    hex: String,
    reference: String,
    maxChroma: Double,
): String {
    val c = parseHex(hex).toOklch()
    val r = parseHex(reference).toOklch()
    return c.copy(l = r.l, c = minOf(c.c, maxChroma)).clampChroma().toHex()
}

/** Shifts OKLCH lightness by [delta] (negative = darker). */
fun shiftLightness(
    hex: String,
    delta: Double,
): String {
    val c = parseHex(hex).toOklch()
    return c.copy(l = (c.l + delta).coerceIn(0.0, 1.0)).clampChroma().toHex()
}

// WCAG 2 contrast (https://www.w3.org/TR/WCAG21/#dfn-contrast-ratio).

fun relativeLuminance(hex: String): Double {
    val c = parseHex(hex)
    return 0.2126 * toLinear(c.r) + 0.7152 * toLinear(c.g) + 0.0722 * toLinear(c.b)
}

fun contrastRatio(
    a: String,
    b: String,
): Double {
    val (hi, lo) = listOf(relativeLuminance(a), relativeLuminance(b)).sortedDescending()
    return (hi + 0.05) / (lo + 0.05)
}
