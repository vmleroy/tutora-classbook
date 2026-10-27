package com.vmleroy.tutora.domain.theme

import com.vmleroy.tutora.domain.ThemeMode
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteTest {
    private val hex = Arb.int(0..0xFFFFFF).map { "#" + it.toString(16).padStart(6, '0') }
    private val theme = Arb.element(themeIds)
    private val scheme = Arb.element(Scheme.entries)
    private val part = Arb.element(CustomPart.entries)
    private val neverCustom =
        listOf(
            "success",
            "success-soft",
            "success-ink",
            "danger",
            "danger-soft",
            "danger-ink",
            "secondary",
            "secondary-soft",
            "secondary-ink",
            "ink",
            "ink-muted",
            "focus",
        )

    private val aa = 4.5
    private val pairs: List<Triple<String, String, Double>> =
        listOf("background", "surface", "surface-sunken").flatMap { listOf(Triple("ink", it, aa), Triple("ink-muted", it, aa)) } +
            listOf(
                Triple("on-primary", "primary", aa),
                Triple("on-primary-muted", "primary", aa),
                Triple("on-header", "header", aa),
                Triple("on-header-muted", "header", aa),
                Triple("on-accent", "accent", aa),
                Triple("accent-ink", "accent-soft", aa),
                Triple("earnings", "background", aa),
                Triple("earnings", "surface", aa),
                Triple("earnings-on-header", "header", 3.0),
                Triple("success-ink", "success-soft", aa),
                Triple("success-ink", "surface", aa),
                Triple("danger-ink", "danger-soft", aa),
                Triple("danger-ink", "surface", aa),
                Triple("secondary-ink", "secondary-soft", aa),
                Triple("secondary-ink", "surface", aa),
            ) + listOf("amber", "green", "blue", "violet", "orange", "red", "teal").map { Triple("tag-$it-ink", "tag-$it-soft", aa) }

    @Test
    fun `AC-002-2 every named text pair in all eight token sets meets its contrast minimum`() {
        assertEquals(21.0, contrastRatio("#000000", "#ffffff"), 1e-6)
        assertEquals(4.48, contrastRatio("#777777", "#ffffff"), 0.01)
        val failures =
            colorSets.flatMap { (id, set) ->
                pairs
                    .filter { (text, ground, min) ->
                        contrastRatio(set.getValue(text), set.getValue(ground)) < min
                    }.map { "$id ${it.first} on ${it.second}" }
            }
        assertEquals(emptyList<String>(), failures)
    }

    @Test
    fun `AC-002-5 every theme and mode resolves to its own token set`() {
        for (t in themeIds) {
            for (s in Scheme.entries) {
                val p = resolvePalette(t, s)
                assertEquals(colorSets.getValue("$t-${s.name.lowercase()}").getValue("background"), p.getValue("background"))
                assertEquals(p.getValue("primary"), p.getValue("btn-primary"))
            }
        }
        assertEquals(Scheme.DARK, resolveScheme(ThemeMode.SYSTEM, systemDark = true))
        assertEquals(Scheme.LIGHT, resolveScheme(ThemeMode.SYSTEM, systemDark = null))
        assertEquals(Scheme.DARK, resolveScheme(ThemeMode.DARK, systemDark = false))
    }

    @Test
    fun `AC-002-6 a custom color changes only the tokens mapped to its part`() {
        runBlocking {
            checkAll(200, theme, scheme, part, hex) { t, s, p, color ->
                val base = resolvePalette(t, s)
                val custom = resolvePalette(t, s, mapOf(p to color))
                val changed = base.keys.filter { base[it] != custom[it] }
                assertTrue("$p changed $changed", tokensOfPart.getValue(p).containsAll(changed))
                for (k in neverCustom + base.keys.filter { it.startsWith("tag-") }) assertEquals(base[k], custom[k])
            }
        }
        val light =
            resolvePalette(
                "quadro",
                Scheme.LIGHT,
                mapOf(
                    CustomPart.PRIMARY_BTN to "#7a2e3b",
                    CustomPart.BACKGROUND to "#fbf6ef",
                    CustomPart.HEADER to "#2f5c9e",
                ),
            )
        assertEquals("#7a2e3b", light["btn-primary"])
        assertEquals("#fbf6ef", light["background"])
        assertEquals("#2f5c9e", light["header"])
    }

    @Test
    fun `AC-002-7 in dark mode a custom color is lightened in OKLCH and on-text flips by contrast`() {
        runBlocking {
            checkAll(200, theme, hex) { t, color ->
                val p = resolvePalette(t, Scheme.DARK, mapOf(CustomPart.PRIMARY_BTN to color, CustomPart.EARNINGS to color))
                val before = oklchLightness(color)
                for (token in listOf("btn-primary", "earnings")) {
                    assertEquals(before + (1 - before) * 0.4, oklchLightness(p.getValue(token)), 0.05)
                }
            }
            checkAll(200, theme, scheme, hex) { t, s, color ->
                val p = resolvePalette(t, s, mapOf(CustomPart.PRIMARY_BTN to color, CustomPart.HEADER to color))
                val base = colorSets.getValue("$t-${s.name.lowercase()}")
                val candidates =
                    if (s ==
                        Scheme.LIGHT
                    ) {
                        listOf("#ffffff", base.getValue("ink"))
                    } else {
                        listOf(base.getValue("ink"), base.getValue("background"))
                    }
                for ((text, ground) in listOf("on-btn-primary" to "btn-primary", "on-header" to "header")) {
                    val bestRatio = candidates.maxOf { contrastRatio(it, p.getValue(ground)) }
                    assertEquals(bestRatio, contrastRatio(p.getValue(text), p.getValue(ground)), 1e-6)
                }
            }
        }
    }

    @Test
    fun `AC-002-8 a custom color that leaves text under 4,5 is flagged with its ratio`() {
        val low = checkCustomColor("quadro", Scheme.LIGHT, CustomPart.CARDS, "#5c6a70")
        assertFalse(low.ok)
        assertTrue(low.ratio < 4.5)
        val fine = checkCustomColor("quadro", Scheme.LIGHT, CustomPart.PRIMARY_BTN, "#7a2e3b")
        assertTrue(fine.ok)
    }

    @Test
    fun `AC-002-8b in dark mode the header is a deep shade, and a custom header is deepened the same way`() {
        for (t in themeIds) {
            val p = resolvePalette(t, Scheme.DARK)
            assertNotEquals(p["primary"], p["header"])
            assertTrue(oklchLightness(p.getValue("header")) < oklchLightness(p.getValue("primary")))
        }
        runBlocking {
            checkAll(100, theme, hex) { t, color ->
                val p = resolvePalette(t, Scheme.DARK, mapOf(CustomPart.HEADER to color))
                val lifted = resolvePalette(t, Scheme.DARK, mapOf(CustomPart.PRIMARY_BTN to color)).getValue("btn-primary")
                assertEquals(mix(lifted, colorSets.getValue("$t-dark").getValue("background"), 0.68), p["header"])
            }
        }
    }

    @Test
    fun `AC-002-9 no custom colors gives back exactly the theme tokens`() {
        for (t in themeIds) {
            for (s in Scheme.entries) {
                val p = resolvePalette(t, s, emptyMap())
                colorSets.getValue("$t-${s.name.lowercase()}").forEach { (k, v) -> assertEquals(v, p[k]) }
            }
        }
    }
}
