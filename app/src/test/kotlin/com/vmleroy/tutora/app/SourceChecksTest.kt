package com.vmleroy.tutora.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class SourceChecksTest {
    private fun names(file: String): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(file))
        val nodes = doc.documentElement.childNodes
        return (0 until nodes.length)
            .map { nodes.item(it) }
            .filter { it.nodeName == "string" || it.nodeName == "plurals" }
            .filter { it.attributes.getNamedItem("translatable")?.nodeValue != "false" }
            .associate { it.attributes.getNamedItem("name").nodeValue to it.textContent.trim() }
    }

    @Test
    fun `AC-001-7 every string exists in English and Portuguese, none empty`() {
        val en = names("src/main/res/values/strings.xml")
        val pt = names("src/main/res/values-pt-rBR/strings.xml")
        assertEquals(en.keys.sorted(), pt.keys.sorted())
        assertEquals(emptyList<String>(), (en + pt.mapKeys { "pt:" + it.key }).filterValues { it.isEmpty() }.keys.toList())
    }

    private fun uiSources() =
        listOf(File("src/main/kotlin/com/vmleroy/tutora/app/ui"), File("../core/designsystem/src/main/kotlin"))
            .flatMap { root -> root.walkTopDown().filter { it.extension == "kt" }.toList() }

    @Test
    fun `AC-002-14 no raw font size or spacing number in screens or design-system components`() {
        // Literal dp or sp values (16.dp, 12.sp) must come from TutoraTokens instead.
        val raw = Regex("""(?<![\w.])\d+(\.\d+)?\.(dp|sp)\b""")
        val offenders = uiSources().filter { raw.containsMatchIn(it.readText()) }.map { it.name }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `screens never touch the data layer (constitution rule 6)`() {
        val offenders =
            File("src/main/kotlin/com/vmleroy/tutora/app/ui")
                .walkTopDown()
                .filter { it.extension == "kt" }
                .filter { Regex("""^import com\.vmleroy\.tutora\.data\.""", RegexOption.MULTILINE).containsMatchIn(it.readText()) }
                .map { it.name }
                .toList()
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `AC-001-10 screens and design-system code use theme colors, never raw ones`() {
        val raw = Regex("""Color\(0x|Color\.(Red|Green|Blue|Black|White|Gray|Yellow|Cyan|Magenta)\b|Color\(\s*\d""")
        val roots = listOf(File("src/main/kotlin"), File("../core/designsystem/src/main/kotlin"))
        val offenders =
            roots
                .flatMap { root -> root.walkTopDown().filter { it.extension == "kt" }.toList() }
                .filter { raw.containsMatchIn(it.readText()) }
                .map { it.name }
        assertEquals(emptyList<String>(), offenders)
    }
}
