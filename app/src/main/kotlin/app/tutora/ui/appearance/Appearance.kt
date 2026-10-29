package app.tutora.ui.appearance

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.ButtonVariant
import app.tutora.designsystem.components.ColorSlider
import app.tutora.designsystem.components.HeaderAction
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.IconButton
import app.tutora.designsystem.components.IconButtonVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.Money
import app.tutora.designsystem.components.MoneySize
import app.tutora.designsystem.components.MoneyTone
import app.tutora.designsystem.components.SectionHeader
import app.tutora.designsystem.components.SegmentedControl
import app.tutora.designsystem.components.Swatch
import app.tutora.designsystem.components.SwatchKind
import app.tutora.designsystem.components.TabBar
import app.tutora.designsystem.components.TabItem
import app.tutora.designsystem.components.TextField
import app.tutora.designsystem.components.ThemeCard
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.tokens.TutoraTokens.Radius
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.designsystem.tokens.hexColor
import app.tutora.domain.Greeting
import app.tutora.domain.Language
import app.tutora.domain.Settings
import app.tutora.domain.ThemeMode
import app.tutora.domain.greeting
import app.tutora.domain.localOf
import app.tutora.domain.theme.ContrastLevel
import app.tutora.domain.theme.CustomPart
import app.tutora.domain.theme.Hsl
import app.tutora.domain.theme.Preset
import app.tutora.domain.theme.Scheme
import app.tutora.domain.theme.highlightPresets
import app.tutora.domain.theme.hslOf
import app.tutora.domain.theme.lowContrastParts
import app.tutora.domain.theme.mainPresets
import app.tutora.domain.theme.partContrast
import app.tutora.domain.theme.quickHighlight
import app.tutora.domain.theme.quickMain
import app.tutora.domain.theme.quickValue
import app.tutora.domain.theme.readHex
import app.tutora.domain.theme.resolvePalette
import app.tutora.domain.theme.resolveScheme
import app.tutora.domain.theme.themeDots
import app.tutora.domain.theme.themeIds
import app.tutora.domain.theme.toHex
import app.tutora.domain.unsavedColors
import app.tutora.state.LocalServices
import app.tutora.ui.CustomColors
import app.tutora.ui.ScreenScaffold
import app.tutora.ui.longDate
import kotlinx.coroutines.launch
import java.text.NumberFormat

// Aparência and Suas cores (spec 013, Theme.dc.html and ColorPicker.dc.html).

@Composable
fun themeName(id: String): String =
    stringResource(
        when (id) {
            "caderno" -> R.string.ap_theme_caderno
            "biblioteca" -> R.string.ap_theme_biblioteca
            "giz" -> R.string.ap_theme_giz
            else -> R.string.ap_theme_quadro
        },
    )

@Composable
private fun presetName(p: Preset): String =
    stringResource(
        when (p.key) {
            "azul" -> R.string.ap_p_azul
            "vinho" -> R.string.ap_p_vinho
            "turquesa" -> R.string.ap_p_turquesa
            "roxo" -> R.string.ap_p_roxo
            "grafite" -> R.string.ap_p_grafite
            "terracota" -> R.string.ap_p_terracota
            "amarelo_lapis" -> R.string.ap_p_amarelo_lapis
            "laranja" -> R.string.ap_p_laranja
            "coral" -> R.string.ap_p_coral
            "menta" -> R.string.ap_p_menta
            "rosa" -> R.string.ap_p_rosa
            "azul_claro" -> R.string.ap_p_azul_claro
            "dourado" -> R.string.ap_p_dourado
            else -> R.string.ap_p_verde_quadro
        },
    )

@Composable
fun partName(p: CustomPart): String =
    stringResource(
        when (p) {
            CustomPart.HEADER -> R.string.cc_part_header
            CustomPart.PRIMARY_BTN -> R.string.cc_part_primaryBtn
            CustomPart.HIGHLIGHT_BTN -> R.string.cc_part_highlightBtn
            CustomPart.EARNINGS -> R.string.cc_part_earnings
            CustomPart.BACKGROUND -> R.string.cc_part_background
            CustomPart.CARDS -> R.string.cc_part_cards
            CustomPart.ACTIVE_TAB -> R.string.cc_part_activeTab
        },
    )

@Composable
private fun modeName(m: ThemeMode) =
    stringResource(
        when (m) {
            ThemeMode.LIGHT -> R.string.ap_light
            ThemeMode.DARK -> R.string.ap_dark
            ThemeMode.SYSTEM -> R.string.ap_system
        },
    )

