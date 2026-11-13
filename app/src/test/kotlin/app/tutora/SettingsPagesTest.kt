package app.tutora

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.ui.SettingsGraph
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class SettingsPagesTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val tmp = TemporaryFolder()
    private lateinit var h: Harness

    @Before
    fun setUp() {
        h = Harness(TestDb(tmp.root))
    }

    private fun open(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo().performClick()
        h.settle(compose)
    }

    private fun back() {
        compose.runOnIdle { h.nav.popBackStack() }
        h.settle(compose)
    }

    @Test
    fun `AC-014-28 Ajustes lists its pages with what is set, and each page holds its settings`() {
        h.show(compose)
        h.go(compose, SettingsGraph)
        // Only the pages, each with its line; no setting sits on Ajustes itself.
        listOf("Seus dados", "Backup e planilha", "Geral", "Aulas", "Lembretes", "Pagamentos", "Sobre").forEach {
            compose.onNodeWithText(it).assertExists()
        }
        compose.onNodeWithText("Português").assertExists()
        compose.onNodeWithText("Créditos e bônus desligados").assertExists()
        compose.onNodeWithText("Idioma do app").assertDoesNotExist()
        open("page-general")
        listOf("Idioma do app", "Como você dá aula", "Mostrar locais padrão", "Durações em HH:MM").forEach {
            compose.onNodeWithText(it).performScrollTo().assertExists()
        }
        compose.onNodeWithTag("appearance-row").performScrollTo().assertExists()
        compose.onNodeWithTag("calendar-row").performScrollTo().assertExists()
        back()
        open("page-lessons")
        compose.onNodeWithContentDescription("Aula ao vivo").assertExists()
        compose.onNodeWithText("Aulas passadas").performScrollTo().assertExists()
        compose.onNodeWithText("Pedir confirmação").performScrollTo().assertExists()
        back()
        open("page-payments")
        compose.onNodeWithContentDescription("Créditos e bônus").assertExists()
        back()
        open("page-about")
        compose.onNodeWithTag("settings-about").assertExists()
    }

    @Test
    fun `AC-020-18 Seus dados has an optional Cidade, used in the Pix QR code`() {
        h.show(compose)
        h.go(compose, app.tutora.ui.PersonalData)
        compose.onNodeWithText("Usada no QR code do Pix").performScrollTo().assertExists()
        compose.onNodeWithContentDescription("Cidade").performTextReplacement("Recife")
        h.awaitDb(compose) { h.services.currentSettings().pixCity == "Recife" }
    }
}
