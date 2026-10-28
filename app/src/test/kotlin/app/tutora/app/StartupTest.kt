package app.tutora.app

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.app.ui.StartupError
import app.tutora.app.ui.TutoraApp
import app.tutora.app.usecases.StartApp
import app.tutora.app.usecases.StartResult
import app.tutora.data.SettingsRepository
import app.tutora.data.openTutoraDatabase
import app.tutora.designsystem.TutoraTheme
import app.tutora.domain.Language
import app.tutora.domain.Settings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "pt-rBR")
class StartupTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val tmp = TemporaryFolder()

    private val file get() = File(tmp.root, "tutora.db")

    private fun startApp(phone: String) =
        StartApp(
            openDatabase = { openTutoraDatabase(ApplicationProvider.getApplicationContext(), file, AndroidSQLiteDriver()) },
            phoneTags = { listOf(phone) },
            now = { "2026-10-27T12:00:00.000Z" },
            newId = {
                java.util.UUID
                    .randomUUID()
                    .toString()
            },
        )

    @Test
    fun `AC-001-5 a first run takes the language from the phone`() =
        runTest {
            val result = startApp("pt-BR")() as StartResult.Ready
            assertEquals(Language.PT_BR, result.settings.language)
            result.db.close()
        }

    @Test
    fun `AC-001-6 after that the saved language rules, whatever the phone`() =
        runTest {
            val first = startApp("pt-BR")() as StartResult.Ready
            SettingsRepository(first.db).save(Settings(language = Language.EN), "2026-10-27T12:00:00.000Z")
            first.db.close()
            val again = startApp("pt-BR")() as StartResult.Ready
            assertEquals(Language.EN, again.settings.language)
            again.db.close()
        }

    @Test
    fun `AC-001-4 nothing is drawn until start-up is done, so nothing flashes`() {
        compose.setContent { TutoraApp(AppState.Loading, AppInfo("1.1.0", "1")) }
        compose.onNodeWithTag("tab-today").assertDoesNotExist()
        compose.onNodeWithTag("startup-error").assertDoesNotExist()
    }

    @Test
    fun `AC-001-14 a failed migration shows the error screen with the copy's path, not the tabs`() =
        runTest {
            // A database from a newer app cannot be opened by this one.
            AndroidSQLiteDriver().open(file.absolutePath).apply {
                prepare("PRAGMA user_version = 99").use { it.step() }
                close()
            }
            val result = startApp("pt-BR")()
            assertTrue(result is StartResult.Failed)
            compose.setContent {
                TutoraTheme(
                    dark = false,
                ) { StartupError("/data/tutora-pre-v2.db", (result as StartResult.Failed).message) }
            }
            compose.onNodeWithText("Não foi possível atualizar seus dados").assertExists()
            compose.onNodeWithText("Cópia salva em: /data/tutora-pre-v2.db").assertExists()
            compose.onNodeWithTag("tab-today").assertDoesNotExist()
        }
}
