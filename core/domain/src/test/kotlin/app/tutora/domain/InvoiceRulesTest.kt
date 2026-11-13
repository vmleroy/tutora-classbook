package app.tutora.domain

import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InvoiceRulesTest {
    private val from = LocalDate(2026, 10, 1)
    private val to = LocalDate(2026, 10, 31)

    private fun cls(
        day: Int,
        value: Cents = 12_000,
        left: Cents = 0,
        who: String = "Ana Souza",
        travel: Cents = 0,
        charge: Cents = 0,
        minutes: Int = 60,
        hour: Int = 14,
    ) = ReportClass("l$day$who", LocalDate(2026, 10, day), minutes, value, who, charge > 0, 0, left, LocalTime(hour, 0), travel, charge)

    private fun report(vararg c: ReportClass) = subjectReport(from, to, c.toList(), emptyList())

    @Test
    fun `AC-020-4 a class's scheduled end is its start plus the time spent in class`() {
        val timed = cls(5, minutes = 95, hour = 14)
        assertEquals(LocalTime(15, 35), timed.end)
    }

    @Test
    fun `AC-020-5 a charged cancellation keeps no time in class and its charge as value`() {
        val r = report(cls(5), cls(8, value = 6_000, charge = 6_000, minutes = 0))
        val cancelled = invoiceOf(r, false).classes.last()
        assertTrue(cancelled.charged)
        assertEquals(0, cancelled.minutes)
        assertEquals(6_000L, cancelled.value)
    }

    @Test
    fun `AC-020-8 the amount to pay is only what is still owed for the period's classes, oldest first`() {
        val inv = invoiceOf(report(cls(22, left = 12_000), cls(5), cls(19, left = 12_000)), false)
        assertEquals(listOf(5, 19, 22), inv.classes.map { it.day.day })
        assertEquals(36_000L to 12_000L, inv.total to inv.paid)
        assertEquals(24_000L, inv.toPay)
    }

    @Test
    fun `AC-020-12 nothing owed in the period makes the Fatura settled`() {
        assertTrue(invoiceOf(report(cls(5), cls(12)), false).settled)
        assertFalse(invoiceOf(report(cls(5, left = 1)), false).settled)
    }

    @Test
    fun `AC-020-13 a place's Fatura groups the classes by student with subtotals, a salary school by month`() {
        val inv = invoiceOf(report(cls(5, who = "Bruno"), cls(6, who = "Ana"), cls(12, who = "Bruno", value = 6_000)), true)
        assertEquals(listOf("Ana", "Bruno"), inv.groups!!.map { it.name })
        assertEquals(18_000L, inv.groups[1].total)
        val school =
            subjectReport(
                from,
                to,
                listOf(cls(5)),
                emptyList(),
                Salary("p", "Escola", 300_000, LocalDate(2026, 1, 1), null),
                to,
            )
        val salaryInv = invoiceOf(school, true)
        assertEquals(listOf(from to 300_000L), salaryInv.salaryMonths)
        assertNull(salaryInv.groups)
        assertTrue(salaryInv.classes.isEmpty())
    }

    @Test
    fun `AC-020-16 the detailed totals separate the classes, travel and extra charges`() {
        val t =
            detailTotals(report(cls(5, value = 15_000, travel = 3_000), cls(8, value = 6_000, charge = 6_000, minutes = 0, left = 6_000)))
        assertEquals(12_000L, t.lessons)
        assertEquals(3_000L, t.travel)
        assertEquals(6_000L, t.charges)
        assertEquals(21_000L, t.total)
        assertEquals(6_000L, t.left)
    }

    @Test
    fun `AC-020-21 a member's shares always add up to the class value and differ by at most a cent`(): Unit =
        runBlocking {
            checkAll(Arb.long(0L..10_000_000L), Arb.int(1..12)) { cents, n ->
                val shares = splitShares(cents, n)
                assertEquals(cents, shares.sum())
                assertTrue(shares.max() - shares.min() <= 1)
                assertEquals(shares.sortedDescending(), shares) // the leftover goes to the first by name
            }
            val group = report(cls(5, value = 10_000, left = 10_000))
            assertEquals(listOf(3_334L, 3_333L, 3_333L), (0..2).map { memberReport(group, 3, it).left })
        }

    @Test
    fun `AC-020-22 only a group with members whose students pay can be split`() {
        assertTrue(canSplit(3, PlaceKind.ONLINE, Payer.STUDENT))
        assertFalse(canSplit(0, PlaceKind.ONLINE, Payer.STUDENT))
        assertFalse(canSplit(3, PlaceKind.SALINHA, Payer.PLACE))
        assertFalse(canSplit(3, PlaceKind.SCHOOL, Payer.STUDENT))
    }

    @Test
    fun `AC-020-23 Faturas do mes lists who still owes, largest first, leaving out who owes nothing`() {
        val rows =
            invoicesDue(
                listOf(
                    DueSource(SubjectKind.STUDENT, "a", "Ana", listOf(12_000, 0)),
                    DueSource(SubjectKind.PLACE, "p", "Salinha", listOf(30_000)),
                    DueSource(SubjectKind.STUDENT, "b", "Bruno", listOf(0)),
                ),
                splitGroups = false,
            )
        assertEquals(listOf("Salinha" to 30_000L, "Ana" to 12_000L), rows.map { it.name to it.toPay })
    }

    @Test
    fun `AC-020-26 with Dividir turmas por aluno a group's members are rows of their own`() {
        val g = DueSource(SubjectKind.GROUP, "g", "Inglês B", listOf(10_000), listOf(Member("1", "Ana"), Member("2", "Bia")))
        assertEquals(listOf("Inglês B"), invoicesDue(listOf(g), false).map { it.name })
        assertEquals(listOf("Ana" to 5_000L, "Bia" to 5_000L), invoicesDue(listOf(g), true).map { it.name to it.toPay })
    }

    @Test
    fun `AC-020-35 a member pays an equal part, a percentage or a set amount, and the rest is split equally`() {
        val m = listOf("ana", "bia", "caio")
        assertEquals(mapOf("ana" to 5_000L, "bia" to 5_000L, "caio" to 5_000L), memberDues(15_000, m, emptyMap()))
        // Ana pays 50%, Bia a set R$ 30: Caio takes what is left.
        val mixed = memberDues(15_000, m, mapOf("ana" to MemberShare.Percent(5_000), "bia" to MemberShare.Amount(3_000)))
        assertEquals(mapOf("ana" to 7_500L, "bia" to 3_000L, "caio" to 4_500L), mixed)
        // Everyone on their own share: what they don't cover isn't charged.
        val all = memberDues(15_000, listOf("ana", "bia"), mapOf("ana" to MemberShare.Amount(5_000), "bia" to MemberShare.Amount(5_000)))
        assertEquals(10_000L, all.values.sum())
        // A set amount never takes more than is left.
        assertEquals(
            mapOf("ana" to 15_000L, "bia" to 0L),
            memberDues(
                15_000,
                listOf("ana", "bia"),
                mapOf(
                    "ana" to MemberShare.Amount(20_000),
                ),
            ),
        )
    }
}
