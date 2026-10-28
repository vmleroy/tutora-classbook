package app.tutora.domain

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportRulesTest {
    private val today = LocalDate(2026, 10, 26)

    private fun cls(
        day: Int,
        value: Cents = 12_000,
        left: Cents = 0,
        who: String = "Ana Souza",
        charged: Boolean = false,
        month: Int = 10,
    ) = ReportClass("l$month$day", LocalDate(2026, month, day), 60, value, who, charged, 0, left)

    @Test
    fun `AC-010-3 the periods cover this month, last month, three months or chosen dates, never past today`() {
        assertEquals(LocalDate(2026, 10, 1) to today, ReportRange.ThisMonth.dates(today))
        assertEquals(LocalDate(2026, 9, 1) to LocalDate(2026, 9, 30), ReportRange.LastMonth.dates(today))
        assertEquals(LocalDate(2026, 8, 1) to today, ReportRange.ThreeMonths.dates(today))
        assertEquals(today to today, ReportRange.Custom(LocalDate(2026, 11, 3), LocalDate(2026, 11, 20)).dates(today))
    }

    @Test
    fun `AC-010-4 the totals are what the classes cost, paid and still to receive`() {
        val r =
            subjectReport(
                LocalDate(2026, 10, 1),
                today,
                listOf(cls(5), cls(12), cls(19, left = 12_000), cls(22, left = 4_000)),
                emptyList(),
            )
        assertEquals(48_000L to 32_000L, r.total to r.paid)
        assertEquals(16_000L, r.left)
        assertEquals(4, r.count)
        assertEquals(240, r.minutes)
    }

    @Test
    fun `AC-010-5 the classes come newest first, a charged cancellation among them`() {
        val r = subjectReport(LocalDate(2026, 10, 1), today, listOf(cls(5), cls(19, value = 6_000, charged = true), cls(12)), emptyList())
        assertEquals(listOf(19, 12, 5), r.classes.map { it.day.day })
        assertEquals(true, r.classes.first().charged)
    }

    @Test
    fun `AC-010-6 a school's report lists the days taught and totals the salary per month, as received`() {
        val salary = Salary("esc", "Colégio", 240_000, LocalDate(2026, 1, 1), null)
        val r =
            subjectReport(
                LocalDate(2026, 8, 1),
                today,
                listOf(cls(23), cls(23), cls(21), cls(2, month = 9)),
                emptyList(),
                salary,
                today,
            )
        assertEquals(720_000L, r.total)
        assertEquals(720_000L, r.paid)
        assertEquals(listOf(TaughtDay(LocalDate(2026, 10, 23), 2), TaughtDay(LocalDate(2026, 10, 21), 1)), r.days?.take(2))
        assertEquals(4, r.count)
    }

    @Test
    fun `AC-010-7 a place's report keeps whose each class was`() {
        val r = subjectReport(LocalDate(2026, 10, 1), today, listOf(cls(22, who = "Ana Souza"), cls(20, who = "Pedro Lima")), emptyList())
        assertEquals(listOf("Ana Souza", "Pedro Lima"), r.classes.map { it.who })
    }

    @Test
    fun `AC-010-9 a long table goes on to more pages`() {
        assertEquals(listOf(0 until 10), paginate(10, 18, 34))
        assertEquals(listOf(0 until 18, 18 until 52, 52 until 60), paginate(60, 18, 34))
    }
}