@Composable
private fun scheme(s: Settings): Scheme = resolveScheme(s.themeMode, isSystemInDarkTheme())

/** Ajustes › Geral › Aparência: the theme, the mode, and whether colors or text are changed (AC-013-1). */
@Composable
fun AppearanceSettingsRow(onClick: () -> Unit) {
    val services = LocalServices.current
    val settings by services.settings.collectAsState(initial = null)
    val s = settings ?: return
    val name = s.savedThemes.firstOrNull { it.id == s.savedThemeId }?.name ?: themeName(s.themeId)
    val extras =
        listOfNotNull(
            stringResource(R.string.ap_rowColors).takeIf { s.customColors.isNotEmpty() },
            stringResource(R.string.ap_rowText).takeIf { s.textScale == "large" },
        )
    val joiner = if (currentLanguage() == Language.PT_BR) " e " else " and "
    ListRow(
        stringResource(R.string.ap_screen),
        Modifier.testTag("appearance-row"),
        subtitle = listOfNotNull(name, modeName(s.themeMode), extras.joinToString(joiner).ifEmpty { null }).joinToString(" · "),
        icon = IconName.STAR,
        onClick = onClick,
    )
}

/**
 * A small Hoje painted with [s] (AC-013-2): the Topo band with the date and greeting, the earnings
 * card and Iniciar aula; [full] adds Salvar aula and the tab bar, as Suas cores shows (AC-013-11).
 */
@Composable
fun AppearancePreview(
    s: Settings,
    full: Boolean,
) {
    val services = LocalServices.current
    val language = currentLanguage()
    val now = remember { localOf(services.now(), services.zone) }
    val hello =
        stringResource(
            when (greeting(now.hour)) {
                Greeting.MORNING -> R.string.home_greeting_morning
                Greeting.AFTERNOON -> R.string.home_greeting_afternoon
                Greeting.EVENING -> R.string.home_greeting_evening
            },
        )
    TutoraTheme(themeId = s.themeId, mode = s.themeMode, custom = s.customColors, largeText = s.textScale == "large") {
        val c = TutoraTheme.colors
        val shape = RoundedCornerShape(Radius.x2xl)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(c.background)
                // The tag carries what it is painted with, so a test can see it follow a choice.
                .testTag("appearance-preview-${s.themeId}-${s.customColors.size}"),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(c.header)
                    .padding(Spacing.s4),
            ) {
                Text(longDate(now.date, language), style = TutoraTheme.type.meta, color = c.onHeaderMuted)
                Text(
                    listOfNotNull(hello, s.teacherName.ifBlank { null }).joinToString(", "),
                    style = TutoraTheme.type.titleMd,
                    color = c.onHeader,
                )
            }
            Column(Modifier.padding(Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.xl))
                        .background(c.surface)
                        .padding(Spacing.s4),
                ) {
                    Text(stringResource(R.string.earn_today), style = TutoraTheme.type.meta, color = c.inkMuted)
                    Money(24_000, size = MoneySize.LG, tone = MoneyTone.EARNINGS)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                    if (full) Button(stringResource(R.string.ap_previewLesson), {}, Modifier.weight(1f))
                    Button(
                        stringResource(R.string.live_start),
                        {},
                        Modifier.weight(1f),
                        variant = ButtonVariant.ACCENT,
                        icon = IconName.PLAY,
                    )
                }
            }
            if (full) {
                TabBar(
                    listOf(
                        TabItem("today", stringResource(R.string.tab_today), IconName.HOME),
                        TabItem("schedule", stringResource(R.string.tab_schedule), IconName.CALENDAR),
                        TabItem("registry", stringResource(R.string.tab_registry), IconName.USERS),
                        TabItem("earnings", stringResource(R.string.tab_earnings), IconName.WALLET),
                    ),
                    "today",
                    {},
                )
            }
        }
    }
}

private sealed interface Pending {
    data class Builtin(
        val id: String,
    ) : Pending

    data class Saved(
        val id: String,
    ) : Pending
}

