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
    val set = colorSets.getValue("$theme-${scheme.name.lowercase()}")
    return set.getValue("primary") to set.getValue("accent")
}
