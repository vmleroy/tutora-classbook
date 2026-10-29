package app.tutora.designsystem

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.Card
import app.tutora.designsystem.components.EarningsSummary
import app.tutora.designsystem.components.IconButton
import app.tutora.designsystem.components.IconButtonVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.Money
import app.tutora.designsystem.components.MoneyTone
import app.tutora.designsystem.components.ON_HEADER_FILL
import app.tutora.designsystem.components.TabBar
import app.tutora.designsystem.components.TabItem
import app.tutora.designsystem.tokens.TutoraColors
import app.tutora.designsystem.tokens.TutoraTokens.Size
import app.tutora.domain.theme.CustomColors
import app.tutora.domain.theme.CustomPart
import app.tutora.domain.theme.Scheme
import app.tutora.domain.theme.checkCustomColor
import app.tutora.domain.theme.contrastRatio
import app.tutora.domain.theme.mix
import app.tutora.domain.theme.relativeLuminance
import app.tutora.domain.theme.resolvePalette
import app.tutora.domain.theme.themeIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// Spec 016, continued from RefreshTest: the colors of the parts and their contrast.
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h2000dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RefreshColorsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var colors: TutoraColors

    private fun show(
        custom: CustomColors = emptyMap(),
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        compose.setContent {
            TutoraTheme(dark = false, custom = custom) {
                colors = TutoraTheme.colors
                // One root size for every test: captures came back empty once it changed between tests.
                Box(Modifier.size(width = 411.dp, height = 640.dp).background(TutoraTheme.colors.background)) { content() }
            }
        }
        compose.waitForIdle()
    }

    /**
     * The color drawn at ([x], [y]) dp inside the node tagged [tag]: the activity's view drawn in
     * software onto a bitmap (Robolectric's hardware copy went blank after a few tests).
     */
    private fun pixel(
        tag: String,
        x: Dp,
        y: Dp,
    ): Int {
        compose.waitForIdle()
        val density = compose.activity.resources.displayMetrics.density
        val bounds = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInWindow
        var color = 0
        compose.runOnUiThread {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            color = bitmap.getPixel((bounds.left + x.value * density).toInt(), (bounds.top + y.value * density).toInt())
        }
        return color
    }

    private fun same(
        expected: Color,
        actual: Int,
    ) = assertEquals(Integer.toHexString(expected.toArgb()), Integer.toHexString(actual))

    @Test
    fun `AC-016-6 Nova aula is a rounded square in the highlight color`() {
        show { Box(Modifier.testTag("fab")) { IconButton(IconName.PLUS, "Nova aula", {}, variant = IconButtonVariant.ACCENT) } }
        val bounds = compose.onNodeWithTag("fab").fetchSemanticsNode().size
        val density =
            compose
                .onRoot()
                .fetchSemanticsNode()
                .layoutInfo.density.density
        assertEquals(Size.fab.value, bounds.width / density, 0.5f)
        // (8, 8) is outside a circle of that size but inside the rounded square.
        same(colors.accent, pixel("fab", 8.dp, 8.dp))
    }

    @Test
    fun `AC-016-7 the tab bar sits on the sunken tone with the active tab on a Menu ativo pill`() {
        show {
            TabBar(
                listOf(TabItem("today", "Hoje", IconName.HOME), TabItem("schedule", "Agenda", IconName.CALENDAR)),
                "today",
                {},
            )
        }
        same(colors.surfaceSunken, pixel("tab-bar", 2.dp, 2.dp))
        same(colors.tabActiveSoft, pixel("tab-pill-today", 3.dp, 12.dp))
    }

    /** The text-on-ground pairs the 2026 look puts on screen, with the ratio each needs. */
    private fun pairs(p: Map<String, String>): List<Triple<String, String, Double>> {
        val (dark, light) = listOf(p.getValue("ink"), p.getValue("background")).sortedBy { relativeLuminance(it) }
        val away = if (relativeLuminance(p.getValue("on-header")) > 0.5) dark else light
        val tile = mix(p.getValue("header"), away, ON_HEADER_FILL.toDouble())
        return listOf(
            Triple("on-header", p.getValue("header"), 4.5),
            Triple("on-header-muted", p.getValue("header"), 4.5),
            Triple("earnings-on-header", p.getValue("header"), 3.0),
            Triple("on-header", tile, 4.5),
            Triple("danger-ink", p.getValue("danger-soft"), 4.5),
            Triple("success-ink", p.getValue("success-soft"), 4.5),
            Triple("on-btn-primary", p.getValue("btn-primary"), 4.5),
            Triple("ink", p.getValue("primary-soft"), 4.5),
            Triple("ink", p.getValue("surface-sunken"), 4.5),
            Triple("ink-muted", p.getValue("surface-sunken"), 4.5),
            Triple("tab-active", p.getValue("tab-active-soft"), 3.0),
            Triple("on-accent", p.getValue("accent"), 3.0),
        )
    }

    @Test
    fun `AC-016-9 every theme, mode and accepted Suas cores choice keeps its text readable`() {
        val samples = listOf("#7a2e3b", "#1f4e5f", "#3d6fb4", "#2d7f86", "#f28c38", "#7bc6a4", "#5b3a8c", "#111111", "#f2b33d")
        val problems = mutableListOf<String>()
        for (theme in themeIds) {
            for (scheme in Scheme.entries) {
                val choices =
                    listOf(emptyMap<CustomPart, String>()) +
                        listOf(CustomPart.HEADER, CustomPart.PRIMARY_BTN, CustomPart.HIGHLIGHT_BTN, CustomPart.ACTIVE_TAB).flatMap { part ->
                            samples.filter { checkCustomColor(theme, scheme, part, it).ok }.map { mapOf(part to it) }
                        }
                for (custom in choices) {
                    val p = resolvePalette(theme, scheme, custom)
                    for ((text, ground, need) in pairs(p)) {
                        val ratio = contrastRatio(p.getValue(text), ground)
                        if (ratio < need) problems += "$theme $scheme $custom: $text on $ground = ${"%.2f".format(ratio)}"
                    }
                }
            }
        }
        assertEquals(emptyList<String>(), problems.distinct())
    }

    @Test
    fun `AC-016-10 every Suas cores part paints something visible`() {
        val custom =
            mapOf(
                CustomPart.HEADER to "#7a2e3b",
                CustomPart.PRIMARY_BTN to "#1f4e5f",
                CustomPart.HIGHLIGHT_BTN to "#f28c38",
                CustomPart.EARNINGS to "#5b3a8c",
                CustomPart.BACKGROUND to "#f6efe4",
                CustomPart.CARDS to "#fffaf2",
                CustomPart.ACTIVE_TAB to "#2d7f86",
            )
        show(custom) {
            Column(Modifier.testTag("page").padding(16.dp)) {
                EarningsSummary("Ganho hoje", 24_000)
                Box(Modifier.testTag("btn")) { Button("Registrar pagamento", {}) }
                Box(Modifier.testTag("fab")) { IconButton(IconName.PLUS, "Nova aula", {}, variant = IconButtonVariant.ACCENT) }
                Card(Modifier.testTag("card")) {
                    Row {
                        Box(Modifier.width(4.dp))
                        Money(240_000, tone = MoneyTone.EARNINGS)
                    }
                    Text("Ana Souza")
                }
                TabBar(listOf(TabItem("today", "Hoje", IconName.HOME)), "today", {})
            }
        }
        same(colors.header, pixel("summary-card", 200.dp, 6.dp))
        same(colors.btnPrimary, pixel("btn", 8.dp, 24.dp))
        same(colors.accent, pixel("fab", 8.dp, 8.dp))
        same(colors.background, pixel("page", 2.dp, 2.dp))
        same(colors.surface, pixel("card", 200.dp, 6.dp))
        same(colors.tabActiveSoft, pixel("tab-pill-today", 3.dp, 12.dp))
        // Each part took the teacher's color (the theme resolves them; the earnings ink paints money).
        assertNotEquals(
            resolvePalette("quadro", Scheme.LIGHT).getValue("header"),
            resolvePalette("quadro", Scheme.LIGHT, custom).getValue("header"),
        )
        assertEquals("#5b3a8c", resolvePalette("quadro", Scheme.LIGHT, custom).getValue("earnings"))
    }
}
