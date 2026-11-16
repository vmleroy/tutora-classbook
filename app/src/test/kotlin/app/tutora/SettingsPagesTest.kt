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
    fun `AC-014-28 AC-021-20 Ajustes lists its pages with what is set, and each page holds what belongs to it`() {
        h.show(compose)
        h.go(compose, SettingsGraph)
        // Only the pages, each with its line; your work first, then the app, then Sobre (Victor, 14 Nov 2026).
        listOf(
            "Seus dados",
            "Aulas",
            "Calendário",
            "Pagamentos",
            "Relatórios",
            "Lembretes",
            "Geral",
            "Backup e planilha",
            "Privacidade",
            "Sobre",
        ).forEach {
            compose.onNodeWithText(it).performScrollTo().assertExists()
        }
        compose.onNodeWithText("Português").assertExists()
        compose.onNodeWithText("Créditos e bônus desligados").assertExists()
        compose.onNodeWithText("Idioma do app").assertDoesNotExist()
        // Calendário is a page of its own, right on Ajustes.
        compose.onNodeWithTag("calendar-row").performScrollTo().assertExists()
        // Geral is how the app shows things: Idioma, Aparência and Durações em HH:MM.
        open("page-general")
        compose.onNodeWithText("Idioma do app").performScrollTo().assertExists()
        compose.onNodeWithTag("appearance-row").performScrollTo().assertExists()
        compose.onNodeWithText("Durações em HH:MM").performScrollTo().assertExists()
        listOf("Como você dá aula", "Mostrar locais padrão").forEach { compose.onNodeWithText(it).assertDoesNotExist() }
        back()
        // Aulas holds what is about classes.
        open("page-lessons")
        listOf("Como você dá aula", "Mostrar locais padrão").forEach {
            compose.onNodeWithText(it).performScrollTo().assertExists()
        }
        compose.onNodeWithText("Durações em HH:MM").assertDoesNotExist()
        compose.onNodeWithContentDescription("Aula ao vivo").performScrollTo().assertExists()
        compose.onNodeWithText("Aulas passadas").performScrollTo().assertExists()
        compose.onNodeWithText("Pedir confirmação").performScrollTo().assertExists()
        back()
        // Pagamentos: Créditos e bônus and the Pix key with its city.
        open("page-payments")
        compose.onNodeWithContentDescription("Créditos e bônus").assertExists()
        compose.onNodeWithTag("pix-key").performScrollTo().assertExists()
        compose.onNodeWithTag("pix-city").performScrollTo().assertExists()
        back()
        // Relatórios: the title and what the PDF shows, on a page of their own.
        open("page-reports")
        compose.onNodeWithTag("report-title").performScrollTo().assertExists()
        compose.onNodeWithText("O que o relatório mostra").performScrollTo().assertExists()
        back()
        open("page-about")
        compose.onNodeWithTag("settings-about").assertExists()
    }

    @Test
    fun `AC-020-18 Pagamentos has an optional Cidade, used in the Pix QR code`() {
        h.show(compose)
        h.go(compose, app.tutora.ui.SettingsPayments)
        compose.onNodeWithText("Usada no QR code do Pix").performScrollTo().assertExists()
        compose.onNodeWithContentDescription("Cidade").performTextReplacement("Recife")
        h.awaitDb(compose) { h.services.currentSettings().pixCity == "Recife" }
    }

    @Test
    fun `AC-020-28 Seus dados and the Pix section in Pagamentos each say what they are for`() {
        h.show(compose)
        h.go(compose, app.tutora.ui.PersonalData)
        compose.onNodeWithTag("section-you").performScrollTo().assertExists()
        compose.onNodeWithText("Seu nome vai no topo dos relatórios e no fim do texto copiado.").performScrollTo().assertExists()
        listOf("section-pix", "section-reports").forEach { compose.onNodeWithTag(it).assertDoesNotExist() }
        h.go(compose, app.tutora.ui.SettingsPayments)
        compose.onNodeWithTag("section-pix").performScrollTo().assertExists()
        listOf(
            "Para receber: a chave vai inteira no relatório. Com seu nome e a cidade, ele ganha também um QR code com o valor.",
        ).forEach { compose.onNodeWithText(it).performScrollTo().assertExists() }
    }

    @Test
    fun `AC-021-18 Ajustes › Relatorios sets which parts start on, Pix and Pagamentos at first, and Lembrar a ultima escolha`() {
        h.show(compose)
        h.go(compose, app.tutora.ui.SettingsReports)
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
