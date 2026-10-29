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
}
