package app.tutora.domain.theme

import kotlin.math.abs
import kotlin.math.roundToInt

// Suas cores' Matiz, Intensidade and Brilho (spec 013), as hue, saturation and lightness. The
// hex code stays the source of truth; the sliders are read from it.

data class Hsl(
    /** 0..360 */
    val h: Int,
    /** 0..100 */
    val s: Int,
    /** 0..100 */
    val l: Int,
)

fun Rgb.toHsl(): Hsl {
    val mx = maxOf(r, g, b)
    val mn = minOf(r, g, b)
    val d = mx - mn
    val l = (mx + mn) / 2
    val s = if (d == 0.0) 0.0 else d / (1 - abs(2 * l - 1))
    var h =
        when {
            d == 0.0 -> 0.0
            mx == r -> ((g - b) / d).mod(6.0)
            mx == g -> (b - r) / d + 2
            else -> (r - g) / d + 4
        } * 60
    if (h < 0) h += 360
    return Hsl(h.roundToInt() % 360, (s * 100).roundToInt(), (l * 100).roundToInt())
}

fun Hsl.toHex(): String {
    val sf = s / 100.0
    val lf = l / 100.0
    val a = sf * minOf(lf, 1 - lf)

    fun f(n: Int): Double {
        val k = (n + h / 30.0) % 12
        return lf - a * maxOf(-1.0, minOf(k - 3, 9 - k, 1.0))
    }
    return Rgb(f(0), f(8), f(4)).toHex()
}

fun hslOf(hex: String): Hsl = parseHex(hex).toHsl()

/** A typed hex code ("#7A2E3B", "7a2e3b"), as "#7a2e3b"; null until it is a whole one (AC-013-12). */
fun readHex(text: String): String? {
    val t = text.trim().removePrefix("#")
    return if (t.length == 6 && t.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) "#${t.lowercase()}" else null
}

enum class ContrastLevel { GOOD, FAIR, POOR }

/** What Suas cores says under the sliders (AC-013-13). */
data class PartContrast(
    /** For a filled part (Topo and the buttons): true when the text on it is white. Null otherwise. */
    val whiteText: Boolean?,
    val ratio: Double,
    val level: ContrastLevel,
    /** In dark mode the color is shown lighter than chosen (AC-013-14). */
    val lightenedInDark: Boolean,
)

private val filled = setOf(CustomPart.HEADER, CustomPart.PRIMARY_BTN, CustomPart.HIGHLIGHT_BTN)

private val onToken =
    mapOf(
        CustomPart.HEADER to "on-header",
        CustomPart.PRIMARY_BTN to "on-btn-primary",
        CustomPart.HIGHLIGHT_BTN to "on-accent",
    )

fun partContrast(
    theme: String,
    scheme: Scheme,
    part: CustomPart,
    hex: String,
): PartContrast {
    val check = checkCustomColor(theme, scheme, part, hex)
    val white =
        if (part in filled) {
            val on = resolvePalette(theme, scheme, mapOf(part to hex)).getValue(onToken.getValue(part))
            relativeLuminance(on) > 0.5
        } else {
            null
        }
    val level =
        when {
            check.ratio >= MIN_TEXT_CONTRAST -> ContrastLevel.GOOD
            check.ratio >= 3.0 -> ContrastLevel.FAIR
            else -> ContrastLevel.POOR
        }
    val lifted = scheme == Scheme.DARK && part !in setOf(CustomPart.BACKGROUND, CustomPart.CARDS)
    return PartContrast(white, check.ratio, level, lifted)
}

/** The parts whose text would read below 4.5:1: leaving Suas cores asks first (AC-013-15). */
fun lowContrastParts(
    theme: String,
    scheme: Scheme,
    custom: CustomColors,
): List<CustomPart> = custom.filter { (part, hex) -> !checkCustomColor(theme, scheme, part, hex).ok }.keys.toList()
