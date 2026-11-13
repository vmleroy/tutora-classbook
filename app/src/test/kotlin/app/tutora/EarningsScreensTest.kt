package app.tutora

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.Payer
import app.tutora.domain.PlaceKind
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.domain.builtinPlace
import app.tutora.ui.EarningsGraph
import app.tutora.usecases.LessonDraft
import app.tutora.usecases.Result
import app.tutora.usecases.WhoId
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Now is Monday 26 Oct 2026, 09:00 in São Paulo.
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class EarningsScreensTest {
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
    }

    private fun draft(
        day: Int,
        time: LocalTime = LocalTime(8, 0),
        place: String = online,
    ) = LessonDraft(WhoId.OfStudent(ana.id), place, LocalDate(2026, 10, day), time, 60, null, null, null)

    /** A class Ana was given on [day] of October at 08:00, R$ 120,00. */
    private fun given(
        day: Int,
        place: String = online,
    ) = runBlocking { h.t.lessons.logLesson(draft(day, place = place)) }

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

    private fun openEarnings() {
        h.show(compose)
        h.go(compose, EarningsGraph)
    }

    @Test
    fun `AC-009-8 Hoje's card shows earned today against expected, the week, the month and what is owed, and opens Ganhos`() {
        given(26)
        runBlocking { h.t.schedule.scheduleLesson(draft(26, LocalTime(14, 0))) }
        h.show(compose)
        listOf("Ganho hoje", "R$ 120,00", "de R$ 240,00 previstos", "Semana", "Outubro", "Alunos devem").forEach { exists(it) }
        compose.onNodeWithTag("today-earnings").performClick()
        h.settle(compose)
        compose.onNodeWithTag("earnings-screen").assertExists()
    }

    @Test
    fun `AC-009-9 Ganhos moves by month, quarter or year, and › stops at the current period`() {
        given(22)
        openEarnings()
        exists("Outubro 2026")
        compose.onNodeWithContentDescription("Próximo período").assertExists()
        compose.onNodeWithContentDescription("Período anterior").performClick()
        h.settle(compose)
        exists("Setembro 2026")
        compose.onAllNodes(matching("Trimestre"), useUnmergedTree = true).onFirst().performClick()
        h.settle(compose)
        exists("Ago – Out 2026")
    }

    @Test
    fun `AC-009-11 the summary shows earned, received and to receive, and the commission taken`() {
        val salinha =
            runBlocking {
                val id =
                    (
                        h.t.catalog.savePlace(
                            builtinPlace(BuiltinPlace.ONLINE).copy(
                                id = "",
                                builtin = null,
                                name = "Salinha",
                                kind = PlaceKind.SALINHA,
                                payer = Payer.STUDENT,
                                rateCents = 10_000,
                                commissionBp = 3_000,
                            ),
                        ) as Result.Ok
                    ).value.id
                h.t.catalog.linkStudents(listOf(ana.id to id))
                id
            }
        given(22, salinha)
        openEarnings()
        listOf("Ganho no período", "R$ 70,00", "Recebido", "A receber", "comissão dos locais: R$ 30,00").forEach { exists(it) }
    }

    @Test
    fun `AC-009-13 De onde veio lists who it came from and opens them`() {
        given(22)
        openEarnings()
        exists("De onde veio")
        compose
            .onAllNodes(matching("Ana Souza"), useUnmergedTree = true)
            .onFirst()
            .performScrollTo()
            .performClick()
        h.settle(compose)
        compose.onNodeWithTag("student-detail").assertExists()
    }

    @Test
    fun `AC-009-14 a period with nothing earned says so`() {
        openEarnings()
        exists("Nada ganho neste período")
    }

    @Test
    @Config(qualifiers = "en-w411dp-h2400dp")
    fun `AC-009-15 Ganhos follows the language and keeps the amounts`() {
        given(22)
        openEarnings()
        listOf("October 2026", "Earned in the period", "R$120.00", "Received", "Where it came from").forEach { exists(it) }
    }

    @Test
    fun `AC-016-26 the period's name opens a picker that jumps to any month`() {
        given(5)
        h.show(compose)
        h.go(compose, EarningsGraph)
        compose.onNodeWithContentDescription("Outubro 2026, escolher período").performClick()
        h.settle(compose)
        exists("Escolher período")
        compose.onNodeWithContentDescription("Setembro 2026").performClick()
        h.settle(compose)
        exists("Setembro 2026")
    }

    @Test
    fun `AC-016-27 what a payment gives on top of its classes counts as earned and received`() {
        given(5)
        runBlocking {
            h.services.payments.record(
                app.tutora.usecases.PaymentDraft(
                    app.tutora.domain.PayerRef(app.tutora.domain.PayerKind.STUDENT, ana.id),
                    20_000,
                    app.tutora.domain.PayMethod.PIX,
                    LocalDate(2026, 10, 26),
                ),
            )
        }
        h.show(compose)
        h.go(compose, EarningsGraph)
        // R$ 120,00 for the class and R$ 80,00 on top.
        exists("R$ 200,00")
        exists("R$ 200")
    }

    @Test
    fun `AC-016-28 Hoje's card opens Ganhos on today's Dia, and the arrows move a day`() {
        given(25)
        given(26)
        h.show(compose)
        compose.onNodeWithTag("today-earnings").performClick()
        h.settle(compose)
        exists("Ganho hoje")
        exists("Seg, 26 out")
        compose.onNodeWithContentDescription("Período anterior").performClick()
        h.settle(compose)
        exists("Dom, 25 out")
        exists("Ganho no dia")
        // The Hoje tab still opens Hoje: the card switched tabs, it did not stack Ganhos on Hoje.
        compose.onNodeWithTag("tab-today").performClick()
        h.settle(compose)
        compose.onNodeWithTag("today-screen").assertExists()
    }

    private fun givenSeptember() =
        runBlocking {
            h.t.lessons.logLesson(
                LessonDraft(WhoId.OfStudent(ana.id), online, LocalDate(2026, 9, 15), LocalTime(8, 0), 60, null, null, null),
            )
        }

    @Test
    fun `AC-014-30 the bars follow the period - weeks on Mes, months on Ano, none on Dia, never another period's`() {
        given(6)
        givenSeptember()
        openEarnings()
        compose.onNodeWithTag("earn-bars").assertExists()
        listOf("1–4", "5–11", "26–31").forEach { exists(it) }
        compose.onAllNodes(matching("set"), useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithText("Ano").performClick()
        h.settle(compose)
        listOf("set", "out").forEach { exists(it) }
        compose.onNodeWithText("Dia").performClick()
        h.settle(compose)
        compose.onNodeWithTag("earn-bars").assertDoesNotExist()
    }

    @Test
    fun `AC-014-31 Evolucao shows the months and compares two of them in reais and percent`() {
        given(6)
        given(8)
        givenSeptember()
        openEarnings()
        compose.onNodeWithTag("see-evolution").performClick()
        h.settle(compose)
        compose.onNodeWithTag("evolution-bars").assertExists()
        compose.onNodeWithContentDescription("set").performClick()
        compose.onNodeWithContentDescription("out").performClick()
        h.settle(compose)
        compose.onNodeWithTag("evolution-compare").assertExists()
        exists("Diferença: +R$ 120,00 (+100%)")
    }

    @Test
    fun `AC-014-33 Ganhos' header has Evolucao and Relatorio as named icons, and no Ver evolucao button`() {
        openEarnings()
        compose.onNodeWithContentDescription("Evolução").assertExists()
        compose.onNodeWithContentDescription("Relatório").assertExists()
        compose.onNodeWithText("Ver evolução").assertDoesNotExist()
        compose.onNodeWithText("Relatório").assertDoesNotExist()
    }

    private fun openEvolution() {
        openEarnings()
        compose.onNodeWithTag("see-evolution").performClick()
        h.settle(compose)
    }

    @Test
    fun `AC-014-34 Evolucao lists each month with its totals, classes and hours`() {
        given(6)
        given(8)
        openEvolution()
        compose.onNodeWithTag("evolution-months").assertExists()
        exists("Outubro 2026")
        exists("Ganho R$ 240,00", part = true)
        exists("2 aulas", part = true)
    }

    @Test
    fun `AC-014-35 Evolucao filters by who and by place kind, with earned and received side by side`() {
        given(6)
        given(8)
        openEvolution()
        listOf("Ganho", "Recebido").forEach { exists(it) }
        choose("evo-who", "Ana Souza")
        exists("Ganho R$ 240,00", part = true)
        choose("place-filter", "Casa do aluno")
        compose.onAllNodes(matching("Ganho R$ 240,00", part = true), useUnmergedTree = true).assertCountEquals(0)
    }

    /** Opens a dropdown field and picks [option] in its sheet. */
    private fun choose(
        field: String,
        option: String,
    ) {
        compose.onNodeWithTag(field).performScrollTo().performClick()
        h.settle(compose)
        compose.onAllNodes(matching(option), useUnmergedTree = true).onLast().performClick()
        h.settle(compose)
        // A field with several choices stays open until Pronto.
        if (compose.onAllNodes(matching("Pronto"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) {
            compose.onAllNodes(matching("Pronto"), useUnmergedTree = true).onLast().performClick()
            h.settle(compose)
        }
    }

    @Test
    fun `AC-019-1 De and Ate always show the period on screen, under the quick periods`() {
        openEvolution()
        listOf("6 meses", "12 meses", "Ano").forEach { exists(it) }
        compose.onNodeWithTag("evo-from").assertExists()
        exists("01/05/2026")
        exists("26/10/2026")
    }

    @Test
    fun `AC-020-33 Ate never shows or takes a day after today, so picking De leaves it where it was`() {
        openEvolution()
        // 6 meses runs to the end of October, but Até shows today.
        exists("26/10/2026")
        compose.onAllNodes(matching("31/10/2026"), useUnmergedTree = true).assertCountEquals(0)
        pickFrom()
        exists("01/05/2026")
        exists("26/10/2026")
        compose.onNodeWithTag("evo-too-long").assertDoesNotExist()
    }

    private fun lit(vararg labels: String) =
        labels.filter { label ->
            compose
                .onAllNodes(matching(label), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .any { n -> generateSequence(n) { it.parent }.any { it.config.getOrNull(SemanticsProperties.Selected) == true } }
        }

    /** De's picker opens on the date shown; OK keeps it, which makes the period one picked by hand. */
    private fun pickFrom() {
        compose.onNodeWithTag("evo-from").performClick()
        h.settle(compose)
        compose.onAllNodes(matching("OK"), useUnmergedTree = true).onLast().performClick()
        h.settle(compose)
    }

    @Test
    fun `AC-020-31 the period is one field, Periodo, and dates picked by hand light no quick period`() {
        openEvolution()
        exists("Período")
        compose.onAllNodes(matching("Personalizado"), useUnmergedTree = true).assertCountEquals(0)
        assertEquals(listOf("6 meses"), lit("6 meses", "12 meses", "Ano"))
        pickFrom()
        assertEquals(emptyList<String>(), lit("6 meses", "12 meses", "Ano"))
        exists("01/05/2026")
    }

    @Test
    fun `AC-019-6 the chosen period is kept through a turn of the phone`() {
        openEvolution()
        pickFrom()
        h.landscape = true
        h.settle(compose)
        h.landscape = false
        h.settle(compose)
        compose.onNodeWithTag("evo-from").assertExists()
        exists("01/05/2026")
    }

    @Test
    fun `AC-019-7 Tipo is a dropdown with Tudo, Particulares and Escola e emprego`() {
        given(6)
        openEvolution()
        compose.onNodeWithText("Particulares").assertDoesNotExist()
        compose.onNodeWithTag("type-filter").performClick()
        h.settle(compose)
        listOf("Tudo", "Particulares", "Escola e emprego").forEach { exists(it) }
        compose.onAllNodes(matching("Escola e emprego"), useUnmergedTree = true).onLast().performClick()
        h.settle(compose)
        exists("Escola e emprego")
        compose.onAllNodes(matching("Ganho R$ 120,00", part = true), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun `AC-019-8 Local is a dropdown of the kinds of place, one or more, none being Todos os locais`() {
        openEvolution()
        exists("Todos os locais")
        compose.onNodeWithTag("place-filter").performScrollTo().performClick()
        h.settle(compose)
        listOf("Online", "Escola").forEach { compose.onAllNodes(matching(it), useUnmergedTree = true).onLast().performClick() }
        compose.onAllNodes(matching("Pronto"), useUnmergedTree = true).onLast().performClick()
        h.settle(compose)
        exists("Online, Escola")
        compose.onNodeWithText("Todos os locais").assertDoesNotExist()
    }

    private fun search() =
        compose.onNode(
            androidx.compose.ui.test
                .hasSetTextAction() and
                androidx.compose.ui.test
                    .hasAnyAncestor(
                        androidx.compose.ui.test
                            .hasTestTag("choice-search"),
                    ),
        )

    @Test
    fun `AC-019-12 Quem is searched as typed, ignoring accents, and takes several at once`() {
        given(6)
        openEvolution()
        compose.onNodeWithTag("evo-who").performScrollTo().performClick()
        h.settle(compose)
        search().performTextInput("ANA SOUZÁ")
        h.settle(compose)
        exists("Ana Souza")
        compose.onAllNodes(matching("Online / Remoto"), useUnmergedTree = true).assertCountEquals(0)
        search().performTextReplacement("")
        h.settle(compose)
        compose.onAllNodes(matching("Ana Souza"), useUnmergedTree = true).onLast().performClick()
        compose.onAllNodes(matching("Online / Remoto"), useUnmergedTree = true).onLast().performClick()
        compose.onAllNodes(matching("Pronto"), useUnmergedTree = true).onLast().performClick()
        h.settle(compose)
        exists("Ana Souza, Online / Remoto")
        exists("Ganho R$ 120,00", part = true)
    }

    @Test
    fun `AC-019-9 Tipo, Quem and Local sit one under the other`() {
        openEvolution()
        val fields = listOf("type-filter", "evo-who", "place-filter").map { compose.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot }
        assertEquals(fields.map { it.top }.sorted(), fields.map { it.top })
        fields.forEach { assertEquals(fields[0].left, it.left, 1f) }
    }

    @Test
    @org.robolectric.annotation.Config(qualifiers = "en-w411dp-h2400dp")
    fun `AC-019-10 the new words read in English`() {
        runBlocking { h.services.saveSettings(h.services.currentSettings().copy(language = app.tutora.domain.Language.EN)) }
        openEvolution()
        listOf("Period", "Type", "Place", "All places").forEach { exists(it) }
    }

    @Test
    fun `AC-019-11 Ganhos' type filter is the same Tipo dropdown`() {
        openEarnings()
        compose.onNodeWithTag("type-filter").assertExists()
        compose.onNodeWithText("Particulares").assertDoesNotExist()
        choose("type-filter", "Particulares")
        exists("Particulares")
    }

    @Test
    fun `AC-019-14 Ganhos' summary shows Recebido and A receber with their cents`() {
        given(6)
        openEarnings()
        compose
            .onAllNodes(
                matching("R$ 120,00") and hasAnyAncestor(hasTestTag("earn-summary")),
                useUnmergedTree = true,
            ).onFirst()
            .assertExists()
        compose
            .onAllNodes(
                matching("R$ 0,00") and hasAnyAncestor(hasTestTag("earn-summary")),
                useUnmergedTree = true,
            ).onFirst()
            .assertExists()
    }
}
