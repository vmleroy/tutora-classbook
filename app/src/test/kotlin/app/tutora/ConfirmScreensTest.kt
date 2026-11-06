package app.tutora

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.LessonStatus
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.ui.ConfirmClasses
import app.tutora.ui.ScheduleGraph
import app.tutora.usecases.LessonDraft
import app.tutora.usecases.Result
import app.tutora.usecases.ScheduleResult
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

// Classes on Monday 26 Oct 2026 in São Paulo; now moves to 15:30 that day, after the 14:00 class.
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class ConfirmScreensTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val tmp = TemporaryFolder()
    private lateinit var h: Harness
    private lateinit var ana: String
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
    }

    private fun lesson(hour: Int): String =
        runBlocking {
            val r =
                h.t.schedule.scheduleLesson(
                    LessonDraft(WhoId.OfStudent(ana), online, LocalDate(2026, 10, 26), LocalTime(hour, 0), 60, null, null, null),
                )
            (r as ScheduleResult.Ok).lessonId ?: ""
        }

    private fun afternoon() {
        h.t.now = "2026-10-26T18:30:00.000Z" // 15:30 in São Paulo
    }

    private fun plain(s: String) = s.replace(' ', ' ').replace(' ', ' ')

    private fun hasPlainText(text: String) =
        SemanticsMatcher("text is $text") { n -> n.config.getOrNull(SemanticsProperties.Text)?.any { plain(it.text) == text } == true }

    private fun count(text: String) = compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun `AC-014-3 a class to confirm is marked A confirmar on Hoje and the Agenda, apart from the class ahead`() {
        lesson(14)
        lesson(16)
        afternoon()
        h.show(compose)
        assertEquals(1, count("A confirmar"))
        h.go(compose, ScheduleGraph)
        compose.onNodeWithText("Dia").performClick()
        h.settle(compose)
        assertEquals(1, count("A confirmar"))
    }

    @Test
    fun `AC-014-4 Hoje counts the classes to confirm, opens their list, and the Agenda tab shows the number`() {
        lesson(10)
        lesson(14)
        afternoon()
        h.show(compose)
        compose.onNodeWithText("2 aulas para confirmar").assertExists()
        compose.onNodeWithTag("tab-count-schedule", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("2 aulas para confirmar").performClick()
        h.settle(compose)
        compose.onNodeWithTag("confirm-screen").assertExists()
    }

    @Test
    fun `AC-019-13 the Agenda says what its tab's number counts, and opens the list`() {
        lesson(10)
        lesson(14)
        afternoon()
        h.show(compose)
        compose.onNodeWithContentDescription("Agenda, 2 aulas para confirmar").assertExists()
        h.go(compose, ScheduleGraph)
        compose.onNode(hasText("2 aulas para confirmar") and hasAnyAncestor(hasTestTag("schedule-screen"))).performClick()
        h.settle(compose)
        compose.onNodeWithTag("confirm-screen").assertExists()
    }

    @Test
    fun `AC-014-5 on Mes a day holding a class to confirm has a ring`() {
        lesson(14)
        afternoon()
        h.show(compose)
        h.go(compose, ScheduleGraph)
        compose.onNodeWithText("Mês").performClick()
        h.settle(compose)
        assertEquals(1, compose.onAllNodesWithTag("month-pending", useUnmergedTree = true).fetchSemanticsNodes().size)
    }

    @Test
    fun `AC-014-6 the list shows every class to confirm, oldest first, with who, value and Dada, Cancelada and Faltou`() {
        lesson(14)
        lesson(10)
        afternoon()
        h.show(compose)
        h.go(compose, ConfirmClasses)
        val rows = compose.onAllNodes(hasTestTag("confirm-row")).fetchSemanticsNodes()
        assertEquals(2, rows.size)
        val first = compose.onAllNodes(hasTestTag("confirm-row"))[0]
        first.assertExistsWith("10:00")
        assertEquals(2, count("Ana Souza"))
        assertEquals(2, count("Dada"))
        assertEquals(2, count("Cancelada"))
        assertEquals(2, count("Faltou"))
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertExistsWith(text: String) {
        val texts = mutableListOf<String>()

        fun walk(n: androidx.compose.ui.semantics.SemanticsNode) {
            n.config.getOrNull(SemanticsProperties.Text)?.forEach { texts += plain(it.text) }
            n.children.forEach(::walk)
        }
        walk(fetchSemanticsNode())
        assertTrue("$texts", texts.any { it == text })
    }

    @Test
    fun `AC-014-9 Confirmar todas asks with the count and total, marks them done, and Desfazer puts them back`() {
        val ids = listOf(lesson(10), lesson(14))
        afternoon()
        h.show(compose)
        h.go(compose, ConfirmClasses)
        compose.onNodeWithText("Confirmar todas como dadas").performClick()
        h.settle(compose)
        compose.onNodeWithText("Marcar 2 aulas como dadas?").assertExists()
        compose.onNode(hasPlainText("R$ 240,00 entram nos ganhos.")).assertExists()
        compose.onNodeWithText("Marcar como dadas").performClick()
        h.awaitDb(compose) {
            ids.all {
                h.services.scheduleRepo
                    .lesson(it)
                    .first()
                    ?.status == LessonStatus.DONE
            }
        }
        h.settle(compose)
        compose.onNodeWithText("2 aulas marcadas como dadas").assertExists()
        compose.onNodeWithText("Desfazer").performClick()
        h.awaitDb(compose) {
            ids.all {
                h.services.scheduleRepo
                    .lesson(it)
                    .first()
                    ?.status == LessonStatus.SCHEDULED
            }
        }
        assertTrue(
            runBlocking {
                ids.all {
                    h.services.scheduleRepo
                        .lesson(it)
                        .first()
                        ?.status == LessonStatus.SCHEDULED
                }
            },
        )
    }

    @Test
    @Config(qualifiers = "en-w411dp-h2400dp")
    fun `AC-014-24 the alert, the marks and the list follow the app language, with plurals`() {
        lesson(14)
        afternoon()
        h.show(compose)
        compose.onNodeWithText("1 class to confirm").assertExists()
        assertEquals(1, count("To confirm"))
        lesson(16)
        h.t.now = "2026-10-26T20:00:00.000Z"
        h.go(compose, ConfirmClasses)
        compose.onNode(hasText("Classes to confirm")).assertExists()
    }
}
