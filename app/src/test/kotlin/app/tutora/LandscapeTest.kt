package app.tutora

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.toSize
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.designsystem.tokens.TutoraTokens.Size
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.ui.EarningsGraph
import app.tutora.ui.LiveLesson
import app.tutora.ui.RegistryGraph
import app.tutora.ui.ScheduleGraph
import app.tutora.ui.SettingsGraph
import app.tutora.ui.StudentEdit
import app.tutora.usecases.LessonDraft
import app.tutora.usecases.Result
import app.tutora.usecases.ScheduleResult
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

// Spec 017: a phone 800 × 360 dp held sideways. Now is Monday 26 Oct 2026, 09:00 in São Paulo.
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "pt-rBR-w800dp-h360dp-land")
class LandscapeTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val tmp = TemporaryFolder()
    private lateinit var h: Harness
    private lateinit var ana: String
    private lateinit var today: String
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
                ).value.id
            }
        today = schedule(LocalDate(2026, 10, 26), LocalTime(14, 0))
        // Classes given earlier this month, so Ganhos has bars and De onde veio.
        runBlocking {
            listOf(5, 12).forEach {
                h.t.lessons.logLesson(draft(LocalDate(2026, 10, it), LocalTime(8, 0)))
            }
        }
    }

    private fun draft(
        day: LocalDate,
        time: LocalTime,
    ) = LessonDraft(WhoId.OfStudent(ana), online, day, time, 60, null, null, null)

    private fun schedule(
        day: LocalDate,
        time: LocalTime,
    ): String = runBlocking { (h.t.schedule.scheduleLesson(draft(day, time)) as ScheduleResult.Ok).lessonId ?: "" }

    private fun bounds(tag: String): Rect =
        compose
            .onAllNodesWithTag(tag, useUnmergedTree = true)
            .onFirst()
            .fetchSemanticsNode()
            .boundsInRoot

    /** Unclipped, so a node below the fold counts too. */
    private fun full(tag: String): Rect =
        compose.onAllNodesWithTag(tag, useUnmergedTree = true).onFirst().fetchSemanticsNode().let {
            Rect(it.positionInRoot, it.size.toSize())
        }

    private fun textBounds(text: String): Rect =
        compose
            .onAllNodes(
                SemanticsMatcher("text is $text") { n ->
                    n.config.getOrNull(SemanticsProperties.Text)?.any { it.text.replace(' ', ' ') == text } == true
                },
                useUnmergedTree = true,
            ).onFirst()
            .fetchSemanticsNode()
            .boundsInRoot

    private fun root(): Rect =
        compose
            .onAllNodes(isRoot())
            .fetchSemanticsNodes()
            .first()
            .boundsInRoot

    private val dp get() = compose.density.density

    /** At most the readable width and centered in the screen beside the rail (AC-017-4). */
    private fun assertReadable(tag: String) {
        val b = bounds(tag)
        assertTrue("$tag is ${b.width / dp} dp wide", b.width / dp <= Size.contentMax.value + 1f)
        val rail = bounds("nav-rail")
        val left = b.left - rail.right
        val right = root().right - b.right
        assertTrue("$tag is centered ($left, $right)", kotlin.math.abs(left - right) <= Spacing.s5.value * dp * 2 + 2f)
    }

    /** [tag]'s page is open in the panel over the right side, the screen still under it. */
    private fun assertInPanel(tag: String) {
        val panel = bounds("side-panel")
        assertTrue("the panel is on the right", panel.right >= root().right - 1f && panel.left > root().width / 3)
        compose.onNode(hasTestTag(tag) and hasAnyAncestor(hasTestTag("side-panel"))).assertExists()
    }

    private fun tab(key: String) {
        compose.onNodeWithTag("tab-$key").performClick()
        h.settle(compose)
    }

    @Test
    fun `AC-017-1 turning the phone keeps the tab, the screen, an open sheet and what was typed`() {
        h.landscape = false
        h.show(compose)
        tab("registry")
        compose.onNode(hasSetTextAction()).performTextInput("Ana")
        h.settle(compose)
        listOf(true, false, true).forEach { side ->
            h.landscape = side
            h.settle(compose)
            compose.onNodeWithTag("registry-screen").assertExists()
            compose.onNodeWithTag("tab-registry").assertIsSelected()
            compose.onNode(hasSetTextAction()).assertTextContains("Ana")
        }
        // An open sheet stays open through a turn: the Agenda's month picker.
        tab("schedule")
        compose.onNodeWithTag("header-title").performClick()
        h.settle(compose)
        compose.onNodeWithText("2026").assertExists()
        h.landscape = false
        h.settle(compose)
        compose.onNodeWithText("2026").assertExists()
        compose.onNodeWithTag("schedule-screen").assertExists()
    }

    @Test
    fun `AC-017-2 sideways the tabs stand in a rail on the left and the screen takes the rest`() {
        h.show(compose)
        compose.onNodeWithTag("tab-bar").assertDoesNotExist()
        val rail = bounds("nav-rail")
        assertEquals(0f, rail.left, 0.5f)
        assertTrue(bounds("today-screen").left >= rail.right - 0.5f)
        listOf("today", "schedule", "registry", "earnings", "settings").forEach { compose.onNodeWithTag("tab-$it").assertExists() }
        compose.onNodeWithTag("tab-today").assertIsSelected()
        // Upright the bar comes back at the bottom.
        h.landscape = false
        h.settle(compose)
        compose.onNodeWithTag("nav-rail").assertDoesNotExist()
        compose.onNodeWithTag("tab-bar").assertExists()
    }

    @Test
    fun `AC-017-3 nothing is cut off sideways on a 360 dp phone with Muito grande text and Ampliada interface`() {
        runBlocking { h.services.saveSettings(h.services.currentSettings().copy(textScale = "xlarge", uiScale = "large")) }
        h.show(compose)
        compose.assertFitsSideways("Hoje")
        h.go(compose, ScheduleGraph)
        compose.assertFitsSideways("Agenda Dia")
        listOf("Semana", "Mês").forEach {
            compose.onNodeWithText(it).performClick()
            h.settle(compose)
            compose.assertFitsSideways("Agenda $it")
        }
        h.go(compose, RegistryGraph)
        compose.assertFitsSideways("Cadastro")
        h.go(compose, EarningsGraph)
        compose.assertFitsSideways("Ganhos")
        h.go(compose, SettingsGraph)
        compose.assertFitsSideways("Ajustes")
        h.go(compose, StudentEdit())
        compose.assertFitsSideways("Novo aluno")
    }

    @Test
    fun `AC-017-6 Hoje sideways keeps one column at a readable width, and a class opens in the panel over it`() {
        h.show(compose)
        assertReadable("today-earnings")
        compose.onNodeWithTag("side-panel").assertDoesNotExist()
        compose
            .onAllNodes(hasText("Ana Souza") and hasClickAction())
            .onLast()
            .performScrollTo()
            .performClick()
        h.settle(compose)
        assertInPanel("lesson-detail")
        compose.onNodeWithTag("today-screen").assertExists()
    }

    @Test
    fun `AC-017-7 Ganhos sideways keeps one column at a readable width, and a source opens in the panel`() {
        h.show(compose)
        tab("earnings")
        assertReadable("earn-summary")
        assertTrue("the bars are under the card", full("earn-bars").top >= full("earn-summary").bottom)
        compose.onNode(hasText("Ana Souza") and hasClickAction()).performScrollTo().performClick()
        h.settle(compose)
        assertInPanel("student-detail")
        compose.onNodeWithTag("earnings-screen").assertExists()
    }

    @Test
    fun `AC-017-8 Dia sideways shows the week strip over the day's hours at the full width, opened at the first class`() {
        h.show(compose)
        tab("schedule")
        val grid = bounds("day-grid")
        val screen = bounds("schedule-screen")
        assertTrue("the hours use the width: ${grid.width} of ${screen.width}", grid.width >= screen.width * 0.8f)
        assertTrue("the strip is above", bounds("week-strip").bottom <= grid.top)
        // The class at 14:00 is in view where the grid opens.
        compose
            .onAllNodes(
                SemanticsMatcher("Ana") { n ->
                    n.config.getOrNull(SemanticsProperties.Text)?.any { it.text == "Ana Souza" } == true
                },
                useUnmergedTree = true,
            ).onLast()
            .assertIsDisplayed()
    }

    @Test
    fun `AC-017-9 Semana sideways is the list of days at a readable width`() {
        h.show(compose)
        tab("schedule")
        compose.onNodeWithText("Semana").performClick()
        h.settle(compose)
        val list = bounds("week-list")
        val screen = bounds("schedule-screen")
        assertTrue("at most the readable width", list.width / dp <= Size.contentMax.value + 1f)
        assertEquals("centered", list.left - screen.left, screen.right - list.right, 2f)
        compose.onNodeWithTag("week-strip").assertExists()
    }

    @Test
    fun `AC-017-10 Mes sideways keeps the grid at a readable width, and a day opens its classes in the panel`() {
        h.show(compose)
        tab("schedule")
        compose.onNodeWithText("Mês").performClick()
        h.settle(compose)
        assertReadable("month-grid")
        compose.onNodeWithContentDescription("26 de outubro, 1 aula").performScrollTo().performClick()
        h.settle(compose)
        assertInPanel("agenda-day")
        compose.onNode(hasText("Ana Souza") and hasAnyAncestor(hasTestTag("side-panel")), useUnmergedTree = true).assertExists()
        compose.onNode(hasText("Ana Souza") and hasClickAction() and hasAnyAncestor(hasTestTag("side-panel"))).performClick()
        h.settle(compose)
        assertInPanel("lesson-detail")
    }

    @Test
    fun `AC-017-11 Cadastro sideways opens a student in the panel over the list, Back closes it first, upright shows it full screen`() {
        h.show(compose)
        tab("registry")
        assertReadable("students-screen")
        compose.onNodeWithText("Ana Souza").performClick()
        h.settle(compose)
        assertInPanel("student-detail")
        compose.onNodeWithTag("side-panel-scrim").assertExists()
        compose.onNodeWithTag("registry-screen").assertExists()
        Espresso.pressBack()
        h.settle(compose)
        compose.onNodeWithTag("side-panel").assertDoesNotExist()
        compose.onNodeWithTag("registry-screen").assertExists()
        compose.onNodeWithTag("tab-registry").assertIsSelected()
        // Turned upright with the panel open, the page shows full screen.
        compose.onNodeWithText("Ana Souza").performClick()
        h.settle(compose)
        h.landscape = false
        h.settle(compose)
        compose.onNodeWithTag("side-panel").assertDoesNotExist()
        compose.onNodeWithTag("student-detail").assertExists()
        compose.onNodeWithTag("registry-screen").assertDoesNotExist()
    }

    @Test
    fun `AC-017-12 the live class sideways gives the clock the left side and keeps who, the value, the note and its buttons in view`() {
        runBlocking { h.t.live.start(today) }
        h.t.now = "2026-10-26T12:20:00.000Z"
        h.show(compose)
        h.go(compose, LiveLesson(today))
        val clockSide = bounds("live-clock-side")
        val info = bounds("live-info-side")
        assertTrue("the clock side is on the left", clockSide.right <= info.left + 0.5f)
        assertEquals("the clock side runs the phone's height", root().height, clockSide.height, 1f)
        val time = textBounds("00:20:00")
        assertTrue("the time is on the clock side", time.right <= clockSide.right)
        listOf("Ana Souza", "Ganho até agora", "Anotação rápida", "Pausar", "Encerrar aula").forEach {
            val b = textBounds(it)
            assertTrue("$it is on the right", b.left >= info.left - 0.5f)
            assertTrue("$it is in view", b.bottom <= root().bottom + 0.5f)
        }
        compose.onNodeWithText("Ao vivo").assertExists()
    }

    @Test
    fun `AC-017-14 a form keeps one column at the readable width, and the field typed in comes into view`() {
        h.show(compose)
        h.go(compose, StudentEdit())
        val form = bounds("student-edit")
        // Unclipped: fields below the fold count too.
        val fields =
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map {
                Rect(it.positionInRoot, it.size.toSize())
            }
        assertTrue(fields.size > 1)
        // One column at the readable width, centered: every field sits inside it.
        val side = (form.width - Size.contentMax.value * dp) / 2
        assertTrue(side > 0f)
        fields.forEach {
            assertTrue("a field starts at ${it.left}", it.left >= form.left + side - 1f)
            assertTrue("a field ends at ${it.right}", it.right <= form.right - side + 1f)
        }
        val last = compose.onAllNodes(hasSetTextAction()).onLast()
        last.performSemanticsAction(SemanticsActions.RequestFocus)
        h.settle(compose)
        last.assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en-w800dp-h360dp-land")
    fun `AC-017-16 the landscape layouts read the same in English`() {
        runBlocking { h.services.saveSettings(h.services.currentSettings().copy(language = app.tutora.domain.Language.EN)) }
        h.show(compose)
        compose.onNodeWithTag("nav-rail").assertExists()
        compose.assertFitsSideways("Today")
        tab("registry")
        textBounds("Directory")
        compose.onNodeWithText("Ana Souza").performClick()
        h.settle(compose)
        assertInPanel("student-detail")
        compose.assertFitsSideways("Directory with a student open")
    }
}
