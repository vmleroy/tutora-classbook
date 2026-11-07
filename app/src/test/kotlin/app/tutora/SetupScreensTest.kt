package app.tutora

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.Language
import app.tutora.domain.PlaceKind
import app.tutora.domain.Teaching
import app.tutora.domain.builtinPlace
import app.tutora.ui.EarningsGraph
import app.tutora.ui.PlaceEdit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class SetupScreensTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val tmp = TemporaryFolder()
    private lateinit var h: Harness

    @Before
    fun setUp() {
        h = Harness(TestDb(tmp.root))
    }

    private fun firstRun() {
        runBlocking { h.services.saveSettings(h.services.currentSettings().copy(setupDone = false)) }
        h.show(compose)
    }

    private fun settings() = runBlocking { h.services.currentSettings() }

    private fun teaching(vararg t: Teaching) = runBlocking { h.services.saveSettings(settings().copy(teaching = t.toSet())) }

    private fun tap(text: String) {
        compose.onNodeWithText(text).performClick()
        h.settle(compose)
    }

    private fun plain(s: String) = s.replace('\u00A0', ' ').replace('\u202F', ' ')

    /** Text that starts with [start], with the money's narrow spaces read as plain ones. */
    private fun hasPlainText(start: String) =
        SemanticsMatcher("text starts with $start") { n ->
            n.config.getOrNull(SemanticsProperties.Text)?.any { plain(it.text).startsWith(start) } == true
        }

    @Test
    fun `AC-014-17 a first run with no data opens on the setup's four steps, each with Pular to the end`() {
        firstRun()
        compose.onNodeWithTag("setup-screen").assertExists()
        compose.onNodeWithText("Passo 1 de 4").assertExists()
        compose.onNodeWithTag("setup-next").performClick()
        h.settle(compose)
        compose.onNodeWithText("Passo 2 de 4").assertExists()
        compose.onNodeWithTag("setup-skip").performClick()
        h.settle(compose)
        compose.onNodeWithText("Passo 4 de 4").assertExists()
        compose.onNodeWithTag("setup-summary").assertExists()
    }

    @Test
    fun `AC-014-18 choosing English on step 1 changes the app language`() {
        firstRun()
        tap("English")
        h.awaitDb(compose) { h.services.currentSettings().language == Language.EN }
        assertEquals(Language.EN, settings().language)
    }

    @Test
    fun `AC-014-19 without school, Ganhos has no Escola e emprego filter and a new place offers no Escola`() {
        teaching(Teaching.PRIVATE, Teaching.SALINHA)
        h.show(compose)
        h.go(compose, EarningsGraph)
        compose.onNodeWithText("Escola e emprego").assertDoesNotExist()
        h.go(compose, PlaceEdit())
        compose.onNodeWithText("Escola").assertDoesNotExist()
        compose.onNodeWithText("Salinha").assertExists()
        teaching(Teaching.PRIVATE)
        h.settle(compose)
        compose.onNodeWithText("Salinha").assertDoesNotExist()
        compose.onNodeWithText("O local").assertDoesNotExist()
    }

    @Test
    fun `AC-014-20 Como voce da aula changes in Ajustes, and a kind already in use shows again`() {
        teaching(Teaching.PRIVATE)
        h.show(compose)
        h.go(compose, app.tutora.ui.SettingsGeneral)
        compose.onNodeWithTag("teaching-row").performScrollTo().performClick()
        h.settle(compose)
        compose.onNodeWithContentDescription("Escola ou emprego fixo").performClick()
        h.awaitDb(compose) { Teaching.SCHOOL in h.services.currentSettings().teaching }
        assertTrue(Teaching.SCHOOL in settings().teaching)
        teaching(Teaching.PRIVATE)
        runBlocking {
            h.t.catalog.savePlace(
                builtinPlace(BuiltinPlace.ONLINE).copy(id = "", builtin = null, name = "Colégio", kind = PlaceKind.SCHOOL),
            )
        }
        h.go(compose, PlaceEdit())
        compose.onNodeWithText("Escola").assertExists()
    }

    @Test
    fun `AC-014-22 step 4 turns on automatic backup with a folder and the reminder before class`() {
        h.picker.folderAnswer = "content://drive/tutora"
        firstRun()
        compose.onNodeWithTag("setup-skip").performClick()
        h.settle(compose)
        compose.onNodeWithContentDescription("Backup automático").performClick()
        h.awaitDb(compose) {
            h.services.backups
                .state()
                .auto
        }
        assertTrue(
            runBlocking {
                h.services.backups
                    .state()
                    .auto
            },
        )
        compose.onNodeWithText("Aviso antes da aula").assertExists()
    }

    @Test
    fun `AC-014-23 the end shows the summary and leads to the first student or to Hoje`() {
        firstRun()
        compose.onNodeWithTag("setup-skip").performClick()
        h.settle(compose)
        compose.onNode(hasPlainText("Português · Aulas particulares · Online / Remoto R$ 100,00/h")).assertExists()
        tap("Cadastrar meu primeiro aluno")
        h.awaitDb(compose) { h.services.currentSettings().setupDone }
        h.settle(compose)
        compose.onNodeWithTag("student-edit").assertExists()
        assertTrue(settings().setupDone)
        // Hoje is under the new student, not an empty stack.
        compose.runOnIdle { h.nav.popBackStack() }
        h.settle(compose)
        compose.onNodeWithTag("today-screen").assertExists()
    }

    @Test
    fun `AC-015-5 step 4 offers Bloquear o Tutora, asking the phone's unlock to turn it on`() {
        firstRun()
        compose.onNodeWithTag("setup-skip").performClick()
        h.settle(compose)
        compose.onNodeWithContentDescription("Bloquear o Tutora").performScrollTo().performClick()
        h.settle(compose)
        assertEquals(1, h.unlock.asks)
        tap("Ir para Hoje")
        h.awaitDb(compose) { h.services.currentSettings().setupDone }
        assertTrue(settings().appLock)
    }
}
