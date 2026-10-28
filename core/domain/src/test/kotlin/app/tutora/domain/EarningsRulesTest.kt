package app.tutora.domain

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EarningsRulesTest {
    private val oct22 = LocalDate(2026, 10, 22)
    private val ana = PayerRef(PayerKind.STUDENT, "ana")

    private fun record(
        status: LessonStatus = LessonStatus.DONE,
        rate: Cents? = 12_000,
        rateType: RateType = RateType.HOUR,
        minutes: Int = 60,
        travel: Cents? = null,
        fee: Cents? = null,
        commissionBp: Int = 0,
        kind: PlaceKind = PlaceKind.ONLINE,
        day: LocalDate = oct22,
        id: String = "l",
    ) = ClassRecord(id, day, status, minutes, null, rate, rateType, travel, fee, ana, "Ana", kind, Payer.STUDENT, commissionBp)

    @Test
    fun `AC-009-1 a done class earns its value with travel, a charge earns the charge, the rest earns nothing`() {
        assertEquals(14_000L, earnedOf(record(travel = 2_000))?.earned)
        assertEquals(6_000L, earnedOf(record(LessonStatus.CANCELLED, rate = null, fee = 6_000))?.earned)
        assertNull(earnedOf(record(LessonStatus.MISSED, rate = null)))
        assertNull(earnedOf(record(LessonStatus.SCHEDULED)))
    }

    @Test
    fun `AC-009-2 with a commission the teacher earns the value after it, never cut from travel`() {
        val c = earnedOf(record(rate = 10_000, commissionBp = 3_000, travel = 1_000))
        assertEquals(3_000L, c?.commission)
        assertEquals(8_000L, c?.earned)
    }

    @Test
    fun `AC-009-3 a package class earns the package price per class`() {
        assertEquals(10_000L, earnedOf(record(rate = 10_000, rateType = RateType.PACKAGE, minutes = 90))?.earned)
    }

    @Test
    fun `AC-009-4 what a bonus covered is not earned, and money credit counts as bonus only while bonuses last`() {
        val cover =
            bonusCover(
                listOf(
                    CreditEntry(null, 5_000, 0, isBonus = true),
                    CreditEntry("a", 4_000, 0, isBonus = false),
                    CreditEntry("b", 4_000, 0, isBonus = false),
                    CreditEntry("c", 3_000, 30, isBonus = false),
                ),
            )
        assertEquals(mapOf("a" to 4_000L, "b" to 1_000L, "c" to 3_000L), cover)
        assertEquals(8_000L, earnedOf(record(), bonus = 4_000)?.earned)
    }

    @Test
    fun `AC-009-5 a school's salary counts once in each of its months up to now`() {
        val s = Salary("esc", "Colégio", 240_000, LocalDate(2026, 3, 10), null)
        val year = Period(PeriodKind.YEAR, oct22).months(oct22)
        assertEquals(8, salaryMonths(s, year).size) // March to October
        val summary =
            summarize(emptyList(), emptyList(), listOf(s), Period(PeriodKind.QUARTER, oct22).months(oct22), emptyMap(), TypeFilter.ALL)
        assertEquals(720_000L, summary.earned)
    }

    @Test
    fun `AC-009-6 received is the payments of the period and the salary, never a bonus`() {
        val s =
            summarize(
                emptyList(),
                listOf(Received(oct22, 12_000, EarningsType.PRIVATE)),
                listOf(Salary("esc", "Colégio", 240_000, LocalDate(2026, 1, 1), null)),
                listOf(LocalDate(2026, 10, 1)),
                emptyMap(),
                TypeFilter.ALL,
            )
        assertEquals(252_000L, s.received)
    }

    @Test
    fun `AC-009-7 to receive is what is still owed on the period's classes`() {
        val classes = listOfNotNull(earnedOf(record(id = "a")), earnedOf(record(id = "b")))
        val s = summarize(classes, emptyList(), emptyList(), emptyList(), mapOf("a" to 0L, "b" to 5_000L), TypeFilter.ALL)
        assertEquals(5_000L, s.toReceive)
    }

    @Test
    fun `AC-009-9 periods cover a month, the quarter ending in it or the year, and move by their length`() {
        val q = Period(PeriodKind.QUARTER, oct22)
        assertEquals(LocalDate(2026, 8, 1) to LocalDate(2026, 10, 31), q.first to q.last)
        assertEquals(LocalDate(2026, 5, 1), q.previous().first)
        assertTrue(Period(PeriodKind.MONTH, oct22).isCurrent(oct22))
        assertFalse(Period(PeriodKind.MONTH, oct22).previous().isCurrent(oct22))
        assertEquals(LocalDate(2026, 12, 31), Period(PeriodKind.YEAR, oct22).last)
    }

    @Test
    fun `AC-009-10 Particulares and Escola e emprego split by the place's type`() {
        val classes = listOfNotNull(earnedOf(record(id = "a")), earnedOf(record(id = "b", kind = PlaceKind.COURSE)))
        assertEquals(12_000L, summarize(classes, emptyList(), emptyList(), emptyList(), emptyMap(), TypeFilter.PRIVATE).earned)
        assertEquals(12_000L, summarize(classes, emptyList(), emptyList(), emptyList(), emptyMap(), TypeFilter.JOB).earned)
        assertEquals(24_000L, summarize(classes, emptyList(), emptyList(), emptyList(), emptyMap(), TypeFilter.ALL).earned)
    }

    @Test
    fun `AC-009-12 the trend has the six months ending at the period, earned and received`() {
        val classes = listOfNotNull(earnedOf(record(day = LocalDate(2026, 6, 3))))
        val t =
            trend(oct22, classes, listOf(Received(LocalDate(2026, 6, 5), 12_000, EarningsType.PRIVATE)), emptyList(), TypeFilter.ALL, oct22)
        assertEquals((5..10).map { LocalDate(2026, it, 1) }, t.map { it.month })
        assertEquals(MonthPoint(LocalDate(2026, 6, 1), 12_000, 12_000), t[1])
    }

    @Test
    fun `AC-009-13 De onde veio sums each payer and school, largest first`() {
        val classes = listOfNotNull(earnedOf(record(id = "a")), earnedOf(record(id = "b")))
        val s =
            sources(
                classes,
                listOf(Salary("esc", "Colégio", 240_000, LocalDate(2026, 1, 1), null)),
                listOf(LocalDate(2026, 10, 1)),
                TypeFilter.ALL,
            )
        assertEquals(listOf("Colégio" to 240_000L, "Ana" to 24_000L), s.map { it.name to it.cents })
    }
}