@Composable
fun AppearanceScreen(nav: NavHostController) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val settings by services.settings.collectAsState(initial = null)
    var pending by remember { mutableStateOf<Pending?>(null) }
    var naming by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    val defaultName = stringResource(R.string.ap_defaultName)
    val s = settings ?: return
    val scheme = scheme(s)
    val a = services.appearance

    fun go(p: Pending) =
        scope.launch {
            when (p) {
                is Pending.Builtin -> a.chooseTheme(p.id)
                is Pending.Saved -> a.chooseSaved(p.id)
            }
        }

    // Leaving unsaved colors asks first (AC-013-6).
    fun choose(p: Pending) {
        val same = (p is Pending.Builtin && p.id == s.themeId && s.customColors.isEmpty()) || (p is Pending.Saved && p.id == s.savedThemeId)
        if (same) return
        if (s.unsavedColors()) pending = p else go(p)
    }

    fun freeName(): String {
        val names = s.savedThemes.map { it.name }.toSet()
        return generateSequence(1) { it + 1 }.map { if (it == 1) defaultName else "$defaultName $it" }.first { it !in names }
    }

    pending?.let { p ->
        BottomSheet(
            stringResource(R.string.ap_switchTitle),
            { pending = null },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                Button(stringResource(R.string.ap_saveAndSwitch), {
                    pending = null
                    scope.launch {
                        a.saveAsTheme(freeName())
                        go(p)
                    }
                }, fullWidth = true)
                Button(stringResource(R.string.ap_switchAnyway), {
                    pending = null
                    go(p)
                }, fullWidth = true, variant = ButtonVariant.OUTLINE)
                Button(stringResource(R.string.catalog_cancel), { pending = null }, fullWidth = true, variant = ButtonVariant.GHOST)
            }
        }
    }
    naming?.let { text ->
        BottomSheet(
            stringResource(R.string.ap_nameTitle),
            { naming = null },
            actions =
                listOf(
                    { m -> Button(stringResource(R.string.catalog_cancel), { naming = null }, m, variant = ButtonVariant.OUTLINE) },
                    { m ->
                        Button(stringResource(R.string.ap_save), {
                            val id = editing
                            naming = null
                            editing = null
                            scope.launch { if (id == null) a.saveAsTheme(text) else a.renameSaved(id, text) }
                        }, m)
                    },
                ),
        ) { TextField(stringResource(R.string.ap_nameTitle), text, { naming = it }) }
    }
    deleting?.let { id ->
        val t = s.savedThemes.firstOrNull { it.id == id }
        BottomSheet(
            stringResource(R.string.ap_deleteTitle, t?.name.orEmpty()),
            { deleting = null },
            actions =
                listOf(
                    { m -> Button(stringResource(R.string.catalog_cancel), { deleting = null }, m, variant = ButtonVariant.OUTLINE) },
                    { m ->
                        Button(stringResource(R.string.ap_delete), {
                            deleting = null
                            scope.launch { a.deleteSaved(id) }
                        }, m, variant = ButtonVariant.DANGER)
                    },
                ),
        )
    }

    ScreenScaffold(stringResource(R.string.ap_screen), "appearance-screen", variant = HeaderVariant.PUSH, onBack = { nav.popBackStack() }) {
        AppearancePreview(s, full = false)
        // Modo (AC-013-3).
        SectionHeader(stringResource(R.string.ap_mode))
        SegmentedControl(
            listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM).map { it to modeName(it) },
            s.themeMode,
            { m -> scope.launch { a.setMode(m) } },
            stringResource(R.string.ap_mode),
        )
        // Tema: the four, then the teacher's own (AC-013-4, 5).
        SectionHeader(stringResource(R.string.ap_theme))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            themeIds.forEach { id ->
                val (main, hi) = themeDots(id, scheme)
                ThemeCard(
                    themeName(id),
                    hexColor(main),
                    hexColor(hi),
                    selected = id == s.themeId && s.savedThemeId == null && s.customColors.isEmpty(),
                    onClick = { choose(Pending.Builtin(id)) },
                    modifier = Modifier.testTag("theme-$id"),
                )
            }
            s.savedThemes.forEach { t ->
                val palette = resolvePalette(t.themeId, scheme, t.custom)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    ThemeCard(
                        t.name,
                        hexColor(palette.getValue("btn-primary")),
                        hexColor(palette.getValue("accent")),
                        selected = t.id == s.savedThemeId,
                        onClick = { choose(Pending.Saved(t.id)) },
                        modifier = Modifier.testTag("saved-${t.id}"),
                    )
                    IconButton(IconName.PENCIL, stringResource(R.string.ap_rename) + " " + t.name, {
                        editing = t.id
                        naming = t.name
                    }, variant = IconButtonVariant.PLAIN)
                    IconButton(
                        IconName.TRASH,
                        stringResource(R.string.ap_delete) + " " + t.name,
                        { deleting = t.id },
                        variant = IconButtonVariant.PLAIN,
                    )
                }
            }
        }
        if (s.unsavedColors()) {
            Button(stringResource(R.string.ap_saveTheme), {
                editing = null
                naming = freeName()
            }, fullWidth = true, variant = ButtonVariant.OUTLINE, icon = IconName.STAR)
        }
        // Personalizar: Cor principal and Cor de destaque (AC-013-7, 8, 9).
        SectionHeader(stringResource(R.string.ap_customize))
        listOf(
            Triple(R.string.ap_main, quickMain, mainPresets),
            Triple(R.string.ap_highlight, quickHighlight, highlightPresets),
        ).forEach { (label, parts, presets) ->
            Text(stringResource(label), style = TutoraTheme.type.bodyStrong, color = TutoraTheme.colors.ink)
            val current = quickValue(s.customColors, parts)
            val themeColor = resolvePalette(s.themeId, scheme).getValue(if (parts == quickMain) "btn-primary" else "accent")
            FlowRow {
                Swatch(
                    hexColor(themeColor),
                    stringResource(R.string.ap_fromTheme),
                    selected = parts.none { it in s.customColors },
                    onClick = { scope.launch { a.setQuick(parts, null) } },
                    kind = SwatchKind.THEME,
                )
                presets.forEach { p ->
                    Swatch(hexColor(p.hex), presetName(p), selected = current == p.hex.lowercase(), onClick = {
                        scope.launch { a.setQuick(parts, p.hex) }
                    })
                }
                Swatch(
                    TutoraTheme.colors.surfaceSunken,
                    stringResource(R.string.ap_more),
                    selected = false,
                    onClick = { nav.navigate(CustomColors(parts.first().key)) },
                    kind = SwatchKind.ADD,
                )
            }
        }
        if (s.customColors.isNotEmpty()) {
            Button(
                stringResource(R.string.ap_restoreTheme),
                { scope.launch { a.restore() } },
                fullWidth = true,
                variant = ButtonVariant.GHOST,
            )
        }
        // Tamanho do texto (AC-013-10).
        SectionHeader(stringResource(R.string.ap_textSize))
        SegmentedControl(
            listOf(false to stringResource(R.string.ap_normal), true to stringResource(R.string.ap_large)),
            s.textScale == "large",
            { large -> scope.launch { a.setTextSize(large) } },
            stringResource(R.string.ap_textSize),
        )
        ListGroup(
            listOf {
                ListRow(
                    stringResource(R.string.ap_advanced),
                    Modifier.testTag("advanced-colors"),
                    subtitle = stringResource(R.string.ap_advancedSub),
                    icon = IconName.PENCIL,
                    onClick = { nav.navigate(CustomColors(CustomPart.HIGHLIGHT_BTN.key)) },
                )
            },
        )
    }
}

