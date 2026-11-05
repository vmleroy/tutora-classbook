package app.tutora.domain.theme

// Aparência's quick colors (spec 013, Theme.dc.html): named presets the teacher picks from,
// stored as data so screens never hold a color literal (rule 13).

/** A preset: its name key (translated by the app) and its color. */
data class Preset(
    val key: String,
    val hex: String,
)

val mainPresets =
    listOf(
        Preset("verde_quadro", "#2E5E4E"),
        Preset("azul", "#2F5C9E"),
        Preset("vinho", "#7A2E3B"),
        Preset("turquesa", "#2D7F86"),
        Preset("roxo", "#5B4A8B"),
        Preset("grafite", "#3A3F47"),
        Preset("terracota", "#A0522D"),
    )

val highlightPresets =
    listOf(
        Preset("amarelo_lapis", "#F2B33D"),
        Preset("laranja", "#F28C38"),
        Preset("coral", "#F07C6C"),
        Preset("menta", "#7BC6A4"),
        Preset("rosa", "#E58FB5"),
        Preset("azul_claro", "#9DB8F2"),
        Preset("dourado", "#D9A441"),
    )

/** What "Cor principal" paints, and "Cor de destaque" (Victor, 29 Oct 2026; AC-013-8). */
val quickMain = listOf(CustomPart.HEADER, CustomPart.PRIMARY_BTN, CustomPart.ACTIVE_TAB)
val quickHighlight = listOf(CustomPart.HIGHLIGHT_BTN)

/** Paints [parts] with [hex], or takes them back to the theme's colors with null ("Do tema"). */
fun applyQuick(
    custom: CustomColors,
    parts: List<CustomPart>,
    hex: String?,
): CustomColors = if (hex == null) custom - parts.toSet() else custom + parts.associateWith { hex.lowercase() }

/** The quick color now in use on [parts]: the same custom color on all of them, or null. */
fun quickValue(
    custom: CustomColors,
    parts: List<CustomPart>,
): String? = parts.map { custom[it] }.distinct().singleOrNull()

/** The theme's main and highlight colors in [scheme], for its card (AC-013-4). */
fun themeDots(
    theme: String,
    scheme: Scheme,
): Pair<String, String> {
    val p = resolvePalette(theme, scheme)
    return p.getValue("btn-primary") to p.getValue("accent")
}

/** A theme made of one of the four and fixed colors (Victor, 4 Nov 2026; AC-013-19). */
data class ExtraTheme(
    val base: String,
    val colors: CustomColors,
)

private fun painted(
    main: String,
    highlight: String,
    background: String,
): CustomColors =
    mapOf(
        CustomPart.HEADER to main,
        CustomPart.PRIMARY_BTN to main,
        CustomPart.ACTIVE_TAB to main,
        CustomPart.EARNINGS to main,
        CustomPart.HIGHLIGHT_BTN to highlight,
        CustomPart.BACKGROUND to background,
    )

val extraThemes: Map<String, ExtraTheme> =
    linkedMapOf(
        "floresta" to ExtraTheme("quadro", painted("#2f6b3f", "#e8b04a", "#f1f6f0")),
        "oceano" to ExtraTheme("caderno", painted("#1f5f7a", "#f2a65a", "#f2f6f9")),
        "lavanda" to ExtraTheme("giz", painted("#5b4a8b", "#e58fb5", "#f5f2fa")),
        "terracota" to ExtraTheme("biblioteca", painted("#a0522d", "#f2b33d", "#f8f1e8")),
    )

/** Every theme Tema offers: the four, then the four made of colors. */
val allThemeIds: List<String> = themeIds + extraThemes.keys

/** Cor de fundo's light tones (AC-013-20). */
val backgroundPresets =
    listOf(
        Preset("creme", "#FAF7F0"),
        Preset("branco", "#FFFFFF"),
        Preset("areia", "#F3EBDD"),
        Preset("cinza_claro", "#F1F2F4"),
        Preset("menta_clara", "#EEF6F1"),
        Preset("lavanda_clara", "#F2F0F8"),
    )

val quickBackground = listOf(CustomPart.BACKGROUND)
