package app.tutora

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.Group
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
import kotlinx.coroutines.flow.first
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
    fun `AC-020-29 Relatorio is only for the one it was opened from, with no switch or list of names`() {
        openReport()
        click("Mês passado", scroll = false)
        exists("01/09/2026")
        exists("Ana Souza")
        listOf("Aluno", "Turma", "Local").forEach {
            compose.onAllNodes(matching(it), useUnmergedTree = true).assertCountEquals(0)
        }
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
        compose.onNodeWithTag("open-report").performClick()
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
    fun `AC-010-16 Seus dados opens from the top of Ajustes, and the copied text carries the name and Pix key`() {
        h.show(compose)
        h.go(compose, SettingsGraph)
        click("Seus dados")
        compose.onNodeWithTag("personal-screen").assertExists()
        compose.onNodeWithContentDescription("Seu nome").performTextReplacement("Victor")
        h.awaitDb(compose) { h.services.currentSettings().teacherName == "Victor" }
        compose.onNodeWithContentDescription("Chave Pix").performTextReplacement("victor@pix.com")
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

    @Test
    fun `AC-019-15 the title typed in Seus dados replaces Tutora's at the top of the PDF, and empty keeps it`() {
        openReport()
        click("PDF")
        assertTrue(
            String(
                h.exports.shared
                    .last()
                    .second,
            ).contains("brand=TUTORA · CONTROLE DE AULAS"),
        )
        h.go(compose, app.tutora.ui.PersonalData)
        compose
            .onNode(
                androidx.compose.ui.test
                    .hasSetTextAction() and
                    androidx.compose.ui.test
                        .hasAnyAncestor(
                            androidx.compose.ui.test
                                .hasTestTag("report-title"),
                        ),
            ).performTextInput("Aulas da Profa. Ana")
        h.awaitDb(compose) { h.services.currentSettings().reportTitle == "Aulas da Profa. Ana" }
        h.go(compose, StudentDetail(ana.id))
        compose.onNodeWithContentDescription("Relatório").performClick()
        h.settle(compose)
        click("PDF")
        assertTrue(
            String(
                h.exports.shared
                    .last()
                    .second,
            ).contains("brand=Aulas da Profa. Ana"),
        )
    }

    @Test
    fun `AC-020-1 Relatorio keeps its export and adds Enviar with the Fatura and the detailed report`() {
        openReport()
        listOf(
            "Exportar este relatório",
            "Enviar",
            "Fatura",
            "Para receber: valor e Pix",
            "Relatório detalhado",
            "Cada aula com dia, horário e deslocamento",
        ).forEach { exists(it) }
    }

    @Test
    fun `AC-020-2 the Fatura and the detailed report are shared under names that say which`() {
        openReport()
        compose.onNodeWithTag("send-invoice").performScrollTo().performClick()
        h.settle(compose)
        compose.onNodeWithTag("sheet-pdf").performClick()
        h.settle(compose)
        compose.onNodeWithTag("send-detail").performScrollTo().performClick()
        h.settle(compose)
        compose.onNodeWithTag("sheet-pdf").performClick()
        h.settle(compose)
        assertEquals(
            listOf("Ana Souza - fatura - outubro 2026.pdf", "Ana Souza - relatório detalhado - outubro 2026.pdf"),
            h.exports.shared.map { it.first },
        )
    }

    @Test
    fun `AC-020-11 without a Pix key Relatorio asks for one in Seus dados`() {
        openReport()
        click("Adicione sua chave Pix em Seus dados")
        compose.onNodeWithTag("personal-screen").assertExists()
    }

    private fun groupReport() {
        val gid =
            runBlocking {
                h.t.catalog.saveStudent(
                    Student("", "Bia Lima", null, null, null, TagColor.AMBER, RateType.HOUR, null),
                    listOf(PlaceLink(online, true, 12_000)),
                )
                val ids =
                    h.services.catalogRepo
                        .students()
                        .first()
                        .map { it.student.id }
                val g = (h.t.catalog.saveGroup(Group("", online, "Inglês B", null, 10_000, ids, null)) as Result.Ok).value
                h.t.lessons.logLesson(
                    LessonDraft(WhoId.OfGroup(g.id), online, LocalDate(2026, 10, 20), LocalTime(9, 0), 60, null, null, null),
                )
                g.id
            }
        h.show(compose)
        h.go(compose, app.tutora.ui.Report("GROUP", gid))
    }

    @Test
    fun `AC-020-20 a private group's Fatura goes to the whole group or split by student`() {
        groupReport()
        compose.onNodeWithTag("send-invoice").performScrollTo().performClick()
        h.settle(compose)
        exists("Turma inteira")
        exists("Dividir por aluno")
        compose.onNodeWithTag("sheet-pdf").performClick()
        h.settle(compose)
        assertEquals(
            "Inglês B - fatura - outubro 2026.pdf",
            h.exports.shared
                .single()
                .first,
        )
    }

    @Test
    fun `AC-020-21 split by student, each member gets their own Fatura for their share`() {
        groupReport()
        compose.onNodeWithTag("send-invoice").performScrollTo().performClick()
        h.settle(compose)
        click("Dividir por aluno", scroll = false)
        compose.onNodeWithTag("member-first-Bia Lima").performClick()
        h.settle(compose)
        compose.onNodeWithTag("member-second-Ana Souza").performClick()
        h.settle(compose)
        assertEquals(
            "Bia Lima - fatura - outubro 2026.pdf",
            h.exports.shared
                .single()
                .first,
        )
        val text = plain(h.exports.copied.single())
        assertTrue(text, text.contains("Total a pagar: R$ 50,00"))
    }

    @Test
    fun `AC-020-30 the period is one row over De and Ate, and dates picked by hand light none of it`() {
        openReport()
        compose.onNodeWithTag("report-period").assertExists()
        compose.onAllNodes(matching("Outro…"), useUnmergedTree = true).assertCountEquals(0)

        fun lit() =
            listOf("Este mês", "Mês passado", "3 meses").filter { label ->
                compose
                    .onAllNodes(matching(label), useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .any { n -> generateSequence(n) { it.parent }.any { it.config.getOrNull(SemanticsProperties.Selected) == true } }
            }
        assertEquals(listOf("Este mês"), lit())
        // De: the picker opens on 01/10/2026; OK keeps it, which is now a period of its own.
        click("01/10/2026")
        click("OK", scroll = false)
        assertEquals(emptyList<String>(), lit())
        exists("01/10/2026")
    }
}