/** "7,2" / "7.2", the contrast ratio in the app language (AC-013-18). */
private fun ratioText(
    r: Double,
    language: Language,
): String =
    NumberFormat
        .getNumberInstance(app.tutora.ui.localeOf(language))
        .apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }.format(r)

@Composable
fun CustomColorsScreen(
    nav: NavHostController,
    startPart: String,
) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val language = currentLanguage()
    val settings by services.settings.collectAsState(initial = null)
    var partKey by rememberSaveable { mutableStateOf(startPart) }
    var draft by rememberSaveable { mutableStateOf<String?>(null) }
    var leaving by rememberSaveable { mutableStateOf(false) }
    val s = settings ?: return
    val scheme = scheme(s)
    val a = services.appearance
    val part = CustomPart.fromKey(partKey) ?: CustomPart.HIGHLIGHT_BTN
    val themePalette = resolvePalette(s.themeId, scheme)
    val baseToken =
        mapOf(
            CustomPart.HEADER to "header",
            CustomPart.PRIMARY_BTN to "btn-primary",
            CustomPart.HIGHLIGHT_BTN to "accent",
            CustomPart.EARNINGS to "earnings",
            CustomPart.BACKGROUND to "background",
            CustomPart.CARDS to "surface",
            CustomPart.ACTIVE_TAB to "tab-active",
        )
    // In light mode the theme's own color; in dark the light one, which is what the teacher chooses from.
    val lightPalette = resolvePalette(s.themeId, Scheme.LIGHT)

    fun colorOf(p: CustomPart) = s.customColors[p] ?: lightPalette.getValue(baseToken.getValue(p))
    val hex = colorOf(part)
    val hsl = hslOf(hex)
    LaunchedEffect(partKey) { draft = null }

    fun set(next: String) = scope.launch { a.setPart(part, next) }

    // Leaving with text below 4.5:1 asks first (AC-013-15).
    fun leave() {
        if (lowContrastParts(s.themeId, scheme, s.customColors).isNotEmpty()) leaving = true else nav.popBackStack()
    }
    BackHandler { leave() }

    if (leaving) {
        BottomSheet(
            stringResource(R.string.cc_leaveTitle),
            { leaving = false },
            actions =
                listOf(
                    { m -> Button(stringResource(R.string.cc_adjust), { leaving = false }, m, variant = ButtonVariant.OUTLINE) },
                    { m ->
                        Button(stringResource(R.string.ap_save), {
                            leaving = false
                            nav.popBackStack()
                        }, m)
                    },
                ),
        )
    }

    ScreenScaffold(
        stringResource(R.string.cc_screen),
        "custom-colors-screen",
        variant = HeaderVariant.PUSH,
        onBack = ::leave,
        actions = listOf(HeaderAction(IconName.CLOSE, stringResource(R.string.cc_restore)) { scope.launch { a.restore() } }),
    ) {
        AppearancePreview(s, full = true)
        // Escolha o que pintar (AC-013-11).
        SectionHeader(stringResource(R.string.cc_pick))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            CustomPart.entries.forEach { p ->
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Swatch(hexColor(colorOf(p)), partName(p), selected = p == part, onClick = { partKey = p.key })
                    Text(partName(p), style = TutoraTheme.type.caption, color = TutoraTheme.colors.inkMuted)
                }
            }
        }
        // Hex, Matiz, Intensidade, Brilho (AC-013-12).
        TextField(
            stringResource(R.string.cc_hex, partName(part)),
            draft ?: hex.uppercase(),
            { typed ->
                draft = typed
                readHex(typed)?.let { set(it) }
            },
            Modifier.testTag("hex-field"),
        )
        ColorSlider(
            stringResource(R.string.cc_hue),
            hsl.h,
            {
                draft = null
                set(hsl.copy(h = it).toHex())
            },
            (0..6).map { hexColor(Hsl(it * 60 % 360, hsl.s.coerceAtLeast(40), 50).toHex()) },
            range = 0..359,
        )
        ColorSlider(
            stringResource(R.string.cc_intensity),
            hsl.s,
            {
                draft = null
                set(hsl.copy(s = it).toHex())
            },
            listOf(hexColor(hsl.copy(s = 0).toHex()), hexColor(hsl.copy(s = 100).toHex())),
        )
        ColorSlider(
            stringResource(R.string.cc_brightness),
            hsl.l,
            {
                draft = null
                set(hsl.copy(l = it).toHex())
            },
            listOf(hexColor(hsl.copy(l = 0).toHex()), hexColor(hsl.copy(l = 50).toHex()), hexColor(hsl.copy(l = 100).toHex())),
        )
        // How well text reads on it (AC-013-13, 14).
        val check = partContrast(s.themeId, scheme, part, hex)
        val level =
            stringResource(
                when (check.level) {
                    ContrastLevel.GOOD -> R.string.cc_good
                    ContrastLevel.FAIR -> R.string.cc_fair
                    ContrastLevel.POOR -> R.string.cc_poor
                },
            )
        val ratio = ratioText(check.ratio, language)
        val line =
            check.whiteText?.let { white ->
                stringResource(
                    R.string.cc_contrastFill,
                    stringResource(if (white) R.string.cc_white else R.string.cc_darkText),
                    ratio,
                    level,
                )
            } ?: stringResource(R.string.cc_contrast, ratio, level)
        val c = TutoraTheme.colors
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Box(
                Modifier
                    .padding(Spacing.s0_5)
                    .clip(RoundedCornerShape(Radius.full))
                    .background(
                        when (check.level) {
                            ContrastLevel.GOOD -> c.success
                            ContrastLevel.FAIR -> c.accent
                            ContrastLevel.POOR -> c.danger
                        },
                    ).padding(Spacing.s1_5),
            )
            Text(
                line,
                style = TutoraTheme.type.meta,
                color =
                    if (check.level ==
                        ContrastLevel.POOR
                    ) {
                        c.dangerInk
                    } else {
                        c.ink
                    },
                modifier = Modifier.testTag("contrast-line"),
            )
        }
        if (check.lightenedInDark) Text(stringResource(R.string.cc_darkNote), style = TutoraTheme.type.meta, color = c.inkMuted)
        Button(stringResource(R.string.cc_restore), { scope.launch { a.restore() } }, fullWidth = true, variant = ButtonVariant.GHOST)
    }
}
