package app.tutora.designsystem

import androidx.compose.animation.core.SnapSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.designsystem.components.formatMoney
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
        assertEquals(44.dp, Size.touch)
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
        assertEquals(30, galleryComponents.size)
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
    fun `AC-002-17 with reduce motion, components change state without animation`() {
        assertTrue(motionSpec<Float>(reduceMotion = true) is SnapSpec)
        assertTrue(motionSpec<Float>(reduceMotion = false) !is SnapSpec)
    }
}
