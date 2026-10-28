package app.tutora.data

import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class MigrationsTest {
    @get:Rule val tmp = TemporaryFolder()

    private val schemas = File("schemas/app.tutora.data.TutoraDatabase")

    @Test
    fun `AC-001-15 every schema version is exported and every step has a migration`() {
        for (v in 1..TutoraDatabase.VERSION) assertTrue("schema $v.json is exported", File(schemas, "$v.json").exists())
        val steps = TutoraDatabase.MIGRATIONS.map { it.startVersion to it.endVersion }
        assertEquals((1 until TutoraDatabase.VERSION).map { it to it + 1 }, steps)
        // Each later migration adds a test here that opens a database seeded at its start
        // version and checks the data survives.
    }

    /** A database file at [version], built from that version's exported schema. */
    private fun createAt(
        version: Int,
        file: File,
        seed: List<String>,
    ) {
        val db = JSONObject(File(schemas, "$version.json").readText()).getJSONObject("database")
        val sql = mutableListOf<String>()
        val entities = db.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val e = entities.getJSONObject(i)
            val table = e.getString("tableName")
            val name = { q: String -> q.replace("${'$'}{TABLE_NAME}", table) }
            sql += name(e.getString("createSql"))
            val indices = e.optJSONArray("indices") ?: continue
            for (j in 0 until indices.length()) sql += name(indices.getJSONObject(j).getString("createSql"))
        }
        val setup = db.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) sql += setup.getString(i)
        AndroidSQLiteDriver().open(file.absolutePath).let { c ->
            try {
                (sql + seed + "PRAGMA user_version = $version").forEach { c.execSQL(it) }
            } finally {
                c.close()
            }
        }
    }

    @Test
    fun `migration 1 to 2 keeps lessons and students, with no charge or reason`() =
        runTest {
            val file = File(tmp.root, "tutora.db")
            val t = "'2026-10-26T12:00:00.000Z'"
            createAt(
                1,
                file,
                listOf(
                    "INSERT INTO place VALUES ('p', 'ONLINE', NULL, NULL, 'online', 'STUDENT', 'HOUR', 12000, 0, 'LESSON', NULL, 60, 'NONE', 0, NULL, $t, $t)",
                    "INSERT INTO student VALUES ('s', 'Ana', NULL, NULL, NULL, 'AMBER', 'HOUR', NULL, $t, $t)",
                    "INSERT INTO lesson VALUES ('l', NULL, NULL, 's', NULL, 'p', $t, 60, 'scheduled', NULL, NULL, 0, 'nota', $t, $t)",
                ),
            )
            val (db, backup) = openTutoraDatabase(ApplicationProvider.getApplicationContext(), file, AndroidSQLiteDriver())
            assertTrue(backup != null)
            val lesson = db.schedule().lesson("l")
            assertEquals("nota", lesson?.notes)
            assertEquals(null, lesson?.feeCents)
            assertEquals(null, lesson?.cancelReason)
            assertEquals("Ana", db.students().get("s")?.name)
            db.close()
        }

    @Test
    fun `migration 2 to 3 keeps a cancelled class and adds the live clock`() =
        runTest {
            val file = File(tmp.root, "tutora.db")
            val t = "'2026-10-26T12:00:00.000Z'"
            createAt(
                2,
                file,
                listOf(
                    "INSERT INTO place VALUES ('p', 'ONLINE', NULL, NULL, 'online', 'STUDENT', 'HOUR', 12000, 0, 'LESSON', NULL, 60, 'NONE', 0, NULL, $t, $t)",
                    "INSERT INTO student VALUES ('s', 'Ana', NULL, NULL, NULL, 'AMBER', 'HOUR', NULL, $t, $t)",
                    "INSERT INTO lesson (id, student_id, place_id, start_at, planned_min, status, is_detached, created_at, updated_at, " +
                        "fee_cents, cancel_reason) VALUES ('l', 's', 'p', $t, 60, 'cancelled', 0, $t, $t, 6000, 'student')",
                ),
            )
            val (db, _) = openTutoraDatabase(ApplicationProvider.getApplicationContext(), file, AndroidSQLiteDriver())
            assertEquals(6000L, db.schedule().lesson("l")?.feeCents)
            db.schedule().upsertTimer(TimerRow("l", "2026-10-26T12:00:00.000Z", 0, null, null, null))
            assertEquals("2026-10-26T12:00:00.000Z", db.schedule().timer("l")?.startedAt)
            db.close()
        }
}
