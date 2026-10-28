package app.tutora.designsystem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `AC-001-8 Quadro-negro light and dark use the product doc's colors`() {
        var light = Color.Unspecified
        var dark = Color.Unspecified
        compose.setContent {
            TutoraTheme(dark = false) { light = TutoraTheme.colors.background }
            TutoraTheme(dark = true) { dark = TutoraTheme.colors.background }
        }
        compose.waitForIdle()
        assertEquals(Color(0xFFFAF7F0), light)
        assertEquals(Color(0xFF141C19), dark)
    }

    @Test
    fun `AC-001-9 switching the phone between light and dark repaints at once`() {
        var isDark by mutableStateOf(false)
        var primary = Color.Unspecified
        compose.setContent { TutoraTheme(dark = isDark) { primary = TutoraTheme.colors.background } }
        compose.waitForIdle()
        assertEquals(Color(0xFFFAF7F0), primary)
        isDark = true
        compose.waitForIdle()
        assertEquals(Color(0xFF141C19), primary)
    }
}
