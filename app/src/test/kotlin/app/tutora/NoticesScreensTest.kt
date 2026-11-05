package app.tutora

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.ui.NoticesHome
import app.tutora.usecases.LessonDraft
import app.tutora.usecases.ReminderKind
import app.tutora.usecases.Reminders
import app.tutora.usecases.Result
import app.tutora.usecases.WhoId
import kotlinx.coroutines.flow.first
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

// Now is Monday 26 Oct 2026, 09:00 in São Paulo; a class from Friday is waiting to be confirmed.
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class NoticesScreensTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val tmp = TemporaryFolder()
    private lateinit var h: Harness

    @Before
    fun setUp() {
        h = Harness(TestDb(tmp.root))
        runBlocking {
            val ana =
                (
                    h.t.catalog.saveStudent(
                        Student("", "Ana Souza", null, null, null, TagColor.AMBER, RateType.HOUR, null),
                        listOf(PlaceLink(BuiltinPlace.ONLINE.id, true, 12_000)),
                    ) as Result.Ok
                ).value.id
            h.t.schedule.scheduleLesson(
                LessonDraft(WhoId.OfStudent(ana), BuiltinPlace.ONLINE.id, LocalDate(2026, 10, 23), LocalTime(7, 0), 60, null, null, null),
            )
            Reminders(h.t.ctx).fire(ReminderKind.CONFIRM, null)
        }
    }

    @Test
    fun `AC-014-39 Notificacoes shows Agora and Recentes, each opening its page, and the cogwheel opens Lembretes`() {
        h.show(compose)
        h.go(compose, NoticesHome)
        compose.onNodeWithTag("notices-now").assertExists()
        // Once in Agora's alert, once in what was sent.
        compose.onAllNodesWithText("1 aula para confirmar", useUnmergedTree = true).assertCountEquals(2)
        compose.onNodeWithTag("notices-recent").assertExists()
        compose.onNodeWithText("Hoje, 09:00").assertExists()
        compose.onNodeWithText("Hoje, 09:00").performClick()
        h.settle(compose)
        compose.onNodeWithTag("confirm-screen").assertExists()
        h.go(compose, NoticesHome)
        compose.onNodeWithTag("notices-settings").performClick()
        h.settle(compose)
        compose.onNodeWithTag("reminders-screen").assertExists()
    }

    @Test
    fun `AC-014-40 the bell counts what is unseen, and opening Notificacoes marks it seen`() {
        h.show(compose)
        compose.onNodeWithTag("count", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("bell").performClick()
        h.awaitDb(compose) {
            h.services.notices
                .unseen()
                .first() == 0
        }
        assertEquals(
            0,
            runBlocking {
                h.services.notices
                    .unseen()
                    .first()
            },
        )
        compose.onNodeWithText("Nova", useUnmergedTree = true).assertExists()
    }
}
