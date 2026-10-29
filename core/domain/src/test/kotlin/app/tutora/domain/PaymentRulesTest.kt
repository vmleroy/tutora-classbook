package app.tutora.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentRulesTest {
    private val ana = Student("s", "Ana Souza", null, null, null, TagColor.AMBER, RateType.HOUR, null)
    private val online = builtinPlace(BuiltinPlace.ONLINE).copy(rateCents = 12_000)
    private val salinha =
        online.copy(id = "sal", builtin = null, name = "Salinha", kind = PlaceKind.SALINHA, payer = Payer.PLACE, rateCents = 6_000)
    private val school = online.copy(id = "esc", builtin = null, name = "Escola", kind = PlaceKind.SCHOOL, rateCents = null)
    private val enem = Group("g", "sal", "Turma ENEM", null, 18_000, emptyList(), null)
    private val hour120 = Price.Set(12_000, RateType.HOUR, PriceSource.PLACE)

    private fun lesson(
        place: Place = online,
        status: LessonStatus = LessonStatus.DONE,
        who: Who = Who.OfStudent(ana, null),
        frozen: Price.Set? = hour120,
        fee: Cents? = null,
        travel: Cents? = null,
    ) = Lesson("l", null, null, null, "2026-10-26T17:00:00.000Z", 60, null, travel, false, null, who, place, status, frozen, fee)

    @Test
    fun `AC-008-1 a done class owes its value to the place that pays, else the group, else the student, and no price owes nothing`() {
        assertEquals(Due(PayerRef(PayerKind.STUDENT, "s"), 12_000), dueOf(lesson(), null))
        assertEquals(PayerKind.PLACE, dueOf(lesson(salinha, who = Who.OfGroup(enem)), null)?.payer?.kind)
        assertEquals(PayerKind.GROUP, payerOf(online, Who.OfGroup(enem)).kind)
        assertNull(dueOf(lesson(frozen = null), null))
        assertNull(dueOf(lesson(school, frozen = null), null))
    }

    @Test
    fun `AC-008-2 a charged cancellation owes its charge, an uncharged one nothing`() {
        assertEquals(6_000L, dueOf(lesson(status = LessonStatus.CANCELLED, frozen = null, fee = 6_000), null)?.cents)
        assertNull(dueOf(lesson(status = LessonStatus.MISSED, frozen = null), null))
        assertNull(dueOf(lesson(status = LessonStatus.SCHEDULED), null))
    }

    @Test
    fun `AC-008-4 the balance is owed minus paid - Deve, Em dia or Credito`() {
        assertEquals(Balance.Owes(24_000), balanceOf(36_000, 12_000))
        assertEquals(Balance.Settled, balanceOf(12_000, 12_000))
        assertEquals(Balance.Credit(4_000), balanceOf(12_000, 16_000))
    }

    private val owed =
        listOf(
            Owed("a", "2026-10-15T17:00:00.000Z", 12_000),
            Owed("b", "2026-10-19T17:00:00.000Z", 12_000),
            Owed("c", "2026-10-22T17:00:00.000Z", 12_000),
        )

    @Test
    fun `AC-008-6 by default a payment pays the oldest classes first, and can be less than owed`() {
        val a = allocate(owed, listOf(Paid("p", "2026-10-23T12:00:00.000Z", 16_000)))
        assertEquals(listOf("a", "b"), a.paidBy["p"])
        assertEquals(mapOf("a" to 0L, "b" to 8_000L, "c" to 12_000L), a.left)
        assertEquals(0L, a.credit)
    }

    @Test
    fun `AC-008-8 chosen classes are paid first, the leftover pays the oldest, and money beyond everything is credit`() {
        val a = allocate(owed, listOf(Paid("p", "2026-10-23T12:00:00.000Z", 16_000, chosen = listOf("c"))))
        assertEquals(mapOf("a" to 8_000L, "b" to 12_000L, "c" to 0L), a.left)
        val more = allocate(owed, listOf(Paid("p", "2026-10-23T12:00:00.000Z", 40_000)))
        assertEquals(4_000L, more.credit)
        assertEquals(true, owed.all { more.isPaid(it.lessonId) })
    }

    @Test
    fun `AC-008-23 the package price per class rounds once, a package class owes only travel, and a charge never uses the package`() {
        assertEquals(3_333L, packageUnit(10_000, 3))
        val pkgAna = ana.copy(rateType = RateType.PACKAGE, packageLessons = 10, packagePriceCents = 100_000)
        assertEquals(Price.Set(10_000, RateType.PACKAGE, PriceSource.PACKAGE), resolvePrice(online, pkgAna))
        val unit = Price.Set(10_000, RateType.PACKAGE, PriceSource.PACKAGE)
        val pkg = PackageState("k", 10, 3)
        assertEquals(Due(PayerRef(PayerKind.STUDENT, "s"), 2_000, "k"), dueOf(lesson(frozen = unit, travel = 2_000), pkg))
        // Used up: the price per class is owed.
        assertEquals(Due(PayerRef(PayerKind.STUDENT, "s"), 10_000), dueOf(lesson(frozen = unit), pkg.copy(used = 10)))
        assertEquals(
            Due(PayerRef(PayerKind.STUDENT, "s"), 5_000),
            dueOf(lesson(status = LessonStatus.CANCELLED, frozen = null, fee = 5_000), pkg),
        )
    }

    // AC-008-29 (credit waiting to be placed) is replaced by AC-016-24 (Victor, 29 Oct 2026).
    @Test
    fun `AC-016-24 a payment's leftover never pays a class given after it and is a tip, not credit`() {
        val early = listOf(Owed("a", "2026-10-15T17:00:00.000Z", 12_000), Owed("later", "2026-10-30T17:00:00.000Z", 12_000))
        val a = allocate(early, listOf(Paid("p", "2026-10-20T12:00:00.000Z", 20_000)))
        assertEquals(0L, a.left["a"])
        assertEquals(12_000L, a.left["later"])
        val owed = early.map { OwedClass(it.lessonId, it.startAt, 60, it.cents) }
        val paid =
            PaymentRecord("p", PayerRef(PayerKind.STUDENT, "s"), 20_000, PayMethod.PIX, "2026-10-20T12:00:00.000Z", null, null, emptyList())
        val ledger = ledgerOf(owed, listOf(paid))
        assertEquals(0L, ledger.creditCents)
    }

    @Test
    fun `AC-008-30 credit put on a class comes off it, and only while the class is owed`() {
        val a = allocate(owed, emptyList(), uses = listOf(CreditUse("b", 5_000), CreditUse("gone", 3_000)))
        assertEquals(7_000L, a.left["b"])
        assertEquals(5_000L, a.usedCents)
    }

    @Test
    fun `AC-008-31 bonus minutes cover a class by the hour, or a fixed price only in full`() {
        val hour = Price.Set(12_000, RateType.HOUR, PriceSource.PLACE)
        assertEquals(6_000L, minutesCover(hour, 60, 30, 12_000))
        assertEquals(12_000L, minutesCover(hour, 60, 90, 12_000))
        val fixed = Price.Set(15_000, RateType.LESSON, PriceSource.PLACE)
        assertEquals(0L, minutesCover(fixed, 60, 30, 15_000))
        assertEquals(15_000L, minutesCover(fixed, 60, 60, 15_000))
    }
}
