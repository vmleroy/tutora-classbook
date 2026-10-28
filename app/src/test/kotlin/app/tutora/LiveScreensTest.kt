package app.tutora

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.domain.pause
import app.tutora.domain.startTimer
import app.tutora.live.AndroidLiveNotifier
import app.tutora.ui.LessonDetail
import app.tutora.ui.LiveLesson
import app.tutora.ui.SettingsGraph
import app.tutora.usecases.LessonDraft
import app.tutora.usecases.LiveNote
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config

// Now is Monday 26 Oct 2026, 09:00 in São Paulo.
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class LiveScreensTest {
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

    private fun lesson(time: LocalTime = LocalTime(14, 0)): String =
        runBlocking {
            val r =
                h.t.schedule.scheduleLesson(
                    LessonDraft(WhoId.OfStudent(ana), online, LocalDate(2026, 10, 26), time, 60, null, null, null),
                )
            (r as ScheduleResult.Ok).lessonId ?: ""
        }

    private fun liveFor(minutes: Int): String {
        val id = lesson(LocalTime(8, 30))
        runBlocking { h.t.live.start(id) }
        h.t.now = "2026-10-26T12:%02d:00.000Z".format(minutes)
        return id
    }

    private fun plain(s: String) = s.replace(' ', ' ').replace(' ', ' ')

    private fun hasPlainText(text: String) =
        SemanticsMatcher("text is $text") { n -> n.config.getOrNull(SemanticsProperties.Text)?.any { plain(it.text) == text } == true }

    private fun exists(text: String) = compose.onNode(hasPlainText(text), useUnmergedTree = true).assertExists()

    private fun absent(text: String) = compose.onNode(hasPlainText(text), useUnmergedTree = true).assertDoesNotExist()

    @Test
    fun `AC-007-1 Iniciar aula is on the class's detail, and on Proxima aula when the class is today`() {
        val id = lesson()
        h.show(compose)
        exists("Próxima aula · em 5h")
        exists("Iniciar aula")
        h.go(compose, LessonDetail(id))
        exists("Iniciar aula")
    }

    @Test
    fun `AC-007-1 Proxima aula on another day has no Iniciar aula`() {
        runBlocking {
            h.t.schedule.scheduleLesson(
                LessonDraft(WhoId.OfStudent(ana), online, LocalDate(2026, 10, 27), LocalTime(14, 0), 60, null, null, null),
            )
        }
        h.show(compose)
        exists("Próxima aula · Ter, 27 out, 14:00")
        absent("Iniciar aula")
    }

    @Test
    fun `AC-007-3 with Aula ao vivo off, Iniciar aula is offered nowhere`() {
        val id = lesson()
        runBlocking { h.services.saveSettings(h.services.currentSettings().copy(liveTimer = false)) }
        h.show(compose)
        absent("Iniciar aula")
        h.go(compose, LessonDetail(id))
        absent("Iniciar aula")
        exists("Cancelar aula")
    }

    @Test
    fun `AC-007-4 the live screen shows the status, who, the clock toward the planned time and its buttons`() {
        val id = liveFor(23)
        h.show(compose)
        h.go(compose, LiveLesson(id))
        compose.onNodeWithTag("live-screen").assertExists()
        listOf(
            "Ao vivo",
            "Ana Souza",
            "00:23:00",
            "de 60 min planejados",
            "Pausar",
            "Encerrar aula",
            "Descartar",
            "Anotação rápida",
        ).forEach {
            exists(it)
        }
        // Per hour, the money earned ticks with the clock (AC-007-5).
        exists("Ganho até agora")
        exists("R$ 46,00")
        exists("R$ 120,00 por hora · R$ 2,00 por minuto")
    }

    @Test
    fun `AC-007-13 a live class shows at the top of Hoje and its detail opens the live screen`() {
        val id = liveFor(23)
        h.show(compose)
        exists("Ao vivo · Ana Souza · 00:23:00")
        h.go(compose, LessonDetail(id))
        compose.onNode(hasPlainText("Abrir aula ao vivo")).performClick()
        h.settle(compose)
        compose.onNodeWithTag("live-screen").assertExists()
    }

    @Test
    fun `AC-007-15 the notification shows who, the running clock, Pausar and Encerrar, and paused it shows the time`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val notifier = AndroidLiveNotifier(app)
        val timer = startTimer("l", "2026-10-26T12:00:00.000Z")
        notifier.show(LiveNote("l", "Ana Souza", timer, "2026-10-26T12:23:41.000Z"))
        val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val running = Shadows.shadowOf(manager).allNotifications.single()
        assertEquals("Ana Souza", running.extras.getString(NotificationCompat.EXTRA_TITLE))
        assertTrue(running.extras.getBoolean(NotificationCompat.EXTRA_SHOW_CHRONOMETER))
        assertEquals(listOf("Pausar", "Encerrar aula"), running.actions.map { it.title.toString() })
        assertTrue(running.flags and android.app.Notification.FLAG_ONGOING_EVENT != 0)
        notifier.show(LiveNote("l", "Ana Souza", pause(timer, "2026-10-26T12:23:41.000Z"), "2026-10-26T13:00:00.000Z"))
        val paused = Shadows.shadowOf(manager).allNotifications.single()
        assertEquals("Pausada · 00:23:41", paused.extras.getCharSequence(NotificationCompat.EXTRA_TEXT).toString())
        assertEquals(listOf("Retomar", "Encerrar aula"), paused.actions.map { it.title.toString() })
        notifier.hide()
        assertTrue(Shadows.shadowOf(manager).allNotifications.isEmpty())
    }

    @Test
    fun `AC-007-17 Ajustes has Aulas with Aula ao vivo, on by default`() {
        h.show(compose)
        h.go(compose, SettingsGraph)
        exists("Aulas")
        exists("Cronômetro e ganho ao vivo durante a aula")
        compose.onNodeWithContentDescription("Aula ao vivo").performClick()
        h.awaitDb(compose) { !h.services.currentSettings().liveTimer }
        assertEquals(false, runBlocking { h.services.currentSettings().liveTimer })
    }

    @Test
    @Config(qualifiers = "en-w411dp-h2400dp")
    fun `AC-007-18 the live screen follows the language and keeps the time and the value`() {
        val id = liveFor(23)
        h.show(compose)
        h.go(compose, LiveLesson(id))
        listOf("Live", "00:23:00", "of 60 min planned", "Earned so far", "R$46.00", "Pause", "End lesson").forEach { exists(it) }
    }
}
