package com.vmleroy.tutora.app

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vmleroy.tutora.app.ui.Tab
import com.vmleroy.tutora.app.ui.TutoraTabs
import com.vmleroy.tutora.designsystem.TutoraTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "pt-rBR")
class TabsTest {
    @get:Rule val compose = createComposeRule()

    private val info = AppInfo("1.1.0", "7")

    private fun show(nav: TestNavHostController? = null) {
        compose.setContent {
            TutoraTheme(dark = false) {
                if (nav != null) TutoraTabs(info, 1, nav = nav) else TutoraTabs(info, 1)
            }
        }
    }

    @Test
    fun `AC-001-1 the app opens on Hoje with five tabs in order`() {
        show()
        val labels = listOf("Hoje", "Agenda", "Cadastro", "Ganhos", "Ajustes")
        Tab.entries.forEachIndexed { i, tab ->
            compose.onNodeWithTag("tab-${tab.key}").assert(hasText(labels[i]))
        }
        assertEquals(listOf("today", "schedule", "registry", "earnings", "settings"), Tab.entries.map { it.key })
        compose.onNodeWithTag("tab-today").assertIsSelected()
        compose.onNodeWithText("Nenhuma aula hoje").assertExists()
    }

    @Test
    fun `AC-001-2 each tab opens its own stack, and switching never piles tabs up`() {
        val nav =
            TestNavHostController(ApplicationProvider.getApplicationContext()).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
        show(nav)
        for (tab in listOf("schedule", "registry", "earnings", "today", "schedule")) {
            compose.onNodeWithTag("tab-$tab").performClick()
            compose.onNodeWithTag("$tab-screen").assertExists()
        }
        // Only the start tab stays under the current one: tabs replace each other.
        val graphs =
            nav.currentBackStack.value
                .mapNotNull { it.destination.parent?.route }
                .distinct()
        assertEquals(2, graphs.size)
    }

    @Test
    fun `AC-001-3 Ajustes is the fifth tab and shows Sobre`() {
        show()
        compose.onNodeWithTag("tab-settings").performClick()
        compose.onNodeWithTag("settings-screen").assertExists()
        compose.onNodeWithText("Sobre").assertExists()
    }

    @Test
    fun `AC-001-22 Sobre shows the version, the build and the schema version`() {
        show()
        compose.onNodeWithTag("tab-settings").performClick()
        compose.onNodeWithText("Versão 1.1.0 · build 7 · banco v1").assertExists()
    }
}
