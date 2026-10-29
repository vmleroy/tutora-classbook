package app.tutora

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.domain.ThemeMode
import app.tutora.domain.theme.CustomPart
import app.tutora.ui.AppearanceHome
import app.tutora.ui.CustomColors
import app.tutora.ui.SettingsGraph
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
class AppearanceScreensTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val tmp = TemporaryFolder()
    private lateinit var h: Harness

    @Before
    fun setUp() {
        h = Harness(TestDb(tmp.root))
    }

    private fun exists(text: String) =
        compose
            .onNodeWithText(text, substring = true)
            .performScrollTo()
            .assertExists()

    private fun click(text: String) {
        compose.onNodeWithText(text).performScrollTo().performClick()
        h.settle(compose)
    }

    /** The size a text is laid out at. */
    private fun fontSize(text: String): Float {
        val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose
            .onNodeWithText(text)
            .fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(results)
        return results
            .first()
            .layoutInput.style.fontSize.value
    }

    private fun settings() = runBlocking { h.services.currentSettings() }

    private fun page() {
        h.show(compose)
        h.go(compose, AppearanceHome)
    }

    @Test
    fun `AC-013-1 Ajustes has Aparencia in Geral, and its page has every part`() {
        h.show(compose)
        h.go(compose, SettingsGraph)
        exists("Quadro-negro · Sistema")
        compose.onNodeWithTag("appearance-row").performScrollTo().performClick()
        h.settle(compose)
        listOf("Modo", "Tema", "Personalizar", "Cor principal", "Cor de destaque", "Tamanho do texto", "Cores avançadas").forEach(::exists)
    }

    @Test
    fun `AC-013-2 the preview follows a choice at once`() {
        page()
        compose.onNodeWithTag("appearance-preview-quadro-0").assertExists()
        compose.onNodeWithContentDescription("Vinho").performScrollTo().performClick()
        h.awaitDb(compose) { settings().customColors.isNotEmpty() }
        h.settle(compose)
        compose.onNodeWithTag("appearance-preview-quadro-3").assertExists()
    }

    @Test
    fun `AC-013-4 Tema lists the four themes and choosing one repaints the app`() {
        page()
        listOf("Quadro-negro", "Caderno azul", "Biblioteca", "Giz pastel").forEach(::exists)
        click("Biblioteca")
        h.awaitDb(compose) { settings().themeId == "biblioteca" }
        assertEquals("biblioteca", settings().themeId)
    }

    @Test
    fun `AC-013-6 switching away from unsaved colors asks, and Salvar e trocar keeps them as a theme`() {
        runBlocking { h.services.appearance.setPart(CustomPart.HEADER, "#3a3f47") }
        page()
        click("Caderno azul")
        compose.onNodeWithText("Suas cores não estão salvas. Salvar como tema antes de trocar?").assertExists()
        compose.onNodeWithText("Salvar e trocar").performClick()
        h.awaitDb(compose) { settings().themeId == "caderno" }
        val s = settings()
        assertEquals(listOf("Meu tema"), s.savedThemes.map { it.name })
        assertTrue(s.customColors.isEmpty())
    }

    @Test
    fun `AC-013-7 the swatches are read by their names, with Do tema first and a plus`() {
        page()
        listOf("Do tema", "Verde quadro", "Terracota", "Amarelo lápis", "Dourado").forEach {
            compose
                .onAllNodes(
                    androidx.compose.ui.test
                        .hasContentDescription(it),
                ).onFirst()
                .assertExists()
        }
        compose
            .onAllNodes(
                androidx.compose.ui.test
                    .hasContentDescription("Mais cores"),
            ).onFirst()
            .performScrollTo()
            .performClick()
        h.settle(compose)
        compose.onNodeWithTag("custom-colors-screen").assertExists()
    }

    @Test
    fun `AC-013-10 Grande makes the text larger at once`() {
        page()
        val before = fontSize("Cores avançadas")
        click("Grande")
        h.awaitDb(compose) { settings().textScale == "large" }
        h.settle(compose)
        val after = fontSize("Cores avançadas")
        assertTrue("$before -> $after", after > before)
    }

    @Test
    fun `AC-013-11 Suas cores shows the preview, the seven parts and the chosen part's controls`() {
        h.show(compose)
        h.go(compose, CustomColors(CustomPart.HIGHLIGHT_BTN.key))
        compose.onNodeWithTag("appearance-preview-quadro-0").assertExists()
        listOf("Topo", "Botões principais", "Botão de destaque", "Números de ganhos", "Fundo", "Cartões", "Menu ativo").forEach {
            compose.onAllNodesWithText(it).onFirst().assertExists()
        }
        listOf("Botão de destaque · código HEX", "Matiz", "Intensidade", "Brilho").forEach(::exists)
    }

    @Test
    fun `AC-013-12 a typed hex code sets the color, a half-typed one changes nothing`() {
        h.show(compose)
        h.go(compose, CustomColors(CustomPart.PRIMARY_BTN.key))
        compose.onNodeWithContentDescription("Botões principais · código HEX").performTextReplacement("7a2e3")
        h.settle(compose)
        assertTrue(settings().customColors.isEmpty())
        compose.onNodeWithContentDescription("Botões principais · código HEX").performTextReplacement("7a2e3b")
        h.awaitDb(compose) { settings().customColors.isNotEmpty() }
        assertEquals("#7a2e3b", settings().customColors[CustomPart.PRIMARY_BTN])
        exists("Texto branco escolhido · contraste")
    }

    @Test
    fun `AC-013-14 in dark mode Suas cores says the color is shown lighter`() {
        runBlocking { h.services.appearance.setMode(ThemeMode.DARK) }
        h.show(compose)
        h.go(compose, CustomColors(CustomPart.PRIMARY_BTN.key))
        exists("No modo escuro, esta cor fica mais clara")
    }

    @Test
    fun `AC-013-15 a change shows at once, and leaving with hard-to-read text asks first`() {
        h.show(compose)
        h.go(compose, CustomColors(CustomPart.EARNINGS.key))
        compose.onNodeWithContentDescription("Números de ganhos · código HEX").performTextReplacement("fff8e0")
        h.awaitDb(compose) { settings().customColors.isNotEmpty() }
        h.settle(compose)
        exists("difícil de ler")
        compose.onNodeWithContentDescription("Voltar").performClick()
        h.settle(compose)
        compose.onNodeWithText("Algum texto pode ficar difícil de ler. Salvar assim?").assertExists()
    }

    @Test
    @Config(qualifiers = "en-w411dp-h2400dp")
    fun `AC-013-18 both pages follow the app language`() {
        page()
        listOf("Mode", "Theme", "Chalkboard", "Text size", "Advanced colors").forEach(::exists)
        h.go(compose, CustomColors(CustomPart.PRIMARY_BTN.key))
        listOf("Choose what to paint", "Hue", "Brightness", "White text chosen · contrast").forEach(::exists)
    }
}
