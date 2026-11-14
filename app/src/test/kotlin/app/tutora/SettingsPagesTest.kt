package app.tutora

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isToggleable
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

    @Test
    fun `AC-020-28 Seus dados is in three sections, each saying what it is for`() {
        h.show(compose)
        h.go(compose, app.tutora.ui.PersonalData)
        listOf("section-you", "section-pix", "section-reports").forEach { compose.onNodeWithTag(it).performScrollTo().assertExists() }
        listOf(
            "Seu nome vai no topo dos relatórios e no fim do texto copiado.",
            "Para receber: a chave vai inteira no relatório. Com seu nome e a cidade, ele ganha também um QR code com o valor.",
            "A linha no topo de cada PDF. Vazia, fica a do Tutora.",
        ).forEach { compose.onNodeWithText(it).performScrollTo().assertExists() }
    }

    @Test
    fun `AC-021-18 Seus dados sets which parts start on, Pix and Pagamentos at first, and Lembrar a ultima escolha`() {
        h.show(compose)
        h.go(compose, app.tutora.ui.PersonalData)
        compose.onNodeWithText("O que o relatório mostra").performScrollTo().assertExists()

        fun switch(tag: String) = compose.onNode(isToggleable() and hasAnyAncestor(hasTestTag(tag)))
        switch("default-pix").performScrollTo().assertIsOn()
        switch("default-payments").performScrollTo().assertIsOn()
        switch("default-details").performScrollTo().assertIsOff()
        switch("doc-remember").performScrollTo().assertIsOff()
        switch("default-details").performScrollTo().performClick()
        h.awaitDb(compose) {
            h.services
                .currentSettings()
                .docParts.details
        }
        switch("doc-remember").performScrollTo().performClick()
        h.awaitDb(compose) { h.services.currentSettings().docRemember }
        org.junit.Assert.assertEquals(
            app.tutora.domain.DocParts(details = true),
            kotlinx.coroutines.runBlocking { h.services.currentSettings().docParts },
        )
    }
}
