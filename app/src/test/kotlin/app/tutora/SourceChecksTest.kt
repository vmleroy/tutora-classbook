package app.tutora

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        listOf(File("src/main/kotlin/app/tutora/ui"), File("../core/designsystem/src/main/kotlin"))
            .flatMap { root -> root.walkTopDown().filter { it.extension == "kt" }.toList() }
            // A moved folder must fail here, not pass with nothing checked.
            .also { check(it.isNotEmpty()) { "no UI sources found" } }

    @Test
    fun `AC-002-14 no raw font size or spacing number in screens or design-system components`() {
        // Literal dp or sp values (16.dp, 12.sp) must come from TutoraTokens instead.
        val raw = Regex("""(?<![\w.])\d+(\.\d+)?\.(dp|sp)\b""")
        val offenders = uiSources().filter { raw.containsMatchIn(it.readText()) }.map { it.name }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `screens never touch the data layer (constitution rule 6)`() {
        val ui = File("src/main/kotlin/app/tutora/ui")
        check(ui.isDirectory) { "no UI sources at $ui" }
        val offenders =
            ui
                .walkTopDown()
                .filter { it.extension == "kt" }
                .filter { Regex("""^import app\.tutora\.data\.""", RegexOption.MULTILINE).containsMatchIn(it.readText()) }
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

    @Test
    fun `AC-017-1 turning the phone never recreates the screen, so nothing typed, chosen or open is lost`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val main = Regex("""<activity[^>]*android:name="\.MainActivity"[^>]*>""").find(manifest)?.value.orEmpty()
        val changes =
            Regex("""android:configChanges="([^"]*)"""")
                .find(main)
                ?.groupValues
                ?.get(1)
                .orEmpty()
                .split("|")
                .toSet()
        assertEquals(emptySet<String>(), setOf("orientation", "screenSize", "screenLayout", "smallestScreenSize") - changes)
    }

    @Test
    fun `AC-017-5 the rail and every screen keep clear of the cut-out and the side navigation buttons`() {
        val rail = File("../core/designsystem/src/main/kotlin/app/tutora/designsystem/components/Navigation.kt").readText()
        val navRail = rail.substring(rail.indexOf("fun NavRail("), rail.indexOf("private fun TabButton("))
        org.junit.Assert.assertTrue(
            "NavRail pads for the start insets",
            Regex("""WindowInsets\.safeDrawing\.only\(WindowInsetsSides\.Start""").containsMatchIn(navRail),
        )
        val scaffold = File("src/main/kotlin/app/tutora/ui/Screens.kt").readText()
        val screen = scaffold.substring(scaffold.indexOf("fun ScreenScaffold("), scaffold.indexOf("fun StartupError("))
        org.junit.Assert.assertTrue("ScreenScaffold pads for the safe-drawing insets", ".safeDrawingPadding()" in screen)
        // The screens that draw their own ground pad for the sides themselves.
        listOf("schedule/Today.kt", "schedule/Agenda.kt").forEach {
            val text = File("src/main/kotlin/app/tutora/ui/$it").readText()
            org.junit.Assert.assertTrue("$it pads for the sides", "WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal" in text)
        }
    }

    @Test
    fun `AC-020-32 short fixed choices are segmented rows, not chips`() {
        val ui = File("src/main/kotlin/app/tutora/ui")

        fun src(path: String) = File(ui, path).readText()
        // Each was a row of chips; now a SegmentedControl (or MethodControl, the payment method's).
        val chipRows =
            listOf(
                "payments/Payments.kt" to "PayMethod.entries.forEach",
                "schedule/LessonForm.kt" to "PayMethod.entries.forEach",
                "schedule/LessonForm.kt" to "lesson_repeats_none,",
                "schedule/LessonForm.kt" to "R.string.lesson_whoAll,",
                "schedule/CancelSheet.kt" to "reasons.forEach",
                "catalog/Places.kt" to "PayCycle.LESSON to R.string",
                "setup/Setup.kt" to ".forEach { (m, label) -> Chip",
                "SettingsScreen.kt" to "ConfirmWhen.END to R.string",
                "SettingsScreen.kt" to "LockAfter.entries.forEach",
                "payments/Payments.kt" to "\"all\" to R.string.pay_filterAll",
                "reports/Report.kt" to "Chip(stringResource(R.string.rep_pdf)",
                "earnings/Evolution.kt" to "Chip(",
                "backup/Backup.kt" to "Chip(",
            ).filter { (file, old) -> src(file).contains(old) }
        assertEquals(emptyList<Pair<String, String>>(), chipRows)
        // The payment method is a dropdown field, since four segments cut "Transferência".
        assertTrue(src("payments/Payments.kt").substringAfter("fun MethodControl(").substringBefore("\n}").contains("ChoiceField("))
    }
}
