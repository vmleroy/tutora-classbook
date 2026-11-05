package app.tutora

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
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
import org.junit.Assert.assertEquals
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

    /** Waits for the node read as [description]: a new month's classes come from the database's own thread. */
    private fun eventually(description: String) {
        repeat(50) {
            if (compose.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty()) return
            h.settle(compose)
        }
        compose.onNodeWithContentDescription(description).assertExists()
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
    fun `AC-016-13 the Agenda's card shows Agenda with the month and year it shows`() {
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
        listOf("Dia", "Semana", "Mês", "26 out – 1 nov de 2026").forEach { exists(it) }
        compose.onNodeWithText("Hoje · 1 aula", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `AC-016-31 Semana lists the days one under another, follows the strip, and the strip follows it`() {
        agenda()
        compose.onNodeWithText("Semana").performClick()
        h.settle(compose)
        compose.onNodeWithTag("week-list").assertExists()
        // Today first in view, with its class as a card: times, who and where, its value.
        compose.onNodeWithTag("week-day-2026-10-26").assertIsDisplayed()
        compose
            .onAllNodes(
                // Money is written with a no-break space after R$.
                hasText("Ana Souza", substring = true) and hasText("120,00", substring = true),
            ).onFirst()
            .assertExists()
        // Tapping Sunday on the strip scrolls the list to it.
        compose.onNodeWithContentDescription("Dom 1").performClick()
        h.settle(compose)
        compose.onNodeWithTag("week-day-2026-11-01").assertIsDisplayed()
        exists("Sem aulas")
    }

    @Test
    fun `AC-016-31 Semana never ends, loading the next weeks as the teacher scrolls`() {
        agenda()
        compose.onNodeWithText("Semana").performClick()
        h.settle(compose)
        // Today is 26 Oct: scrolling down loads week after week, past the stored days too.
        repeat(30) {
            compose.onNodeWithTag("week-list").performTouchInput { swipeUp() }
            compose.waitForIdle()
        }
        h.settle(compose)
        compose.onNodeWithTag("week-day-2026-12-31").assertDoesNotExist()
        // The strip followed the list: it no longer shows the first week.
        compose.onNodeWithText("26 out – 1 nov de 2026").assertDoesNotExist()
    }

    @Test
    fun `AC-016-33 the week's line names its dates with the year, across months and years too`() {
        val pt = app.tutora.domain.Language.PT_BR
        val en = app.tutora.domain.Language.EN
        assertEquals("23 – 29 de novembro de 2026", app.tutora.ui.weekRange(LocalDate(2026, 11, 23), LocalDate(2026, 11, 29), pt))
        assertEquals("30 nov – 6 dez de 2026", app.tutora.ui.weekRange(LocalDate(2026, 11, 30), LocalDate(2026, 12, 6), pt))
        assertEquals("28 dez 2026 – 3 jan 2027", app.tutora.ui.weekRange(LocalDate(2026, 12, 28), LocalDate(2027, 1, 3), pt))
        assertEquals("Dec 28, 2026 – Jan 3, 2027", app.tutora.ui.weekRange(LocalDate(2026, 12, 28), LocalDate(2027, 1, 3), en))
        agenda()
        exists("26 out – 1 nov de 2026")
    }

    @Test
    fun `AC-016-34 every day can be chosen, past the stored days too, in every view`() {
        agenda()
        // Today is 26 Oct; classes are stored to 31 Dec. › walks on into 2027.
        repeat(10) {
            compose.onNodeWithContentDescription("Próxima semana").performClick()
            h.settle(compose)
        }
        exists("4 – 10 de janeiro de 2027")
        compose.onNodeWithContentDescription("Ter 5").assertIsEnabled().performClick()
        h.settle(compose)
        compose.onNodeWithText("Semana").performClick()
        h.settle(compose)
        compose.onNodeWithTag("week-day-2027-01-05").assertExists()
        compose.onNodeWithText("Mês").performClick()
        h.settle(compose)
        exists("Janeiro 2027")
    }

    @Test
    fun `AC-016-35 past the stored days a series shows forecast classes, and one opens what it is`() {
        agenda()
        tap("Outubro 2026, escolher mês e ano")
        tap("Próximo ano")
        compose.onNodeWithContentDescription("Fevereiro 2027", substring = true).performClick()
        h.settle(compose)
        // Ana's weekly Monday class goes on as forecasts: a dot on each Monday.
        eventually("1 de fevereiro, 1 aula")
        compose.onNodeWithText("Semana").performClick()
        h.settle(compose)
        compose.onAllNodesWithText("Prevista", useUnmergedTree = true).onFirst().assertExists()
        compose.onAllNodes(hasText("Ana Souza", substring = true)).onFirst().performClick()
        h.settle(compose)
        compose.onNodeWithText("Aula prevista").assertExists()
        compose.onNodeWithText("Ver a série").assertExists()
    }

    @Test
    fun `AC-016-32 the shown day is filled on the strip and highlighted in the list, today outlined`() {
        agenda()
        compose.onNodeWithText("Semana").performClick()
        h.settle(compose)
        compose.onNodeWithContentDescription("Seg 26").assertIsSelected()
        compose.onNodeWithContentDescription("Qua 28").performClick()
        h.settle(compose)
        compose.onNodeWithContentDescription("Qua 28").assertIsSelected()
        compose.onNodeWithTag("week-day-2026-10-28").assertIsDisplayed()
    }

    @Test
    fun `AC-016-16 Mes shows the month from Monday, and a neighbor's day opens its month`() {
        month()
        compose.onNodeWithTag("month-grid").assertExists()
        compose.onNodeWithContentDescription("1 de outubro, nenhuma aula").assertIsEnabled()
        tap("28 de setembro, nenhuma aula")
        exists("Setembro 2026")
        compose.onNodeWithContentDescription("28 de setembro, nenhuma aula").assertIsSelected()
        tap("Próximo mês")
        exists("Outubro 2026")
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
        // The sheet counts each month's classes from the database; wait for November's.
        compose.waitUntil(5_000) {
            compose
                .onAllNodes(
                    androidx.compose.ui.test
                        .hasContentDescription("Novembro 2026, 5 aulas"),
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
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
    fun `AC-016-23 the Agenda goes past the stored days into any month`() {
        month()
        tap("Próximo mês")
        tap("Próximo mês")
        exists("Dezembro 2026")
        // Stored to 31 Dec: 28 Dec has its class; › goes on to January, with its forecast dots.
        eventually("28 de dezembro, 1 aula")
        compose.onNodeWithContentDescription("Próximo mês").assertIsEnabled()
        tap("Próximo mês")
        exists("Janeiro 2027")
        eventually("4 de janeiro, 1 aula")
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

    @Test
    fun `AC-016-29 swiping the month shows the next or previous one`() {
        // The gesture is checked on a device; here its two named actions, which run the same change.
        month()

        fun swipe(name: String) {
            compose
                .onNode(
                    androidx.compose.ui.test.SemanticsMatcher("has action $name") { n ->
                        n.config.getOrNull(androidx.compose.ui.semantics.SemanticsActions.CustomActions)?.any { it.label == name } == true
                    },
                ).fetchSemanticsNode()
                .config[androidx.compose.ui.semantics.SemanticsActions.CustomActions]
                .first { it.label == name }
                .action()
            h.settle(compose)
        }
        swipe("Próximo mês")
        exists("Novembro 2026")
        swipe("Mês anterior")
        exists("Outubro 2026")
        // It goes on past the stored days: October, then three months on.
        swipe("Próximo mês")
        swipe("Próximo mês")
        swipe("Próximo mês")
        exists("Janeiro 2027")
    }

    @Test
    fun `AC-016-30 every month shows six weeks, so the grid keeps its height`() {
        month()
        val october =
            compose
                .onNodeWithTag("month-grid")
                .fetchSemanticsNode()
                .size.height
        compose.onNodeWithContentDescription("Próximo mês").performClick()
        h.settle(compose)
        // November 2026 needs only five weeks; it still shows six.
        assertEquals(
            october,
            compose
                .onNodeWithTag("month-grid")
                .fetchSemanticsNode()
                .size.height,
        )
    }
}
