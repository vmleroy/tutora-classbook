package app.tutora

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.CancelReason
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Repeat
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.domain.dayRange
import app.tutora.ui.ScheduleGraph
import app.tutora.usecases.Cancellation
import app.tutora.usecases.LessonDraft
import app.tutora.usecases.Result
import app.tutora.usecases.WhoId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Spec 016, the Agenda by month and year. Now is Monday 26 Oct 2026, 09:00 in São Paulo.
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class AgendaMonthTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val tmp = TemporaryFolder()
    private lateinit var h: Harness
    private val online = BuiltinPlace.ONLINE.id

    @Before
    fun setUp() {
        h = Harness(TestDb(tmp.root))
        val ana =
            runBlocking {
                (
                    h.t.catalog.saveStudent(
                        Student("", "Ana Souza", "Matemática", null, null, TagColor.AMBER, RateType.HOUR, null),
                        listOf(PlaceLink(online, true, 12_000)),
                    ) as Result.Ok
                ).value.id
            }
        runBlocking {
            // Mondays at 14:00, kept 8 weeks ahead, and a one-off on Wednesday 28.
            h.t.schedule.scheduleLesson(
                LessonDraft(WhoId.OfStudent(ana), online, LocalDate(2026, 10, 26), LocalTime(14, 0), 60, null, null, Repeat.WEEKLY),
            )
            h.t.schedule.scheduleLesson(
                LessonDraft(WhoId.OfStudent(ana), online, LocalDate(2026, 10, 28), LocalTime(16, 30), 60, null, null, null),
            )
        }
    }

    private fun plain(s: String) = s.replace('\u00A0', ' ').replace('\u202F', ' ')

    private fun text(t: String) =
        SemanticsMatcher("text is $t") { n -> n.config.getOrNull(SemanticsProperties.Text)?.any { plain(it.text) == t } == true }

    private fun exists(t: String) = compose.onAllNodes(text(t), useUnmergedTree = true).onFirst().assertExists()

    /** Taps the node read as [description], scrolling to it when it sits in a scrolling list. */
    private fun tap(description: String) {
        val node = compose.onNodeWithContentDescription(description)
        runCatching { node.performScrollTo() }
        node.performClick()
        h.settle(compose)
    }

    private fun agenda() {
        h.show(compose)
        h.go(compose, ScheduleGraph)
    }

    private fun month(label: String = "Mês") {
        agenda()
        compose.onNodeWithText(label).performClick()
        h.settle(compose)
    }

    @Test
    fun `AC-016-13 the Agenda is titled by the month and year it shows, under a small Agenda`() {
        agenda()
        exists("Agenda")
        exists("Outubro 2026")
        tap("Próxima semana")
        exists("Novembro 2026")
    }

    @Test
    fun `AC-016-14 Hoje brings the Agenda back to today`() {
        agenda()
        tap("Próxima semana")
        tap("Próxima semana")
        // The header's Hoje comes before the tab's.
        compose.onAllNodesWithText("Hoje").onFirst().performClick()
        h.settle(compose)
        exists("Outubro 2026")
        compose.onNodeWithContentDescription("Seg 26").assertIsSelected()
    }

    @Test
    fun `AC-016-15 the switch reads Dia, Semana and Mes, with the week's dates over the strip`() {
        agenda()
        listOf("Dia", "Semana", "Mês", "Semana de 26 out a 1 nov").forEach { exists(it) }
    }

    @Test
    fun `AC-016-16 Mes shows the month from Monday, the neighbors' days greyed, and moves a month`() {
        month()
        compose.onNodeWithTag("month-grid").assertExists()
        compose.onNodeWithContentDescription("28 de setembro, nenhuma aula").assertIsNotEnabled()
        compose.onNodeWithContentDescription("1 de outubro, nenhuma aula").assertIsEnabled()
        tap("Mês anterior")
        exists("Setembro 2026")
    }

    @Test
    fun `AC-016-17 each day carries a dot per class, a cancelled class none`() {
        runBlocking {
            val (from, to) = dayRange(LocalDate(2026, 10, 28), LocalDate(2026, 10, 28), h.t.ctx.zone())
            val wed =
                h.services.scheduleRepo
                    .between(from, to)
                    .first()
                    .single()
            h.t.lessons.cancel(wed.id, Cancellation(CancelReason.STUDENT))
        }
        month()
        compose.onNodeWithContentDescription("26 de outubro, 1 aula").assertExists()
        compose.onNodeWithContentDescription("28 de outubro, nenhuma aula").assertExists()
        listOf("Escola", "Dada", "Particular", "Turma").forEach { exists(it) }
    }

    @Test
    fun `AC-016-18 tapping a day lists its classes with their count and value`() {
        month()
        tap("28 de outubro, 1 aula")
        compose.onNodeWithContentDescription("28 de outubro, 1 aula").assertIsSelected()
        exists("Qua, 28 out")
        compose
            .onAllNodes(SemanticsMatcher("any text") { it.config.getOrNull(SemanticsProperties.Text) != null }, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .forEach { println("TEXT " + plain(it.config[SemanticsProperties.Text].joinToString())) }
        exists("1 aula · R$ 120")
        compose.onNodeWithText("Ana Souza", useUnmergedTree = true).performScrollTo().performClick()
        h.settle(compose)
        compose.onNodeWithTag("lesson-detail").assertExists()
    }

    @Test
    fun `AC-016-19 every day is read aloud with its date and classes`() {
        month()
        compose.onNodeWithContentDescription("26 de outubro, 1 aula").assertExists()
        compose.onNodeWithContentDescription("27 de outubro, nenhuma aula").assertExists()
    }

    @Test
    fun `AC-016-20 the month and year open a sheet with the year, its months and Voltar para hoje`() {
        agenda()
        tap("Outubro 2026, escolher mês e ano")
        compose.onNodeWithTag("month-picker").assertExists()
        exists("2026")
        exists("Voltar para hoje")
        compose.onNodeWithContentDescription("Outubro 2026, 2 aulas").assertIsSelected()
    }

    @Test
    fun `AC-016-21 choosing a month opens Mes on it`() {
        agenda()
        tap("Outubro 2026, escolher mês e ano")
        compose.onNodeWithContentDescription("Novembro 2026, 5 aulas").performClick()
        h.settle(compose)
        exists("Novembro 2026")
        compose.onNodeWithTag("month-grid").assertExists()
    }

    @Test
    @Config(qualifiers = "en-w411dp-h2400dp")
    fun `AC-016-22 month names and the week follow the language`() {
        month("Month")
        listOf("October 2026", "Month", "M", "School", "Done", "Private", "Group").forEach { exists(it) }
        compose.onNodeWithContentDescription("October 26, 1 lesson").assertExists()
    }

    @Test
    fun `AC-016-23 the Agenda looks 8 weeks ahead and no further`() {
        month()
        tap("Próximo mês")
        tap("Próximo mês")
        exists("Dezembro 2026")
        compose.onNodeWithContentDescription("Próximo mês").assertIsNotEnabled()
        // 21 Dec is the last day with classes; 28 Dec is past the horizon.
        compose.onNodeWithContentDescription("21 de dezembro, 1 aula").assertExists()
        compose.onNodeWithContentDescription("28 de dezembro, nenhuma aula").assertExists()
        tap("Dezembro 2026, escolher mês e ano")
        compose.onNodeWithContentDescription("Próximo ano").assertIsNotEnabled()
    }

    @Test
    @Config(qualifiers = "pt-rBR-w360dp-h2400dp")
    fun `AC-016-11 with Texto grande the month view fits 360 dp`() {
        runBlocking { h.services.saveSettings(h.services.currentSettings().copy(textScale = "large")) }
        month()
        val root = compose.onRoot().fetchSemanticsNode()
        val density = root.layoutInfo.density.density
        val grid = compose.onNodeWithTag("month-grid").fetchSemanticsNode().boundsInRoot
        assertTrue("the grid fits the screen", grid.right <= root.size.width + 0.5f)
        // Seven days a week, each still a comfortable target.
        val day = compose.onNodeWithContentDescription("26 de outubro, 1 aula").fetchSemanticsNode().boundsInRoot
        assertTrue("a day is ${day.width / density} dp wide", day.width / density >= 40f)
        compose.onNodeWithText("Ana Souza", useUnmergedTree = true).performScrollTo().assertExists()
    }
}
