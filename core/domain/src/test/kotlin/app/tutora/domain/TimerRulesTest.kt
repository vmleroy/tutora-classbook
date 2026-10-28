package app.tutora.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimerRulesTest {
    private fun at(time: String) = "2026-10-26T$time.000Z"

    private val started = startTimer("l", at("17:00:00"))

    @Test
    fun `AC-007-5 a per-hour price earns by the second, rounded once, and a per-lesson price doesn't tick`() {
        val hour = Price.Set(12_000, RateType.HOUR, PriceSource.PLACE)
        assertEquals(3_000L, earnedSoFar(hour, 15 * 60_000L))
        assertEquals(3L, earnedSoFar(hour, 1_000L)) // R$ 0,0333 per second rounds to R$ 0,03
        assertNull(earnedSoFar(Price.Set(15_000, RateType.LESSON, PriceSource.PLACE), 15 * 60_000L))
    }

    @Test
    fun `AC-007-6 the time counted comes from timestamps, so reading it later gives the same answer`() {
        assertEquals(23 * 60_000L + 41_000L, elapsedMs(started, at("17:23:41")))
        val paused = pause(started, at("17:10:00"))
        // However late it is read, a paused timer stays where it stopped.
        assertEquals(10 * 60_000L, elapsedMs(paused, at("23:59:59")))
        assertEquals(0L, elapsedMs(started, at("16:00:00")))
    }

    @Test
    fun `AC-007-7 pausing stops the count and resuming goes on from where it stopped`() {
        val paused = pause(started, at("17:10:00"))
        val resumed = resume(paused, at("17:25:00"))
        assertEquals(15 * 60_000L, resumed.pausedMs)
        assertEquals(15 * 60_000L, elapsedMs(resumed, at("17:30:00")))
    }

    @Test
    fun `AC-007-9 the real duration offered is the nearest minute, at least one`() {
        assertEquals(62, liveMinutes(61 * 60_000L + 30_000L))
        assertEquals(61, liveMinutes(61 * 60_000L + 29_000L))
        assertEquals(1, liveMinutes(5_000L))
    }
}
