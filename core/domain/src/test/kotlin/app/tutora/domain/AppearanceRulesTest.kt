package app.tutora.domain

import app.tutora.domain.theme.ContrastLevel
import app.tutora.domain.theme.CustomPart
import app.tutora.domain.theme.Hsl
import app.tutora.domain.theme.Scheme
import app.tutora.domain.theme.applyQuick
import app.tutora.domain.theme.hslOf
import app.tutora.domain.theme.lowContrastParts
import app.tutora.domain.theme.partContrast
import app.tutora.domain.theme.quickHighlight
import app.tutora.domain.theme.quickMain
import app.tutora.domain.theme.readHex
import app.tutora.domain.theme.resolvePalette
import app.tutora.domain.theme.toHex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceRulesTest {
    @Test
    fun `AC-013-6 colors no saved theme holds are unsaved`() {
        val wine = mapOf(CustomPart.PRIMARY_BTN to "#7a2e3b")
        val s = Settings(Language.PT_BR, customColors = wine)
        assertTrue(s.unsavedColors())
        assertFalse(s.copy(savedThemes = listOf(SavedTheme("t", "Meu tema", "quadro", wine))).unsavedColors())
        assertFalse(Settings(Language.PT_BR).unsavedColors())
    }

    @Test
    fun `AC-013-8 Cor principal paints Topo, buttons and active tab, Cor de destaque the highlight, Do tema clears them`() {
        val main = applyQuick(emptyMap(), quickMain, "#7A2E3B")
        assertEquals(setOf(CustomPart.HEADER, CustomPart.PRIMARY_BTN, CustomPart.ACTIVE_TAB), main.keys)
        val both = applyQuick(main, quickHighlight, "#F07C6C")
        assertEquals("#f07c6c", both[CustomPart.HIGHLIGHT_BTN])
        assertEquals(setOf(CustomPart.HIGHLIGHT_BTN), applyQuick(both, quickMain, null).keys)
    }

    @Test
    fun `AC-013-12 a hex code is read with or without the hash, and the sliders round-trip it`() {
        assertEquals("#7a2e3b", readHex("#7A2E3B"))
        assertEquals("#7a2e3b", readHex(" 7a2e3b"))
        assertNull(readHex("7a2e3"))
        assertNull(readHex("#7a2e3g"))
        val hsl = hslOf("#7a2e3b")
        assertEquals(Hsl(350, 45, 33), hsl)
        assertEquals(hsl, hslOf(hsl.toHex()))
    }

    @Test
    fun `AC-013-13 the contrast line says which text goes on the color and how well it reads`() {
        val dark = partContrast("quadro", Scheme.LIGHT, CustomPart.PRIMARY_BTN, "#7a2e3b")
        assertEquals(true, dark.whiteText)
        assertEquals(ContrastLevel.GOOD, dark.level)
        val yellow = partContrast("quadro", Scheme.LIGHT, CustomPart.HIGHLIGHT_BTN, "#f2b33d")
        assertEquals(false, yellow.whiteText)
        assertNull(partContrast("quadro", Scheme.LIGHT, CustomPart.EARNINGS, "#2e5e4e").whiteText)
        assertEquals(ContrastLevel.POOR, partContrast("quadro", Scheme.LIGHT, CustomPart.EARNINGS, "#fff8e0").level)
    }

    @Test
    fun `AC-013-14 in dark mode a painted color is said to be shown lighter`() {
        assertTrue(partContrast("quadro", Scheme.DARK, CustomPart.PRIMARY_BTN, "#7a2e3b").lightenedInDark)
        assertFalse(partContrast("quadro", Scheme.LIGHT, CustomPart.PRIMARY_BTN, "#7a2e3b").lightenedInDark)
    }

    @Test
    fun `AC-013-15 leaving asks only when some part reads below 4,5 to 1`() {
        assertEquals(emptyList<CustomPart>(), lowContrastParts("quadro", Scheme.LIGHT, mapOf(CustomPart.PRIMARY_BTN to "#7a2e3b")))
        assertEquals(listOf(CustomPart.EARNINGS), lowContrastParts("quadro", Scheme.LIGHT, mapOf(CustomPart.EARNINGS to "#fff8e0")))
    }

    @Test
    fun `AC-013-17 red and green for owed and paid never change, whatever the colors`() {
        val base = resolvePalette("quadro", Scheme.LIGHT)
        val painted = resolvePalette("quadro", Scheme.LIGHT, CustomPart.entries.associateWith { "#5b4a8b" })
        listOf("danger", "danger-ink", "danger-soft", "success", "success-ink", "success-soft").forEach {
            assertEquals(it, base[it], painted[it])
        }
    }
}
