package app.tutora.app

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.app.ui.Tab
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "pt-rBR-w411dp-h891dp")
class TabsTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val tmp = TemporaryFolder()
    private lateinit var h: Harness

    @Before
    fun setUp() {
        h = Harness(TestDb(tmp.root))
    }

    @Test
    fun `AC-001-1 the app opens on Hoje with five tabs in order`() {
        h.show(compose)
        val labels = listOf("Hoje", "Agenda", "Cadastro", "Ganhos", "Ajustes")
        Tab.entries.forEachIndexed { i, tab -> compose.onNodeWithTag("tab-${tab.key}").assert(hasText(labels[i])) }
        assertEquals(listOf("today", "schedule", "registry", "earnings", "settings"), Tab.entries.map { it.key })
        compose.onNodeWithTag("tab-today").assertIsSelected()
        compose.onNodeWithText("Nenhuma aula hoje").assertExists()
    }

    @Test
    fun `AC-001-2 each tab opens its own stack, and switching never piles tabs up`() {
        h.show(compose)
        for (tab in listOf("schedule", "registry", "earnings", "today", "schedule")) {
            compose.onNodeWithTag("tab-$tab").performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("$tab-screen").assertExists()
        }
        val graphs =
            h.nav.currentBackStack.value
                .mapNotNull { it.destination.parent?.route }
                .distinct()
        assertEquals(2, graphs.size)
    }

    @Test
    fun `AC-001-3 Ajustes is the fifth tab and shows Sobre`() {
        h.show(compose)
        compose.onNodeWithTag("tab-settings").performClick()
        compose.onNodeWithTag("settings-screen").assertExists()
        compose.onNodeWithText("Sobre").assertExists()
    }

    @Test
    fun `AC-001-22 Sobre shows the version, the build and the schema version`() {
        h.show(compose)
        compose.onNodeWithTag("tab-settings").performClick()
        compose.onNodeWithText("Versão 1.1.0 · build 7 · banco v1").assertExists()
    }

    @Test
    fun `AC-005-27 Cadastro holds Alunos and Locais as sub-pages`() {
        h.show(compose)
        compose.onNodeWithTag("tab-registry").performClick()
        compose.onNodeWithTag("students-screen").assertExists()
        compose.onNodeWithText("Locais").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("places-screen").assertExists()
        compose.onNodeWithText("Casa do Aluno").assertExists()
    }
}
