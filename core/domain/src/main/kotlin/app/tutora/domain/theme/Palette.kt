package app.tutora.domain.theme

import app.tutora.domain.ThemeMode

// Turns the teacher's choices into one flat palette (docs/design-system/Theming.md).

val themeIds = listOf("quadro", "caderno", "biblioteca", "giz")

enum class Scheme { LIGHT, DARK }

/** The seven parts of Suas cores, with their stored keys. */
enum class CustomPart(
    val key: String,
) {
    HEADER("header"),
    PRIMARY_BTN("primaryBtn"),
    HIGHLIGHT_BTN("highlightBtn"),
    EARNINGS("earnings"),
    BACKGROUND("background"),
    CARDS("cards"),
    ACTIVE_TAB("activeTab"),
    ;

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key }
    }
}

typealias CustomColors = Map<CustomPart, String>

/** Tokens for repaintable parts that components read instead of primary. */
val partTokens = listOf("btn-primary", "on-btn-primary", "tab-active", "tab-active-soft")

/** Every palette token: the color tokens plus the part tokens. */
typealias Palette = Map<String, String>

/** Which tokens each part may change; everything else stays the theme's (AC-002-6). */
val tokensOfPart: Map<CustomPart, List<String>> =
    mapOf(
        CustomPart.HEADER to listOf("header", "on-header", "on-header-muted", "earnings-on-header"),
        CustomPart.PRIMARY_BTN to listOf("btn-primary", "on-btn-primary"),
        CustomPart.HIGHLIGHT_BTN to listOf("accent", "accent-soft", "on-accent", "accent-ink", "earnings-on-header"),
        CustomPart.EARNINGS to listOf("earnings"),
        CustomPart.BACKGROUND to listOf("background"),
        CustomPart.CARDS to listOf("surface", "surface-sunken"),
        CustomPart.ACTIVE_TAB to listOf("tab-active", "tab-active-soft"),
    )

private const val DARK_LIFT = 0.4 // custom colors are lightened about 40% in OKLCH in dark mode
private const val HEADER_DEPTH = 0.68 // dark header = the color mixed 68% toward the background
private const val DARK_GROUND_CHROMA = 0.04 // a custom background or cards keeps only a tint in dark mode
const val MIN_TEXT_CONTRAST = 4.5

/** The chosen mode, or the phone's when the mode is "system" (light if the phone says nothing). */
fun resolveScheme(
    mode: ThemeMode,
    systemDark: Boolean?,
): Scheme =
    when (mode) {
        ThemeMode.LIGHT -> Scheme.LIGHT
        ThemeMode.DARK -> Scheme.DARK
        ThemeMode.SYSTEM -> if (systemDark == true) Scheme.DARK else Scheme.LIGHT
    }

private fun best(
    ground: String,
    candidates: List<String>,
) = candidates.maxBy { contrastRatio(it, ground) }

fun resolvePalette(
    theme: String,
    scheme: Scheme,
    custom: CustomColors = emptyMap(),
): Palette {
    val base = colorSets.getValue("$theme-${scheme.name.lowercase()}")
    val p = base.toMutableMap()
    p["btn-primary"] = base.getValue("primary")
    p["on-btn-primary"] = base.getValue("on-primary")
    p["tab-active"] = base.getValue("primary")
    p["tab-active-soft"] = base.getValue("primary-soft")
    val dark = scheme == Scheme.DARK

    // White or ink text, whichever reads better (dark mode: ink is light) (AC-002-7).
    fun textOn(ground: String) =
        best(ground, if (dark) listOf(base.getValue("ink"), base.getValue("background")) else listOf("#ffffff", base.getValue("ink")))

    fun lift(hex: String) = if (dark) lighten(hex, DARK_LIFT) else hex

    custom[CustomPart.BACKGROUND]?.let {
        p["background"] = if (dark) atLightnessOf(it, base.getValue("background"), DARK_GROUND_CHROMA) else it
    }
    custom[CustomPart.CARDS]?.let {
        val surface = if (dark) atLightnessOf(it, base.getValue("surface"), DARK_GROUND_CHROMA) else it
        p["surface"] = surface
        // Sunken sits one step darker than surface in light mode and one step lighter in dark.
        p["surface-sunken"] =
            shiftLightness(surface, oklchLightness(base.getValue("surface-sunken")) - oklchLightness(base.getValue("surface")))
    }
    custom[CustomPart.PRIMARY_BTN]?.let {
        p["btn-primary"] = lift(it)
        p["on-btn-primary"] = textOn(p.getValue("btn-primary"))
    }
    custom[CustomPart.ACTIVE_TAB]?.let {
        p["tab-active"] = lift(it)
        p["tab-active-soft"] = mix(p.getValue("tab-active"), p.getValue("surface"), if (dark) 0.75 else 0.85)
    }
    custom[CustomPart.EARNINGS]?.let { p["earnings"] = lift(it) }
    custom[CustomPart.HIGHLIGHT_BTN]?.let {
        p["accent"] = lift(it)
        p["on-accent"] = textOn(p.getValue("accent"))
        p["accent-soft"] = mix(p.getValue("accent"), base.getValue("surface"), if (dark) 0.78 else 0.72)
        p["accent-ink"] = best(p.getValue("accent-soft"), listOf(base.getValue("accent-ink"), base.getValue("ink")))
    }
    custom[CustomPart.HEADER]?.let {
        p["header"] = if (dark) mix(lift(it), base.getValue("background"), HEADER_DEPTH) else it
        p["on-header"] = textOn(p.getValue("header"))
        p["on-header-muted"] = mix(p.getValue("on-header"), p.getValue("header"), 0.2)
    }
    if (CustomPart.HEADER in custom || CustomPart.HIGHLIGHT_BTN in custom) {
        // Money on the header: accent when it holds 3:1 there (large text only), else on-header.
        p["earnings-on-header"] =
            if (contrastRatio(p.getValue("accent"), p.getValue("header")) >= 3) p.getValue("accent") else p.getValue("on-header")
    }
    return p
}

/** Text pairs each part puts at risk, checked when the teacher picks a color (AC-002-8). */
private val pairsOfPart: Map<CustomPart, List<Pair<String, String>>> =
    mapOf(
        CustomPart.HEADER to listOf("on-header" to "header"),
        CustomPart.PRIMARY_BTN to listOf("on-btn-primary" to "btn-primary"),
        CustomPart.HIGHLIGHT_BTN to listOf("on-accent" to "accent", "accent-ink" to "accent-soft"),
        CustomPart.EARNINGS to listOf("earnings" to "background", "earnings" to "surface"),
        CustomPart.BACKGROUND to listOf("ink" to "background", "ink-muted" to "background"),
        CustomPart.CARDS to listOf("ink" to "surface", "ink-muted" to "surface", "ink" to "surface-sunken"),
        CustomPart.ACTIVE_TAB to listOf("tab-active" to "surface"),
    )

data class ColorCheck(
    val ok: Boolean,
    val ratio: Double,
)

/** The lowest text contrast a custom color leaves, and whether it reaches 4.5:1. */
fun checkCustomColor(
    theme: String,
    scheme: Scheme,
    part: CustomPart,
    hex: String,
): ColorCheck {
    val p = resolvePalette(theme, scheme, mapOf(part to hex))
    val ratio = pairsOfPart.getValue(part).minOf { (text, ground) -> contrastRatio(p.getValue(text), p.getValue(ground)) }
    return ColorCheck(ratio >= MIN_TEXT_CONTRAST, ratio)
}
