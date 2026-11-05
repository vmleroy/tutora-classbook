package app.tutora.domain

import kotlinx.datetime.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Instant

class ReminderRulesTest {
    private val zone = "America/Sao_Paulo"
    private val ana = Student("s", "Ana Souza", null, null, null, TagColor.AMBER, RateType.HOUR, null)
    private val online = builtinPlace(BuiltinPlace.ONLINE)

    private fun lesson(
        id: String,
        start: String,
        status: LessonStatus = LessonStatus.SCHEDULED,
        forecast: Boolean = false,
    ) = Lesson(id, null, null, null, start, 60, null, null, false, null, Who.OfStudent(ana, null), online, status, forecast = forecast)

    private fun at(iso: String) = Instant.parse(iso)

    @Test
    fun `AC-014-12 the reminder comes the chosen minutes before the next class, none for cancelled or forecast classes or when off`() {
        val lessons =
            listOf(
                lesson("cancelled", "2026-10-26T16:00:00.000Z", LessonStatus.CANCELLED),
                lesson("forecast", "2026-10-26T16:30:00.000Z", forecast = true),
                lesson("ana", "2026-10-26T17:00:00.000Z"),
                lesson("later", "2026-10-26T19:00:00.000Z"),
            )
        val now = at("2026-10-26T15:00:00Z")
        assertEquals(ClassReminder("ana", at("2026-10-26T16:45:00Z")), nextClassReminder(lessons, now, 15))
        assertEquals(ClassReminder("ana", at("2026-10-26T16:00:00Z")), nextClassReminder(lessons, now, 60))
        // Once Ana's reminder has gone, the next one is for the class after.
        assertEquals("later", nextClassReminder(lessons, at("2026-10-26T16:45:00Z"), 15)?.lessonId)
        assertNull(nextClassReminder(lessons, now, 0))
    }

    @Test
    fun `AC-014-13 who owes comes weekly on the chosen day and hour, with how many and the total, and not when no one owes`() {
        // Thursday 29 Oct 2026, 10:00 in São Paulo: next Monday 2 Nov at 08:00 (11:00 UTC).
        assertEquals(at("2026-11-02T11:00:00Z"), nextWeekly(at("2026-10-29T13:00:00Z"), DayOfWeek.MONDAY, 8, zone))
        // On Monday at 08:00 itself, the next is a week later.
        assertEquals(at("2026-11-09T11:00:00Z"), nextWeekly(at("2026-11-02T11:00:00Z"), DayOfWeek.MONDAY, 8, zone))
        assertEquals(
            OwedSummary(3, 48_000),
            owedSummary(listOf(Balance.Owes(20_000), Balance.Settled, Balance.Owes(18_000), Balance.Credit(5_000), Balance.Owes(10_000))),
        )
        assertNull(owedSummary(listOf(Balance.Settled, Balance.Credit(1_000))))
    }

    @Test
    fun `AC-014-14 the to-confirm notification comes once a day at the chosen hour`() {
        assertEquals(at("2026-10-26T23:00:00Z"), nextDaily(at("2026-10-26T13:00:00Z"), 20, zone))
        assertEquals(at("2026-10-27T23:00:00Z"), nextDaily(at("2026-10-26T23:00:00Z"), 20, zone))
    }
}
