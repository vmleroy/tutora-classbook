package app.tutora

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.CancelReason
import app.tutora.domain.Charge
import app.tutora.domain.LessonStatus
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.ui.LessonDetail
import app.tutora.ui.LessonEdit
import app.tutora.ui.LessonNew
import app.tutora.ui.SettingsGraph
import app.tutora.ui.StudentEdit
import app.tutora.usecases.Cancellation
import app.tutora.usecases.LessonDraft
import app.tutora.usecases.Result
import app.tutora.usecases.ScheduleResult
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

// Now is Monday 26 Oct 2026, 09:00 in São Paulo.
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class LessonScreensTest {
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

    private fun lesson(time: LocalTime): String =
        runBlocking {
            val r =
                h.t.schedule.scheduleLesson(
                    LessonDraft(WhoId.OfStudent(ana), online, LocalDate(2026, 10, 26), time, 60, null, null, null),
                )
            (r as ScheduleResult.Ok).lessonId ?: ""
        }

    private fun cancel(
        id: String,
        reason: CancelReason,
        charge: Charge? = null,
    ) = runBlocking { h.t.lessons.cancel(id, Cancellation(reason, "viagem", charge)) }

    private suspend fun stored(id: String) =
        h.services.scheduleRepo
            .lesson(id)
            .first()

    private fun plain(s: String) = s.replace('\u00A0', ' ').replace('\u202F', ' ')

    private fun hasPlainText(text: String) =
        SemanticsMatcher("text is $text") { n -> n.config.getOrNull(SemanticsProperties.Text)?.any { plain(it.text) == text } == true }

    private fun exists(text: String) = compose.onNode(hasPlainText(text), useUnmergedTree = true).assertExists()

    @Test
    fun `AC-006-1 Marcar como dada opens a sheet with the real duration and Confirmar with the value`() {
        val id = lesson(LocalTime(8, 0))
        h.show(compose)
        h.go(compose, LessonDetail(id))
        compose.onNodeWithText("Marcar como dada").performClick()
        h.settle(compose)
        exists("Duração real")
        exists("Confirmar · R$ 120,00")
        compose.onNodeWithContentDescription("Aumentar").performClick()
        h.settle(compose)
        exists("Confirmar · R$ 130,00")
        compose.onNode(hasPlainText("Confirmar · R$ 130,00")).performClick()
        h.awaitDb(compose) { stored(id)?.status == LessonStatus.DONE }
        assertEquals(65, runBlocking { stored(id)?.plannedMin })
    }

    @Test
    fun `AC-006-5 Nova aula has Registrar aula dada and Agendar aula, and Registrar hides Repetir`() {
        h.show(compose)
        h.go(compose, LessonNew())
        exists("Registrar aula dada")
        exists("Agendar aula")
        exists("Repetir")
        compose.onNodeWithText("Registrar aula dada").performClick()
        h.settle(compose)
        compose.onNode(hasPlainText("Repetir"), useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `AC-006-7 Registrar shows how the value is worked out and saves with it on the button`() {
        h.show(compose)
        h.go(compose, LessonNew())
        compose.onNodeWithText("Registrar aula dada").performClick()
        compose.onNodeWithText("Escolher aluno ou turma").performClick()
        compose.waitForIdle()
        compose.onNode(hasContentDescription("Ana Souza", substring = true)).performClick()
        h.settle(compose)
        exists("60 min × R$ 120,00/h")
        exists("Registrar aula · R$ 120,00")
    }

    @Test
    fun `AC-006-9 a scheduled class has Cancelar aula, Marcar como dada once started, and the edit form no status`() {
        val ahead = lesson(LocalTime(14, 0))
        val started = lesson(LocalTime(8, 0))
        h.show(compose)
        h.go(compose, LessonDetail(ahead))
        exists("Cancelar aula")
        compose.onNode(hasPlainText("Marcar como dada"), useUnmergedTree = true).assertDoesNotExist()
        compose.runOnIdle { h.nav.popBackStack() }
        h.go(compose, LessonDetail(started))
        exists("Cancelar aula")
        exists("Marcar como dada")
        compose.runOnIdle { h.nav.popBackStack() }
        h.go(compose, LessonEdit(started))
        compose.onNode(hasPlainText("Cancelada"), useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `AC-006-14 the detail shows the status with its charge and reason, and who pays`() {
        val ahead = lesson(LocalTime(14, 0))
        val cancelled = lesson(LocalTime(8, 0))
        cancel(cancelled, CancelReason.STUDENT, Charge.Percent(5_000))
        h.show(compose)
        h.go(compose, LessonDetail(ahead))
        exists("Agendada · em 5h")
        exists("Quem paga")
        exists("O aluno")
        compose.runOnIdle { h.nav.popBackStack() }
        h.go(compose, LessonDetail(cancelled))
        exists("Cancelada pelo aluno · cobra R$ 60,00")
        exists("viagem")
        exists("Desfazer")
    }

    @Test
    fun `AC-006-15 Hoje marks a done class and a missed one`() {
        val done = lesson(LocalTime(7, 0))
        val missed = lesson(LocalTime(8, 0))
        runBlocking { h.t.lessons.markDone(done, 60) }
        cancel(missed, CancelReason.NO_SHOW)
        h.show(compose)
        exists("Dada")
        exists("Faltou")
    }

    @Test
    fun `AC-006-18 Cancelar aula opens a sheet with what happened, the reason and the charge toggle, and Voltar changes nothing`() {
        val id = lesson(LocalTime(8, 0))
        h.show(compose)
        h.go(compose, LessonDetail(id))
        compose.onNodeWithText("Cancelar aula").performClick()
        h.settle(compose)
        listOf(
            "O que aconteceu?",
            "Aluno cancelou",
            "Aluno faltou",
            "Eu cancelei",
            "Motivo (opcional)",
            "Cobrar esta aula?",
            "Voltar",
        ).forEach {
            exists(it)
        }
        compose.onNodeWithContentDescription("Cobrar esta aula?").performClick()
        h.settle(compose)
        listOf("50%", "100%", "Outro valor", "Cancelar aula · cobrar R$ 60,00").forEach { exists(it) }
        compose.onNodeWithText("Eu cancelei").performClick()
        h.settle(compose)
        compose.onNode(hasPlainText("Cobrar esta aula?"), useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Voltar").performClick()
        h.settle(compose)
        assertEquals(LessonStatus.SCHEDULED, runBlocking { stored(id)?.status })
    }

    @Test
    fun `AC-006-23 Ajustes and the student form have no cancellation rule`() {
        h.show(compose)
        h.go(compose, SettingsGraph)
        compose.onNode(hasPlainText("Cancelamentos e faltas"), useUnmergedTree = true).assertDoesNotExist()
        h.go(compose, StudentEdit(ana))
        compose.onNode(hasPlainText("Não cobrar"), useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "en-w411dp-h2400dp")
    fun `AC-006-24 statuses and charges follow the language and keep their amounts`() {
        val id = lesson(LocalTime(8, 0))
        cancel(id, CancelReason.NO_SHOW, Charge.Percent(10_000))
        h.show(compose)
        h.go(compose, LessonDetail(id))
        exists("No-show · charges R$120.00")
    }
}
