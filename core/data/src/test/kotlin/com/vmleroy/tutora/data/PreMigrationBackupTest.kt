package com.vmleroy.tutora.data

import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PreMigrationBackupTest {
    private val driver = AndroidSQLiteDriver()
    private val backup = PreMigrationBackup(driver)

    @get:Rule val tmp = TemporaryFolder()

    private fun databaseAt(
        version: Int,
        language: String,
    ): File {
        val file = File(tmp.root, "tutora.db")
        val c = driver.open(file.absolutePath)
        c.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        c.execSQL("INSERT INTO settings VALUES ('language', '\"$language\"')")
        c.execSQL("PRAGMA user_version = $version")
        c.close()
        return file
    }

    private fun languageIn(file: File): String {
        val c = driver.open(file.absolutePath)
        return c
            .prepare("SELECT value FROM settings WHERE key = 'language'")
            .use {
                it.step()
                it.getText(0)
            }.also { c.close() }
    }

    @Test
    fun `AC-001-13 an older database is copied to tutora-pre-v{n}db before migrating, and kept on retry`() {
        val file = databaseAt(1, "en")
        val copy = backup.copyIfOlder(file, target = 2)
        assertEquals(File(tmp.root, "tutora-pre-v2.db").absolutePath, copy)
        assertEquals("\"en\"", languageIn(File(copy!!)))
        assertEquals(1, backup.versionOf(File(copy)))
        // A retry after a failed upgrade keeps the first copy.
        driver.open(file.absolutePath).also {
            it.execSQL("UPDATE settings SET value = '\"pt-BR\"'")
            it.close()
        }
        backup.copyIfOlder(file, target = 2)
        assertEquals("\"en\"", languageIn(File(copy)))
    }

    @Test
    fun `AC-001-13 a fresh or up-to-date database makes no copy`() {
        assertEquals(null, backup.copyIfOlder(File(tmp.root, "none.db"), target = 2))
        val file = databaseAt(2, "en")
        assertEquals(null, backup.copyIfOlder(file, target = 2))
        assertFalse(File(tmp.root, "tutora-pre-v2.db").exists())
    }
}
