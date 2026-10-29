package app.tutora

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Student
import app.tutora.domain.TagColor
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

// Spec 016, Hoje as Victor chose it on 28 Oct 2026. Now is Monday 26 Oct 2026, 09:00 in São Paulo.
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class HojeLookTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val tmp = TemporaryFolder()
    private lateinit var h: Harness

    @Before
    fun setUp() {
        h = Harness(TestDb(tmp.root))
        val online = BuiltinPlace.ONLINE.id
        runBlocking {
            val ana =
                (
                    h.t.catalog.saveStudent(
                        Student("", "Ana Souza", "Matemática", null, null, TagColor.AMBER, RateType.HOUR, null),
                        listOf(PlaceLink(online, true, 12_000)),
                    ) as Result.Ok
                ).value.id
            h.t.schedule.scheduleLesson(
                LessonDraft(WhoId.OfStudent(ana), online, LocalDate(2026, 10, 26), LocalTime(14, 0), 60, null, null, null),
            )
        }
    }

    private fun bounds(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    @Test
    fun `AC-016-1 Hoje's date and greeting sit on the Topo band, with the earnings card right under it`() {
        h.show(compose)
        val band = bounds("today-band")
        val card = bounds("today-earnings")
        assertTrue("the greeting is on the band", bounds("screen-header").bottom <= band.bottom + 0.5f)
        assertEquals("the card starts where the band's header ends", band.bottom, card.top, 0.5f)
    }

    @Test
    fun `AC-016-2 Hoje's earnings card shows its stats in equal, aligned columns`() {
        h.show(compose)
        val stats = compose.onAllNodesWithTag("summary-stat", useUnmergedTree = true).fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals(3, stats.size)
        stats.forEach {
            // Equal to a pixel of rounding.
            assertEquals(stats[0].width, it.width, 1.5f)
            assertEquals(stats[0].top, it.top, 0.5f)
        }
    }

    @Test
    fun `AC-016-5 Marcar como dada sits beside Iniciar aula, the same size`() {
        runBlocking { h.services.saveSettings(h.services.currentSettings().copy(earlyDone = true)) }
        h.show(compose)
        val mark = compose.onNodeWithText("Marcar como dada", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val start = compose.onNodeWithText("Iniciar aula", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals("one row", mark.center.y, start.center.y, 1f)
        assertTrue("Marcar como dada first", mark.right < start.left)
    }
}
