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
    fun `AC-014-30 the bars follow the period - weeks of a month, months of a quarter or year, none for a day`() {
        val oct = Period(PeriodKind.MONTH, oct22)
        val weeks = periodSpans(oct)
        // October 2026 starts on a Thursday: 1-4, 5-11, 12-18, 19-25, 26-31.
        assertEquals(listOf(1 to 4, 5 to 11, 12 to 18, 19 to 25, 26 to 31), weeks.map { it.first.day to it.second.day })
        assertEquals(3, periodSpans(Period(PeriodKind.QUARTER, oct22)).size)
        assertEquals(12, periodSpans(Period(PeriodKind.YEAR, oct22)).size)
        assertEquals(emptyList<Pair<LocalDate, LocalDate>>(), periodSpans(Period(PeriodKind.DAY, oct22)))
        // A class in June never shows on October's bars.
        val classes = listOfNotNull(earnedOf(record(day = LocalDate(2026, 6, 3))), earnedOf(record(id = "b", day = LocalDate(2026, 10, 6))))
        val b = bars(weeks, classes, emptyList(), emptyList(), TypeFilter.ALL, oct22)
        assertEquals(listOf(0L, 12_000L, 0L, 0L, 0L), b.map { it.earned })
    }

    @Test
    fun `AC-014-31 Evolucao compares two months - the later minus the earlier, in reais and percent`() {
        val sep = Bar(LocalDate(2026, 9, 1), LocalDate(2026, 9, 30), 280_000, 250_000)
        val oct = Bar(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31), 320_000, 300_000)
        val c = compareBars(oct, sep)
        assertEquals(sep, c.earlier)
        assertEquals(40_000L, c.difference)
        assertEquals(14L, c.percent)
        assertEquals(null, compareBars(sep.copy(earned = 0), oct).percent)
        assertEquals(6, monthSpans(LocalDate(2026, 5, 1), 6).size)
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

    @Test
    fun `AC-014-34 each month of Evolucao has its totals, classes, hours and the change from the month before`() {
        val classes =
            listOfNotNull(
                earnedOf(record(id = "s", day = LocalDate(2026, 9, 10))),
                earnedOf(record(id = "o1", day = LocalDate(2026, 10, 6), minutes = 90)),
                earnedOf(record(id = "o2", day = LocalDate(2026, 10, 8))),
            )
        val months =
            monthDetails(
                monthSpans(LocalDate(2026, 9, 1), 2),
                classes,
                listOf(Received(LocalDate(2026, 10, 9), 12_000, EarningsType.PRIVATE, ana)),
                emptyList(),
                EvolutionFilter(),
                oct22,
                mapOf("o2" to 12_000L),
            )
        val oct = months[1]
        assertEquals(30_000L, oct.bar.earned)
        assertEquals(12_000L, oct.bar.received)
        assertEquals(12_000L, oct.toReceive)
        assertEquals(2, oct.classes)
        assertEquals(150, oct.minutes)
        assertEquals(18_000L, oct.change?.difference)
        assertEquals(150L, oct.change?.percent)
        assertNull(months[0].change)
    }

    @Test
    fun `AC-014-35 Evolucao filters by who pays and by place kind, combined with the type`() {
        val bia = PayerRef(PayerKind.STUDENT, "bia")
        val classes =
            listOfNotNull(
                earnedOf(record(id = "a")),
                earnedOf(record(id = "h", kind = PlaceKind.STUDENT_HOME)),
                earnedOf(record(id = "b").copy(payer = bia)),
            )
        val span = monthSpans(LocalDate(2026, 10, 1), 1)

        fun earned(f: EvolutionFilter) = monthDetails(span, classes, emptyList(), emptyList(), f, oct22, emptyMap()).single().bar.earned
        assertEquals(36_000L, earned(EvolutionFilter()))
        assertEquals(24_000L, earned(EvolutionFilter(payer = ana)))
        assertEquals(12_000L, earned(EvolutionFilter(payer = ana, kind = PlaceKind.STUDENT_HOME)))
        assertEquals(0L, earned(EvolutionFilter(type = TypeFilter.JOB)))
    }

    @Test
    fun `AC-019-2 a period runs from De to Ate, never past today, and at most 5 years`() {
        val today = LocalDate(2026, 11, 5)
        assertTrue(evolutionPeriodOk(LocalDate(2024, 11, 6), today, today))
        assertTrue(evolutionPeriodOk(today, today, today))
        assertFalse("Até before De", evolutionPeriodOk(LocalDate(2026, 11, 2), LocalDate(2026, 11, 1), today))
        assertFalse("Até after today", evolutionPeriodOk(LocalDate(2026, 10, 1), LocalDate(2026, 11, 6), today))
        assertTrue("5 years", evolutionPeriodOk(LocalDate(2021, 11, 6), today, today))
        assertFalse("over 5 years", evolutionPeriodOk(LocalDate(2021, 11, 5), today, today))
    }

    @Test
    fun `AC-019-3 a gap of dates is shown month by month, the first and last month cut to it`() {
        assertEquals(
            listOf(
                LocalDate(2026, 3, 15) to LocalDate(2026, 3, 31),
                LocalDate(2026, 4, 1) to LocalDate(2026, 4, 30),
                LocalDate(2026, 5, 1) to LocalDate(2026, 5, 10),
            ),
            rangeSpans(LocalDate(2026, 3, 15), LocalDate(2026, 5, 10)),
        )
        assertEquals(listOf(LocalDate(2026, 10, 5) to LocalDate(2026, 10, 9)), rangeSpans(LocalDate(2026, 10, 5), LocalDate(2026, 10, 9)))
        assertEquals(24, rangeSpans(LocalDate(2024, 11, 1), LocalDate(2026, 10, 31)).size)
    }

    @Test
    fun `AC-019-4 each month's row counts only the classes inside the gap`() {
        val classes =
            listOfNotNull(
                earnedOf(record(id = "before", day = LocalDate(2026, 10, 2))),
                earnedOf(record(id = "inside", day = LocalDate(2026, 10, 22))),
            )
        val rows =
            monthDetails(
                rangeSpans(LocalDate(2026, 10, 10), oct22),
                classes,
                emptyList(),
                emptyList(),
                EvolutionFilter(),
                oct22,
                emptyMap(),
            )
        assertEquals(1, rows.single().classes)
        assertEquals(12_000L, rows.single().bar.earned)
    }

    @Test
    fun `AC-019-5 a 2-year period compares any two of its 24 months`() {
        val classes =
            listOfNotNull(
                earnedOf(record(id = "old", day = LocalDate(2024, 11, 20))),
                earnedOf(record(id = "new", day = oct22, rate = 24_000)),
            )
        val rows =
            monthDetails(rangeSpans(LocalDate(2024, 11, 1), oct22), classes, emptyList(), emptyList(), EvolutionFilter(), oct22, emptyMap())
        val c = compareBars(rows.first().bar, rows.last().bar)
        assertEquals(12_000L, c.difference)
        assertEquals(100L, c.percent)
    }
}
