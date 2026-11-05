package app.tutora.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

class ConfirmRulesTest {
    private val zone = "America/Sao_Paulo"

    // Monday 26 Oct 2026, 14:00 to 15:00 in São Paulo.
    private val start = "2026-10-26T17:00:00.000Z"

    private fun at(iso: String) = Instant.parse(iso)

    private fun toConfirm(
        now: String,
        whenTo: ConfirmWhen = ConfirmWhen.END,
        status: LessonStatus = LessonStatus.SCHEDULED,
        forecast: Boolean = false,
    ) = isToConfirm(status, forecast, start, 60, at(now), zone, whenTo)

    @Test
    fun `AC-014-1 a scheduled class is to confirm once it ended, an hour later or the next day, never when done, cancelled, missed, live or forecast`() {
        assertFalse(toConfirm("2026-10-26T17:59:00Z"))
        assertTrue(toConfirm("2026-10-26T18:00:00Z"))
        assertFalse(toConfirm("2026-10-26T18:59:00Z", ConfirmWhen.HOUR))
        assertTrue(toConfirm("2026-10-26T19:00:00Z", ConfirmWhen.HOUR))
        // The next day starts at 00:00 in São Paulo, 03:00 UTC.
        assertFalse(toConfirm("2026-10-27T02:59:00Z", ConfirmWhen.NEXT_DAY))
        assertTrue(toConfirm("2026-10-27T03:00:00Z", ConfirmWhen.NEXT_DAY))
        for (s in listOf(LessonStatus.DONE, LessonStatus.CANCELLED, LessonStatus.MISSED, LessonStatus.LIVE)) {
            assertFalse(toConfirm("2026-11-30T00:00:00Z", status = s))
        }
        assertFalse(toConfirm("2026-11-30T00:00:00Z", forecast = true))
    }
}
