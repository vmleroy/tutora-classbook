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
import app.tutora.domain.FeeChoice
import app.tutora.domain.FeeRule
import app.tutora.domain.Language
import app.tutora.domain.LessonStatus
import app.tutora.domain.PlaceLink
import app.tutora.domain.RateType
import app.tutora.domain.Settings
import app.tutora.domain.Student
import app.tutora.domain.TagColor
import app.tutora.ui.LessonDetail
import app.tutora.ui.LessonEdit
import app.tutora.ui.LessonNew
import app.tutora.ui.SettingsGraph
import app.tutora.ui.StudentEdit
import app.tutora.usecases.LessonChange
import app.tutora.usecases.LessonDraft
import app.tutora.usecases.Result
import app.tutora.usecases.ScheduleResult
import app.tutora.usecases.Scope
import app.tutora.usecases.StatusChange
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

    private fun status(
        id: String,
        change: StatusChange,
    ) = runBlocking { h.t.schedule.editLesson(id, LessonChange(status = change), Scope.ONE) }

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
    fun `AC-006-9 Situacao offers the four statuses with a line on what each means`() {
        val id = lesson(LocalTime(8, 0))
        h.show(compose)
        h.go(compose, LessonEdit(id))
        listOf("Agendada", "Dada", "Cancelada", "Faltou").forEach { exists(it) }
        exists("Conta no previsto da semana.")
        compose.onNodeWithText("Dada").performClick()
        h.settle(compose)
        exists("Entra no ganho e no saldo do aluno.")
    }

    @Test
    fun `AC-006-14 the detail shows the status, a kept fee, and who pays`() {
        val ahead = lesson(LocalTime(14, 0))
        val missed = lesson(LocalTime(8, 0))
        runBlocking { h.services.saveSettings(missRule()) }
        status(missed, StatusChange(LessonStatus.MISSED))
        h.show(compose)
        h.go(compose, LessonDetail(ahead))
        exists("Agendada · em 5h")
        exists("Quem paga")
        exists("O aluno")
        compose.runOnIdle { h.nav.popBackStack() }
        h.go(compose, LessonDetail(missed))
        exists("Faltou · taxa R$ 120,00")
    }

    @Test
    fun `AC-006-15 Hoje marks a done class and a missed one`() {
        val done = lesson(LocalTime(7, 0))
        val missed = lesson(LocalTime(8, 0))
        status(done, StatusChange(LessonStatus.DONE))
        status(missed, StatusChange(LessonStatus.MISSED))
        h.show(compose)
        exists("Dada")
        exists("Faltou")
    }

    @Test
    fun `AC-006-18 Ajustes opens the cancellation rule with its example, and Salvar regra saves it`() {
        h.show(compose)
        h.go(compose, SettingsGraph)
        compose.onNode(hasContentDescription("Cancelamentos e faltas", substring = true)).performClick()
        h.settle(compose)
        exists("Cancelou em cima da hora")
        exists("Faltou sem avisar")
        exists("Cancelou às 11:00 (3h antes): sem taxa")
        compose.onNodeWithContentDescription("Cancelou em cima da hora").performClick()
        h.settle(compose)
        listOf("2h", "12h", "24h", "48h", "50%", "100%", "Valor fixo").forEach { exists(it) }
        exists("Cancelou às 11:00 (3h antes): taxa de R$ 60,00")
        compose.onNodeWithText("Salvar regra").performClick()
        h.awaitDb(compose) {
            h.services
                .currentSettings()
                .feeRule.lateOn
        }
    }

    @Test
    fun `AC-006-20 a student follows Padrao, has its own rule or is never charged`() {
        h.show(compose)
        h.go(compose, StudentEdit(ana))
        exists("Padrão")
        exists("Própria")
        exists("Não cobra cancelamentos nem faltas")
        compose.onNodeWithText("Não cobrar").performClick()
        h.settle(compose)
        exists("Nunca cobra cancelamentos nem faltas")
        compose.onNodeWithText("Salvar").performClick()
        h.awaitDb(compose) {
            h.services.catalogRepo
                .student(ana)
                .first()
                ?.student
                ?.feeChoice == FeeChoice.Never
        }
    }

    @Test
    fun `AC-006-23 Faltou shows Cobrar a falta with the fee and why, and off it reads Dispensado`() {
        runBlocking { h.services.saveSettings(missRule()) }
        val id = lesson(LocalTime(8, 0))
        h.show(compose)
        h.go(compose, LessonEdit(id))
        compose.onNodeWithText("Faltou").performClick()
        h.settle(compose)
        exists("Cobrar a falta")
        exists("R$ 120,00 · 100%")
        compose.onNodeWithContentDescription("Cobrar a falta").performClick()
        h.settle(compose)
        exists("Dispensado desta vez: não cobra nada")
    }

    @Test
    @Config(qualifiers = "en-w411dp-h2400dp")
    fun `AC-006-28 statuses and fees follow the language and keep their amounts`() {
        runBlocking { h.services.saveSettings(missRule().copy(language = Language.EN)) }
        val id = lesson(LocalTime(8, 0))
        status(id, StatusChange(LessonStatus.MISSED))
        h.show(compose)
        h.go(compose, LessonDetail(id))
        exists("No-show · fee R$120.00")
    }

    /** Settings whose rule charges no-shows 100%. */
    private fun missRule() = Settings(Language.PT_BR, feeRule = FeeRule(missOn = true))
}
