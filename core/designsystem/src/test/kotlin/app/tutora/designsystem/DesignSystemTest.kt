package app.tutora.designsystem

import androidx.compose.animation.core.SnapSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.components.readableWidth
import app.tutora.designsystem.tokens.TutoraColors
import app.tutora.designsystem.tokens.TutoraTokens.Size
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.designsystem.tokens.hexColor
import app.tutora.domain.Language
import app.tutora.domain.theme.colorSets
import app.tutora.domain.theme.colorTokens
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h2000dp")
class DesignSystemTest {
    @get:Rule val compose = createComposeRule()

    private fun camel(name: String) =
        name.split('-').mapIndexed { i, p -> if (i == 0) p else p.replaceFirstChar(Char::uppercase) }.joinToString("")

    @Test
    fun `AC-002-1 the Kotlin tokens come from design-tokens-json, generated on every build`() {
        val color = JSONObject(File("../../design/tokens.json").readText()).getJSONObject("color")
        val tokens = color.getJSONArray("tokens")
        val sets = (0 until color.getJSONArray("themes").length()).map { color.getJSONArray("themes").getJSONObject(it).getString("id") }
        assertEquals(sets.toSet(), colorSets.keys)
        for (i in 0 until tokens.length()) {
            val t = tokens.getJSONObject(i)
            for (set in sets) {
                assertEquals(
                    t.getJSONObject("value").getString(set).lowercase(),
                    colorSets.getValue(set).getValue(t.getString("name")),
                )
            }
        }
    }

    @Test
    fun `AC-002-3 every color token is reachable by name, with spacing, radius and type scales`() {
        val fields =
            TutoraColors::class.java.declaredFields
                .map { it.name }
                .toSet()
        for (token in colorTokens) assertTrue(token, camel(token) in fields)
        assertEquals(16.dp, Spacing.s4)
        // 48 since spec 016 (AC-016-8).
        assertEquals(48.dp, Size.touch)
    }

    @Test
    fun `AC-002-4 spacing renders at its size in dp whatever the text size`() {
        compose.setContent {
            TutoraTheme(largeText = true, dark = false) {
                Box(Modifier.padding(Spacing.s4)) { Box(Modifier.size(Size.touch).testTag("child")) }
            }
        }
        compose.onNodeWithTag("child").assertLeftPositionInRootIsEqualTo(16.dp)
    }

    @Test
    fun `AC-002-5 changing the theme repaints at once, with no restart`() {
        var theme by mutableStateOf("quadro")
        var bg = Color.Unspecified
        compose.setContent { TutoraTheme(themeId = theme, dark = false) { bg = TutoraTheme.colors.background } }
        compose.waitForIdle()
        assertEquals(hexColor(colorSets.getValue("quadro-light").getValue("background")), bg)
        theme = "caderno"
        compose.waitForIdle()
        assertEquals(hexColor(colorSets.getValue("caderno-light").getValue("background")), bg)
    }

    @Test
    fun `AC-002-10 AC-002-11 the 30 components render in a light and a dark token set`() {
        var isDark by mutableStateOf(false)
        compose.setContent {
            TutoraTheme(dark = isDark) {
                Box(
                    Modifier.background(TutoraTheme.colors.background),
                ) { Column(Modifier.verticalScroll(rememberScrollState())) { GallerySections() } }
            }
        }
        // 30 from spec 002, MonthBars (009), four from 016, BusyBlock (012), three from 013, AgendaCard and DayHeading (016's Semana), UndoBar and NumberField (014), NavRail and SidePanel (017), ChoiceField (019).
        assertEquals(46, galleryComponents.size)
        for (name in galleryComponents) compose.onNodeWithTag("section-$name").assertExists()
        isDark = true
        compose.waitForIdle()
        for (name in galleryComponents) compose.onNodeWithTag("section-$name").assertExists()
    }

