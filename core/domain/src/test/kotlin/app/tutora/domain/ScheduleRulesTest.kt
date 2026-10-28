package app.tutora.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleRulesTest {
    private fun d(s: String) = LocalDate.parse(s)

    private val ana = Student("s", "Ana Souza", null, null, null, TagColor.AMBER, RateType.HOUR, null)
    private val online = builtinPlace(BuiltinPlace.ONLINE).copy(rateCents = 10_000)
    private val home = builtinPlace(BuiltinPlace.STUDENT_HOME).copy(rateCents = 12_000)

    private fun worth(
        place: Place,
        value: Cents? = null,
        travel: Cents? = null,
        link: PlaceLink? = null,
    ) = lessonWorth(place, Who.OfStudent(ana, link), 90, value, travel)

    @Test
    fun `AC-005-6 a class repeats weekly or every two weeks`() {
        val weekly = RuleDates(d("2026-10-26"), null, Repeat.WEEKLY)
        assertEquals(
            listOf(
                "2026-10-26",
                "2026-11-02",
                "2026-11-09",
                "2026-11-16",
            ),
            occurrences(weekly, d("2026-10-26"), d("2026-11-16")).map {
                it.toString()
            },
        )
        assertEquals(
            listOf(
                "2026-10-26",
                "2026-11-09",
            ),
            occurrences(weekly.copy(repeat = Repeat.BIWEEKLY), d("2026-10-26"), d("2026-11-16")).map {
                it.toString()
            },
        )
    }

    @Test
    fun `AC-005-7 a series fills 8 weeks on its weekday, from any window, up to its end, minus skips`() {
        val rule = RuleDates(d("2026-10-26"), null, Repeat.WEEKLY)
        val eight = occurrences(rule, d("2026-10-26"), d("2026-12-20"))
        assertEquals(8, eight.size)
        assertTrue(eight.all { it.dayOfWeek == DayOfWeek.MONDAY })
        assertEquals(
            listOf(d("2026-11-09"), d("2026-11-23")),
            occurrences(rule.copy(repeat = Repeat.BIWEEKLY), d("2026-11-03"), d("2026-11-30")),
        )
        assertEquals(
            listOf(d("2026-10-26")),
            occurrences(rule.copy(endsOn = d("2026-11-08"), skips = setOf(d("2026-11-02"))), d("2026-10-26"), d("2026-12-20")),
        )
        assertEquals(d("2026-10-26"), weekStart(d("2026-11-01")))
    }

    @Test
    fun `AC-005-9 a class keeps its local clock time across a daylight-saving change`() {
        val zone = "America/New_York"
        assertEquals("2026-10-26T22:00:00.000Z", localToInstant(d("2026-10-26"), LocalTime(18, 0), zone).isoText())
        assertEquals("2026-11-02T23:00:00.000Z", localToInstant(d("2026-11-02"), LocalTime(18, 0), zone).isoText())
        assertEquals(LocalTime(18, 0), localOf("2026-11-02T23:00:00.000Z", zone).time)
        assertEquals("2026-10-26T17:00:00.000Z", localToInstant(d("2026-10-26"), LocalTime(14, 0), "America/Sao_Paulo").isoText())
    }

    @Test
    fun `AC-005-4 a class is worth its price for the duration plus travel, unset without a price, or a typed value`() {
        assertEquals(Worth.Set(15_000, PriceSource.PLACE), worth(online))
        assertEquals(Worth.Set(20_000, PriceSource.PLACE), worth(home, travel = 2000))
        assertEquals(Worth.Unset, worth(online.copy(rateCents = null)))
        assertEquals(Worth.Set(9000, PriceSource.HAND), worth(online, value = 9000))
    }

    @Test
    fun `AC-005-16 a scheduled class follows the current price and a typed value stays`() {
        val raised = online.copy(rateCents = 12_000)
        assertEquals(18_000L, (worth(raised) as Worth.Set).cents)
        assertEquals(9000L, (worth(raised, value = 9000) as Worth.Set).cents)
        assertEquals(19_500L, (worth(raised, link = PlaceLink(online.id, true, 13_000)) as Worth.Set).cents)
    }

    @Test
    fun `AC-005-19 Previsto sums priced classes and flags the ones a salary covers`() {
        assertEquals(Expected(12_000, true), expected(listOf(Worth.Set(12_000, PriceSource.PLACE), Worth.Unset, Worth.Salary)))
    }

    @Test
    fun `AC-005-21 overlapping classes sit side by side`() {
        val layout = layoutDay(listOf(Span("a", 600, 660), Span("b", 630, 720), Span("c", 800, 860)))
        assertEquals(Slot(0, 2), layout["a"])
        assertEquals(Slot(1, 2), layout["b"])
        assertEquals(Slot(0, 1), layout["c"])
    }

    @Test
    fun `AC-005-22 the greeting follows the time of day`() {
        assertEquals(listOf(Greeting.MORNING, Greeting.AFTERNOON, Greeting.EVENING, Greeting.EVENING), listOf(8, 14, 20, 2).map(::greeting))
    }

    @Test
    fun `AC-005-23 the next class is the first one that has not started`() {
        val starts = listOf("2026-10-27T17:00:00.000Z", "2026-10-26T11:00:00.000Z", "2026-10-26T17:00:00.000Z")
        assertEquals("2026-10-26T17:00:00.000Z", nextLesson(starts, "2026-10-26T16:35:00.000Z") { it })
    }
}
