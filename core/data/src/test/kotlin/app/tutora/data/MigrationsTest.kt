package app.tutora.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MigrationsTest {
    private val schemas = File("schemas/app.tutora.data.TutoraDatabase")

    @Test
    fun `AC-001-15 every schema version is exported and every step has a migration`() {
        for (v in 1..TutoraDatabase.VERSION) assertTrue("schema $v.json is exported", File(schemas, "$v.json").exists())
        val steps = TutoraDatabase.MIGRATIONS.map { it.startVersion to it.endVersion }
        assertEquals((1 until TutoraDatabase.VERSION).map { it to it + 1 }, steps)
        // Each later migration adds a test here that opens a database seeded at its start
        // version (Room's MigrationTestHelper) and checks the data survives.
    }
}
