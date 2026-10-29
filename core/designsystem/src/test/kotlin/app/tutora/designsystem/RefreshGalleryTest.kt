package app.tutora.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Spec 016 checks over the whole gallery, apart from RefreshTest's pixel captures.
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h2000dp")
class RefreshGalleryTest {
    @get:Rule val compose = createComposeRule()

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent { TutoraTheme(dark = false) { content() } }
        compose.waitForIdle()
    }

    @Test
    fun `AC-016-8 every pressable is at least 48 dp and named`() {
        show { Column(Modifier.verticalScroll(rememberScrollState())) { GallerySections() } }
        val density =
            compose
                .onRoot()
                .fetchSemanticsNode()
                .layoutInfo.density.density
        val problems = mutableListOf<String>()

        fun walk(node: SemanticsNode) {
            if (node.config.getOrNull(SemanticsActions.OnClick) != null) {
                val w = node.size.width / density
                val h = node.size.height / density
                val c = node.config
                val name =
                    (c.getOrNull(SemanticsProperties.ContentDescription)?.joinToString() ?: "") +
                        (c.getOrNull(SemanticsProperties.Text)?.joinToString() ?: "") +
                        (c.getOrNull(SemanticsProperties.EditableText)?.text ?: "")
                if (w < 48f - 0.5f || h < 48f - 0.5f) problems += "too small ${w}x$h: $name"
                if (name.isBlank()) problems += "no name at ${node.boundsInRoot}"
            }
            node.children.forEach(::walk)
        }
        walk(compose.onRoot().fetchSemanticsNode())
        assertEquals(emptyList<String>(), problems)
    }

    @Test
    fun `AC-016-12 the gallery shows the new and changed pieces`() {
        show { Column(Modifier.verticalScroll(rememberScrollState())) { GallerySections() } }
        listOf("SummaryCard", "SplitButtons", "MonthGrid", "MonthPicker", "ListGroup", "TabBar", "IconButton", "EarningsSummary").forEach {
            compose.onNodeWithTag("section-$it").assertExists()
        }
    }
}
