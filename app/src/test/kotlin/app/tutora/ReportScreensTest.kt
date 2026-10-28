package app.tutora

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.ui.EarningsGraph
import app.tutora.ui.SettingsGraph
import app.tutora.ui.StudentDetail
import app.tutora.usecases.LessonDraft
import app.tutora.usecases.Result
import app.tutora.usecases.WhoId
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Now is Monday 26 Oct 2026, 09:00 in São Paulo.
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class ReportScreensTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val tmp = TemporaryFolder()
    private lateinit var h: Harness
    private lateinit var ana: Student
    private val online = BuiltinPlace.ONLINE.id

    @Before
    fun setUp() {
        h = Harness(TestDb(tmp.root))
        ana =
            runBlocking {
                (
                    h.t.catalog.saveStudent(
                        Student("", "Ana Souza", "Matemática", null, null, TagColor.AMBER, RateType.HOUR, null),
                        listOf(PlaceLink(online, true, 12_000)),
                    ) as Result.Ok
                ).value
            }
        runBlocking {
            h.t.lessons.logLesson(
                LessonDraft(WhoId.OfStudent(ana.id), online, LocalDate(2026, 10, 22), LocalTime(8, 0), 60, null, null, null),
            )
        }
    }

    private fun plain(s: String) = s.replace(' ', ' ').replace(' ', ' ')

    private fun matching(
        text: String,
        part: Boolean = false,
    ) = SemanticsMatcher("text is $text") { n ->
        n.config.getOrNull(SemanticsProperties.Text)?.any { if (part) plain(it.text).contains(text) else plain(it.text) == text } == true
    }

    private fun exists(
        text: String,
        part: Boolean = false,
    ) {
        compose.onAllNodes(matching(text, part), useUnmergedTree = true).onFirst().assertExists()
    }

    private fun click(
        text: String,
        scroll: Boolean = true,
    ) {
        val node = compose.onAllNodes(matching(text), useUnmergedTree = true).onFirst()
        if (scroll) node.performScrollTo()
        node.performClick()
        h.settle(compose)
    }

    private fun openReport() {
        h.show(compose)
        h.go(compose, StudentDetail(ana.id))
        compose.onNodeWithContentDescription("Relatório").performClick()
        h.settle(compose)
    }

    @Test
    fun `AC-010-1 Relatorio on a student's detail opens their report for this month`() {
        openReport()
        compose.onNodeWithTag("report-screen").assertExists()
        exists("01/10/2026")
        exists("1 aula · 1h")
        exists("R$ 120,00")
    }

    @Test
    fun `AC-010-2 switching between Aluno, Turma and Local keeps the period`() {
        openReport()
        click("Mês passado", scroll = false)
        click("Local", scroll = false)
        exists("01/09/2026")
        exists("Online / Remoto")
    }

    @Test
    fun `AC-010-8 the report offers PDF and Excel side by side, and Copiar como texto`() {
        openReport()
        listOf("Exportar este relatório", "PDF", "Excel", "Copiar como texto").forEach { exists(it) }
    }

    @Test
    fun `AC-010-11 PDF and Excel go to the share sheet, named after who and the period`() {
        openReport()
        click("PDF")
        click("Excel")
        assertEquals(listOf("Ana Souza - outubro 2026.pdf", "Ana Souza - outubro 2026.xlsx"), h.exports.shared.map { it.first })
    }

    @Test
    fun `AC-010-12 Copiar como texto copies a short summary and says Copiado`() {
        openReport()
        click("Copiar como texto")
        val text = plain(h.exports.copied.single())
        assertTrue(text.startsWith("Ana Souza · 01/10/2026 a 26/10/2026"))
        assertTrue(text.contains("1 aula · R$ 120,00 · pago R$ 0,00 · a receber R$ 120,00"))
        assertTrue(text.contains("A receber: 22/10"))
        exists("Copiado")
    }

    @Test
    fun `AC-010-13 Ganhos' Relatorio uses the screen's period and type and shares the file`() {
        h.show(compose)
        h.go(compose, EarningsGraph)
        compose.onNodeWithContentDescription("Relatório").performClick()
        h.settle(compose)
        exists("Outubro 2026 · Tudo · usa os filtros da tela")
        listOf("Ganhos por aluno e por local", "Lista de aulas", "Pagamentos recebidos").forEach { exists(it) }
        click("Gerar PDF", scroll = false)
        assertEquals(
            "Relatório de ganhos - Outubro 2026.pdf",
            h.exports.shared
                .single()
                .first,
        )
    }

    @Test
    fun `AC-010-16 Ajustes has Seu nome and Chave Pix, and the copied text carries them`() {
        h.show(compose)
        h.go(compose, SettingsGraph)
        compose.onNodeWithContentDescription("Seu nome").performScrollTo().performTextReplacement("Victor")
        compose.onNodeWithContentDescription("Chave Pix").performScrollTo().performTextReplacement("victor@pix.com")
        h.awaitDb(compose) { h.services.currentSettings().pixKey == "victor@pix.com" }
        h.go(compose, StudentDetail(ana.id))
        compose.onNodeWithContentDescription("Relatório").performClick()
        h.settle(compose)
        click("Copiar como texto")
        val text = h.exports.copied.single()
        assertTrue(text.contains("Pix: victor@pix.com"))
        assertTrue(text.endsWith("— Prof. Victor"))
    }

    @Test
    @Config(qualifiers = "en-w411dp-h2400dp")
    fun `AC-010-17 the report follows the language and keeps the amounts`() {
        h.show(compose)
        h.go(compose, StudentDetail(ana.id))
        compose.onNodeWithContentDescription("Report").performClick()
        h.settle(compose)
        listOf("This month", "Export this report", "Copy as text", "R$120.00").forEach { exists(it) }
    }
}
