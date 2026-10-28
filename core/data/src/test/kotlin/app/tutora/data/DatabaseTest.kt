package app.tutora.data

import androidx.room.useReaderConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tutora.domain.Language
import app.tutora.domain.Settings
import app.tutora.domain.ThemeMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun open(file: File = File(tmp.root, "tutora.db")) =
        openTutoraDatabase(ApplicationProvider.getApplicationContext(), file, AndroidSQLiteDriver())

    @Test
    fun `AC-001-12 a fresh database has WAL, foreign keys and default settings`() =
        runTest {
            val (db, backup) = open()
            val repo = SettingsRepository(db)
            repo.ensureDefaults(Language.PT_BR, NOW)
            assertEquals(
                Settings(language = Language.PT_BR, themeId = "quadro", themeMode = ThemeMode.SYSTEM, setupDone = false),
                repo.load(),
            )
            assertEquals(null, backup)
            db.useReaderConnection { c ->
                assertEquals(
                    "wal",
                    c.usePrepared("PRAGMA journal_mode") {
                        it.step()
                        it.getText(0)
                    },
                )
                assertEquals(
                    1L,
                    c.usePrepared("PRAGMA foreign_keys") {
                        it.step()
                        it.getLong(0)
                    },
                )
            }
            db.close()
        }

    @Test
    fun `AC-001-12 defaults never overwrite saved settings`() =
        runTest {
            val (db, _) = open()
            val repo = SettingsRepository(db)
            repo.save(Settings(language = Language.EN), NOW)
            repo.ensureDefaults(Language.PT_BR, NOW)
            assertEquals(Language.EN, repo.load().language)
            db.close()
        }

    companion object {
        const val NOW = "2026-10-27T12:00:00.000Z"
    }
}
