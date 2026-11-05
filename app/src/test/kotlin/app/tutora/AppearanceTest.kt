package app.tutora

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.data.BackupStore
import app.tutora.data.SettingsRepository
import app.tutora.domain.BackupRead
import app.tutora.domain.Language
import app.tutora.domain.ThemeMode
import app.tutora.domain.theme.CustomPart
import app.tutora.domain.theme.quickMain
import app.tutora.usecases.Appearance
import app.tutora.usecases.Backups
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppearanceTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var t: TestDb
    private lateinit var a: Appearance
    private lateinit var repo: SettingsRepository

    @Before
    fun setUp() =
        runTest {
            t = TestDb(tmp.root)
            a = Appearance(t.ctx)
            repo = SettingsRepository(t.db)
            repo.ensureDefaults(Language.PT_BR, t.now)
            BackupStore(t.db).ensureDefaults(t.now)
        }

    @Test
    fun `AC-013-3 Modo starts on Sistema and changes to Claro or Escuro`() =
        runTest {
            assertEquals(ThemeMode.SYSTEM, repo.load().themeMode)
            a.setMode(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, repo.load().themeMode)
        }

    @Test
    fun `AC-013-5 colors are saved as a theme of one's own, renamed, chosen back, deleted, and kept through a backup`() =
        runTest {
            a.chooseTheme("biblioteca")
            a.setQuick(quickMain, "#2F5C9E")
            val id = a.saveAsTheme("Escola")
            a.renameSaved(id, "Escola Azul")
            a.chooseTheme("giz")
            a.chooseSaved(id)
            val s = repo.load()
            assertEquals("biblioteca", s.themeId)
            assertEquals("#2f5c9e", s.customColors[CustomPart.PRIMARY_BTN])
            assertEquals(listOf("Escola Azul"), s.savedThemes.map { it.name })
            assertEquals(id, s.savedThemeId)
            // A backup carries it: restored after being deleted.
            val backups = Backups(t.ctx)
            val file = (backups.inspect(backups.makeBackup().second) as BackupRead.Ok).file
            a.deleteSaved(id)
            assertTrue(repo.load().savedThemes.isEmpty())
            backups.restore(file)
            assertEquals(listOf("Escola Azul"), repo.load().savedThemes.map { it.name })
        }

    @Test
    fun `AC-013-6 choosing another theme starts with no custom colors`() =
        runTest {
            a.setPart(CustomPart.CARDS, "#fff8e0")
            a.chooseTheme("caderno")
            val s = repo.load()
            assertEquals("caderno", s.themeId)
            assertTrue(s.customColors.isEmpty())
            assertNull(s.savedThemeId)
        }

    @Test
    fun `AC-013-9 Restaurar tema removes every custom color`() =
        runTest {
            a.setQuick(quickMain, "#7A2E3B")
            a.setPart(CustomPart.EARNINGS, "#5b4a8b")
            a.restore()
            assertTrue(repo.load().customColors.isEmpty())
        }

    @Test
    fun `AC-013-16 Restaurar in Suas cores takes every part back, and a saved theme stays saved`() =
        runTest {
            a.setPart(CustomPart.HEADER, "#3a3f47")
            a.saveAsTheme("Grafite")
            a.restore()
            val s = repo.load()
            assertTrue(s.customColors.isEmpty())
            assertNull(s.savedThemeId)
            assertEquals(1, s.savedThemes.size)
        }
}
