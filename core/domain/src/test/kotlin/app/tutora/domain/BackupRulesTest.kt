package app.tutora.domain

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

class BackupRulesTest {
    private val zone = "America/Sao_Paulo"

    private fun at(iso: String) = Instant.parse(iso)

    private val file =
        BackupFile(
            BACKUP_APP,
            5,
            "2026-10-25T23:00:00.000Z",
            mapOf(
                "student" to listOf(mapOf("id" to JsonPrimitive("s1"), "name" to JsonPrimitive("Ana"), "phone" to JsonNull)),
                "lesson" to listOf(mapOf("id" to JsonPrimitive("l1"), "planned_min" to JsonPrimitive(60))),
            ),
        )

    @Test
    fun `AC-011-3 a backup carries the app, the version of its data and when it was made, and reads back the same`() {
        val text = encodeBackup(file)
        assertTrue(text.startsWith("{\"app\":\"tutora\",\"schemaVersion\":5,\"exportedAt\":\"2026-10-25T23:00:00.000Z\""))
        assertEquals(BackupRead.Ok(file), readBackup(text.encodeToByteArray(), 5))
    }

    @Test
    fun `AC-011-6 automatic backups run at 20h00 every day, every Sunday or on the 1st`() {
        val thu = at("2026-10-29T15:00:00.000Z") // Thursday 12:00 in São Paulo
        assertEquals(at("2026-10-29T23:00:00.000Z"), nextAutoBackup(thu, BackupEvery.DAY, zone))
        assertEquals(at("2026-11-01T23:00:00.000Z"), nextAutoBackup(thu, BackupEvery.WEEK, zone))
        assertEquals(at("2026-11-01T23:00:00.000Z"), nextAutoBackup(thu, BackupEvery.MONTH, zone))
        assertEquals(at("2026-12-01T23:00:00.000Z"), nextAutoBackup(at("2026-11-01T23:00:00.000Z"), BackupEvery.MONTH, zone))
    }

    @Test
    fun `AC-011-7 only the oldest of the files Tutora wrote go, keeping the newest N`() {
        val written = listOf("a.json", "b.json", "c.json", "d.json")
        assertEquals(listOf("a.json", "b.json"), filesToPrune(written, 2))
        assertEquals(emptyList<String>(), filesToPrune(written, 5))
    }

    @Test
    fun `AC-011-8 a missed run is owed once, however many slots passed`() {
        val last = at("2026-10-04T23:00:00.000Z") // a Sunday's backup
        val threeWeeksLater = at("2026-10-26T12:00:00.000Z")
        assertTrue(autoBackupDue(last, last, BackupEvery.WEEK, threeWeeksLater, zone))
        // Once it runs, the next is the coming Sunday, not one per missed week.
        assertTrue(!autoBackupDue(threeWeeksLater, last, BackupEvery.WEEK, at("2026-10-27T12:00:00.000Z"), zone))
    }

    @Test
    fun `AC-011-10 the warning shows after 14 days without a backup, or 14 days after the first class`() {
        val now = at("2026-10-29T15:00:00.000Z")
        assertEquals(16, backupWarningDays(at("2026-10-13T15:00:00.000Z"), null, now, zone))
        assertNull(backupWarningDays(at("2026-10-20T15:00:00.000Z"), null, now, zone))
        assertEquals(14, backupWarningDays(null, at("2026-10-15T15:00:00.000Z"), now, zone))
        assertNull(backupWarningDays(null, null, now, zone))
    }

    @Test
    fun `AC-011-12 a file that is not a Tutora backup is refused`() {
        assertEquals(BackupRead.NotTutora, readBackup("not json".encodeToByteArray(), 5))
        assertEquals(BackupRead.NotTutora, readBackup("{\"app\":\"other\",\"schemaVersion\":1,\"tables\":{}}".encodeToByteArray(), 5))
        assertEquals(BackupRead.NotTutora, readBackup(byteArrayOf(0x50, 0x4b, 0x03, 0x04), 5))
    }

    @Test
    fun `AC-011-15 an older backup is brought up to date, a newer one refused`() {
        val old = encodeBackup(file.copy(schemaVersion = 2, tables = mapOf("student" to file.tables.getValue("student"))))
        val read = readBackup(old.encodeToByteArray(), 5) as BackupRead.Ok
        assertEquals(5, read.file.schemaVersion)
        assertTrue(listOf("lesson_timer", "payment", "package", "payment_lesson", "credit").all { it in read.file.tables })
        assertEquals(file.tables["student"], read.file.tables["student"])
        assertEquals(BackupRead.Newer(6), readBackup(encodeBackup(file.copy(schemaVersion = 6)).encodeToByteArray(), 5))
    }
}