    @Test
    fun `AC-002-11 variants respond to taps`() {
        compose.setContent { TutoraTheme(dark = false) { Column(Modifier.verticalScroll(rememberScrollState())) { GallerySections() } } }
        val ano = compose.onAllNodesWithText("Ano")[0]
        ano.performScrollTo().performClick()
        assertEquals(true, ano.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Selected))
    }

    @Test
    fun `AC-002-12 every pressable has a 44 x 44 hit area and a label`() {
        compose.setContent { TutoraTheme(dark = false) { Column(Modifier.verticalScroll(rememberScrollState())) { GallerySections() } } }
        compose.waitForIdle()
        val problems = mutableListOf<String>()
        val density =
            compose
                .onRoot()
                .fetchSemanticsNode()
                .layoutInfo.density.density

        fun walk(node: SemanticsNode) {
            if (node.config.getOrNull(SemanticsActions.OnClick) != null) {
                // Layout size, so rows scrolled out of view are measured too.
                val w = node.size.width / density
                val h = node.size.height / density
                val c = node.config
                val label = c.getOrNull(SemanticsProperties.ContentDescription)?.joinToString() ?: ""
                val text =
                    (c.getOrNull(SemanticsProperties.Text)?.joinToString() ?: "") +
                        (c.getOrNull(SemanticsProperties.EditableText)?.text ?: "")
                if (w < 44f - 0.5f || h < 44f - 0.5f) problems += "too small ${w}x$h: $label$text"
                if (label.isBlank() && text.isBlank()) problems += "no label at ${node.boundsInRoot}"
            }
            node.children.forEach(::walk)
        }
        walk(compose.onRoot(useUnmergedTree = false).fetchSemanticsNode())
        assertEquals(emptyList<String>(), problems)
    }

    @Test
    fun `AC-002-13 money is integer cents formatted for the app language, in reais`() {
        fun plain(s: String) = s.replace(' ', ' ').replace(' ', ' ')
        assertEquals("R$ 1.234,56", plain(formatMoney(123_456, Language.PT_BR)))
        assertEquals("R$1,234.56", plain(formatMoney(123_456, Language.EN)))
        assertEquals("tnum", TutoraTypography(1f).moneyMd.fontFeatureSettings)
    }

    @Test
    fun `AC-003-5 negative amounts keep their sign in both languages`() {
        fun plain(s: String) = s.replace('\u00a0', ' ').replace('\u202f', ' ')
        assertEquals("-R$ 240,00", plain(formatMoney(-24_000, Language.PT_BR)))
        assertEquals("-R$240.00", plain(formatMoney(-24_000, Language.EN)))
        assertEquals("-R$ 1.181", plain(formatMoney(-118_050, Language.PT_BR, compact = true)))
    }

    @Test
    fun `AC-003-6 in Portuguese amounts read R$ 1234,56 and in English R$1234,56, the same amount`() {
        fun plain(s: String) = s.replace('\u00a0', ' ').replace('\u202f', ' ')
        assertEquals("R$ 1.234,56", plain(formatMoney(123_456, Language.PT_BR)))
        assertEquals("R$1,234.56", plain(formatMoney(123_456, Language.EN)))
        assertEquals("R$ 1.181", plain(formatMoney(118_050, Language.PT_BR, compact = true)))
    }

    @Test
    fun `AC-002-15 the developer screen shows every component with a theme and mode switcher`() {
        compose.setContent { ComponentGallery() }
        for (name in galleryComponents) compose.onNodeWithTag("section-$name").assertExists()
        compose.onAllNodesWithText("Dark")[0].performClick()
        assertTrue(
            compose
                .onAllNodesWithText("Dark")[0]
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.Selected) == true,
        )
        assertTrue(compose.onAllNodes(isSelectable()).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun `AC-002-16 Large text scales every type style by 1,15`() {
        val normal = TutoraTypography(1f)
        val large = TutoraTypography(LARGE_TEXT_SCALE)
        assertEquals(normal.body.fontSize.value * 1.15f, large.body.fontSize.value, 0.01f)
        assertEquals(normal.moneyXl.fontSize.value * 1.15f, large.moneyXl.fontSize.value, 0.01f)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `AC-002-16 a segment label too long for one line wraps instead of being clipped, at 360 px with Large text`() {
        val long = "Também com o app fechado"
        compose.setContent {
            TutoraTheme(dark = false, largeText = true) {
                Box(Modifier.size(width = 360.dp, height = 400.dp)) {
                    app.tutora.designsystem.components.SegmentedControl(
                        listOf(1 to "Só com o app aberto", 2 to long),
                        1,
                        {},
                        "Início automático",
                        size = app.tutora.designsystem.components.ControlSize.SM,
                    )
                }
            }
        }
        val node = compose.onAllNodesWithText(long, useUnmergedTree = true).fetchSemanticsNodes().single()
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val layout = layouts.single()
        assertTrue("the label is not clipped", !layout.hasVisualOverflow)
        assertTrue("the label wraps to fit", layout.lineCount > 1)
    }

    @Test
    fun `AC-002-17 with reduce motion, components change state without animation`() {
        assertTrue(motionSpec<Float>(reduceMotion = true) is SnapSpec)
        assertTrue(motionSpec<Float>(reduceMotion = false) !is SnapSpec)
    }

    @Test
    fun `AC-002-16 with Large text the live clock stays on one line inside its ring`() {
        compose.setContent {
            TutoraTheme(
                largeText = true,
            ) {
                app.tutora.designsystem.components
                    .TimerRing("00:00:04", 0.1f, caption = "de 01:00 planejados")
            }
        }
        val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose
            .onNodeWithText("00:00:04")
            .fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(results)
        assertEquals(1, results.first().lineCount)
        assertTrue(!results.first().hasVisualOverflow)
    }

    @Test
    fun `AC-014-3 a class to confirm reads To confirm on the Agenda's blocks and cards and in a lesson row`() {
        compose.setContent {
            TutoraTheme(dark = false) {
                Column {
                    app.tutora.designsystem.components.AgendaBlock(
                        app.tutora.designsystem.components.Tag.GREEN,
                        "Ana",
                        state = app.tutora.designsystem.components.BlockState.TO_CONFIRM,
                    )
                    app.tutora.designsystem.components.AgendaCard(
                        "14:00",
                        "15:00",
                        app.tutora.designsystem.components.Tag.GREEN,
                        "Bia",
                        state = app.tutora.designsystem.components.BlockState.TO_CONFIRM,
                    )
                }
            }
        }
        assertEquals(2, compose.onAllNodesWithText("To confirm").fetchSemanticsNodes().size)
    }

    @Test
    fun `AC-014-5 a month day holding a class to confirm shows a ring, and the Agenda tab its count`() {
        compose.setContent {
            TutoraTheme(dark = false) {
                Column {
                    app.tutora.designsystem.components.MonthGrid(
                        listOf(
                            app.tutora.designsystem.components
                                .MonthDay(19, true, emptyList(), 0, "19", pending = true),
                            app.tutora.designsystem.components
                                .MonthDay(20, true, emptyList(), 0, "20"),
                        ),
                        null,
                        {},
                        listOf("S", "T", "Q", "Q", "S", "S", "D"),
                        "Outubro",
                    )
                    app.tutora.designsystem.components.TabBar(
                        listOf(
                            app.tutora.designsystem.components.TabItem(
                                "schedule",
                                "Agenda",
                                app.tutora.designsystem.components.IconName.CALENDAR,
                                count = 3,
                            ),
                        ),
                        "today",
                        {},
                    )
                }
            }
        }
        assertEquals(
            1,
            compose
                .onAllNodes(
                    androidx.compose.ui.test
                        .hasTestTag("month-pending"),
                ).fetchSemanticsNodes()
                .size,
        )
        compose.onNodeWithTag("tab-count-schedule", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `AC-014-29 a stepper's value can be typed - percent, money, and out of range kept within bounds`() {
        var bp by mutableStateOf(3_000L)
        var cents by mutableStateOf(10_000L)
        compose.setContent {
            TutoraTheme(dark = false) {
                Column {
                    app.tutora.designsystem.components.Stepper(
                        bp,
                        { bp = it },
                        step = 500,
                        max = 9_000,
                        format = { "${it / 100}%" },
                        valueLabel = "Comissão",
                        typedScale = 100,
                    )
                    app.tutora.designsystem.components.Stepper(
                        cents,
                        { cents = it },
                        step = 500,
                        min = 500,
                        format = { "R$ ${it / 100}" },
                        valueLabel = "Preço",
                        input = app.tutora.designsystem.components.StepperInput.MONEY,
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("Comissão, 30%").performClick()
        compose.onNodeWithTag("stepper-entry").performTextReplacement("45")
        compose.onNodeWithTag("stepper-entry").performImeAction()
        compose.waitForIdle()
        assertEquals(4_500L, bp)
        compose.onNodeWithContentDescription("Comissão, 45%").performClick()
        compose.onNodeWithTag("stepper-entry").performTextReplacement("95")
        compose.onNodeWithTag("stepper-entry").performImeAction()
        compose.waitForIdle()
        assertEquals(9_000L, bp)
        compose.onNodeWithContentDescription("Preço, R$ 100").performClick()
        compose.onNodeWithTag("stepper-entry").performTextReplacement("13550")
        compose.onNodeWithTag("stepper-entry").performImeAction()
        compose.waitForIdle()
        assertEquals(13_550L, cents)
    }

    @Test
    fun `AC-014-37 a number is typed in its field, with − and + inside, and a duration moves by its step`() {
        var count by mutableStateOf(10L)
        var minutes by mutableStateOf(60)
        compose.setContent {
            TutoraTheme(dark = false) {
                Column {
                    app.tutora.designsystem.components
                        .NumberField("Aulas", count, { count = it }, min = 1, max = 100)
                    app.tutora.designsystem.components
                        .DurationField("Duração", minutes, { minutes = it ?: minutes }, step = 5, minMinutes = 5)
                }
            }
        }
        compose.onNodeWithContentDescription("Increase Aulas").performClick()
        compose.waitForIdle()
        assertEquals(11L, count)
        compose.onNodeWithText("11").performTextReplacement("25")
        compose.waitForIdle()
        assertEquals(25L, count)
        // Past the maximum, the number stays.
        compose.onNodeWithText("25").performTextReplacement("250")
        compose.waitForIdle()
        assertEquals(25L, count)
        compose.onNodeWithContentDescription("Decrease Duração").performClick()
        compose.waitForIdle()
        assertEquals(55, minutes)
        compose.onNodeWithText("00:55").assertExists()
    }

    private val tabs =
        listOf(
            app.tutora.designsystem.components
                .TabItem("today", "Hoje", app.tutora.designsystem.components.IconName.HOME),
            app.tutora.designsystem.components
                .TabItem("schedule", "Agenda", app.tutora.designsystem.components.IconName.CALENDAR, 2),
            app.tutora.designsystem.components
                .TabItem("registry", "Cadastro", app.tutora.designsystem.components.IconName.USERS),
            app.tutora.designsystem.components
                .TabItem("earnings", "Ganhos", app.tutora.designsystem.components.IconName.WALLET),
            app.tutora.designsystem.components
                .TabItem("settings", "Ajustes", app.tutora.designsystem.components.IconName.GEAR),
        )

    @Test
    @Config(qualifiers = "w800dp-h360dp-land")
    fun `AC-017-2 sideways the five tabs stand in a rail on the left with the same labels, pill and count`() {
        var landscape = false
        compose.setContent {
            TutoraTheme(dark = false) {
                landscape = TutoraTheme.landscape
                androidx.compose.foundation.layout.Row {
                    app.tutora.designsystem.components
                        .NavRail(tabs, active = "schedule", onSelect = {})
                    Box(Modifier.weight(1f).size(Size.touch).testTag("content"))
                }
            }
        }
        assertTrue(landscape)
        val rail = compose.onNodeWithTag("nav-rail").fetchSemanticsNode().boundsInRoot
        assertEquals(0f, rail.left)
        assertEquals(
            Size.rail.value,
            compose
                .onNodeWithTag("nav-rail")
                .fetchSemanticsNode()
                .size.width / compose.density.density,
            1f,
        )
        // One tab under the other, in order.
        val tops =
            tabs.map {
                compose
                    .onNodeWithTag("tab-${it.key}")
                    .fetchSemanticsNode()
                    .boundsInRoot.top
            }
        assertEquals(tops.sorted(), tops)
        tabs.forEach { compose.onNodeWithText(it.label).assertExists() }
        compose.onNodeWithTag("tab-pill-schedule", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("tab-count-schedule", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("tab-schedule").assertIsSelected()
        // The content takes the rest of the width.
        assertEquals(
            rail.right,
            compose
                .onNodeWithTag("content")
                .fetchSemanticsNode()
                .boundsInRoot.left,
        )
    }

    @Test
    @Config(qualifiers = "w800dp-h360dp-land")
    fun `AC-017-4 content wider than the readable width sits centered at it, with the background on both sides`() {
        compose.setContent {
            TutoraTheme(dark = false) {
                Box(Modifier.background(TutoraTheme.colors.background).testTag("screen")) {
                    Column(Modifier.readableWidth().testTag("readable")) { Box(Modifier.size(Size.touch)) }
                }
            }
        }
        val screen = compose.onNodeWithTag("screen").fetchSemanticsNode().boundsInRoot
        val content = compose.onNodeWithTag("readable").fetchSemanticsNode().boundsInRoot
        val d = compose.density.density
        assertEquals(Size.contentMax.value, content.width / d, 1f)
        assertEquals(content.left - screen.left, screen.right - content.right, 1f)
        assertTrue(content.left > 0f)
    }

    @Test
    @Config(qualifiers = "w800dp-h360dp-land")
    fun `AC-017-15 a sheet sideways opens at the readable width, centered, and its tall body scrolls`() {
        compose.setContent {
            TutoraTheme(dark = false) {
                app.tutora.designsystem.components.BottomSheet("Pagamento", onDismiss = {}) {
                    repeat(30) { Text("Linha $it", Modifier.testTag("line-$it")) }
                }
            }
        }
        compose.waitForIdle()
        val sheet = compose.onNodeWithContentDescription("Pagamento").fetchSemanticsNode().boundsInRoot
        val d = compose.density.density
        assertTrue("sheet ${sheet.width / d} dp wide", sheet.width / d <= Size.contentMax.value + 1f)
        // Centered in the 800 dp window: as much room on the left as on the right.
        assertEquals(sheet.left / d, 800f - sheet.right / d, 2f)
        compose.onNodeWithTag("line-29").performScrollTo().assertIsDisplayed()
    }
}
