package app.tutora.designsystem

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.designsystem.components.EarningsStat
import app.tutora.designsystem.components.EarningsSummary
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.MoneyTone
import app.tutora.designsystem.components.ScreenHeader
import app.tutora.designsystem.tokens.TutoraColors
import app.tutora.domain.theme.CustomColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// Spec 016: the 2026 look. Pixel checks read the theme's own colors, never hard-coded values.
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h2000dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RefreshTest {
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
    fun `AC-016-1 no header paints a band of its own, Hoje's included`() {
        show {
            Column {
                HeaderVariant.entries.forEach { v ->
                    Box(Modifier.testTag("h-$v")) { ScreenHeader("Boa tarde", variant = v, eyebrow = "Segunda, 26 de outubro") }
                }
            }
        }
        HeaderVariant.entries.forEach { same(colors.background, pixel("h-$it", 2.dp, 2.dp)) }
    }

    @Test
    fun `AC-016-2 AC-016-3 the summary card is painted Topo, owed in the red tile and paid in the green`() {
        show {
            EarningsSummary(
                "Ganho no período",
                502_000,
                stats =
                    listOf(
                        EarningsStat("Recebido", 420_000, MoneyTone.PAID),
                        EarningsStat("A receber", 82_000, MoneyTone.OWED),
                        EarningsStat("Semana", 118_000),
                    ),
            )
        }
        same(colors.header, pixel("summary-card", 200.dp, 6.dp))
        same(colors.dangerSoft, pixel("summary-tile-owed", 20.dp, 4.dp))
        same(colors.successSoft, pixel("summary-tile-paid", 20.dp, 4.dp))
        assertNotEquals(colors.header.toArgb(), pixel("summary-tile-plain", 20.dp, 4.dp))
    }

    @Test
    fun `AC-016-4 a list is one card of rows with a section title above it`() {
        show {
            ListGroup(
                listOf({ ListRow("Ana Souza") }, { ListRow("Pedro Lima") }),
                Modifier.padding(16.dp),
                title = "Aulas de hoje",
            )
        }
        same(colors.surface, pixel("list-group", 40.dp, 10.dp))
        // The corners are rounded 28: the very corner shows the page behind.
        same(colors.background, pixel("list-group", 1.dp, 1.dp))
    }
}
