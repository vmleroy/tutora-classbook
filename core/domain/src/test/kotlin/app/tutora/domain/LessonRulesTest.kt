package app.tutora.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LessonRulesTest {
    private val ana = Student("s", "Ana Souza", null, null, null, TagColor.AMBER, RateType.HOUR, null)
    private val online = builtinPlace(BuiltinPlace.ONLINE).copy(rateCents = 12_000)
    private val home =
        builtinPlace(
            BuiltinPlace.STUDENT_HOME,
        ).copy(rateCents = 12_000, travelMode = TravelMode.FIXED, travelFeeCents = 2_000)

    // Monday 26 Oct 2026, 14:00 in São Paulo.
    private val start = "2026-10-26T17:00:00.000Z"

    private fun lesson(
        place: Place = online,
        minutes: Int = 60,
        status: LessonStatus = LessonStatus.SCHEDULED,
        value: Cents? = null,
        travel: Cents? = null,
        frozen: Price.Set? = null,
        fee: Cents? = null,
    ) = Lesson(
        "l",
        null,
        null,
        null,
        start,
        minutes,
        value,
        travel,
        false,
        null,
        Who.OfStudent(ana, null),
        place,
        status,
        frozen,
        fee,
    )

    private val frozen120 = Price.Set(12_000, RateType.HOUR, PriceSource.PLACE)

    @Test
    fun `AC-006-2 a done class is worth its price for its real minutes plus travel, or the value typed by hand`() {
        assertEquals(Worth.Set(20_000, PriceSource.PLACE), lesson(home, 90, LessonStatus.DONE, travel = 2_000, frozen = frozen120).worth())
        assertEquals(Worth.Set(7_000, PriceSource.HAND), lesson(home, 90, LessonStatus.DONE, value = 5_000, travel = 2_000).worth())
    }

    @Test
    fun `AC-006-3 a done class keeps its frozen price when the place's price changes`() {
        val done = lesson(online, 60, LessonStatus.DONE, frozen = frozen120)
        assertEquals(Worth.Set(12_000, PriceSource.PLACE), done.copy(place = online.copy(rateCents = 20_000)).worth())
        // A scheduled class still follows the price.
        assertEquals(Worth.Set(20_000, PriceSource.PLACE), lesson(online.copy(rateCents = 20_000)).worth())
    }

    @Test
    fun `AC-006-12 changing a done class's minutes recomputes from its frozen price, a typed value stays`() {
        val done = lesson(online.copy(rateCents = 50_000), 60, LessonStatus.DONE, frozen = frozen120)
        assertEquals(Worth.Set(18_000, PriceSource.PLACE), done.copy(plannedMin = 90).worth())
        assertEquals(Worth.Set(9_000, PriceSource.HAND), done.copy(plannedMin = 90, valueCents = 9_000).worth())
    }

    @Test
    fun `AC-006-16 Previsto counts scheduled and done classes and charges, not a cancellation without a charge`() {
        val day =
            listOf(
                lesson(),
                lesson(status = LessonStatus.DONE, frozen = frozen120),
                lesson(status = LessonStatus.CANCELLED, fee = 6_000),
                lesson(status = LessonStatus.MISSED),
                lesson(status = LessonStatus.CANCELLED),
            )
        assertEquals(Expected(30_000, false), expected(day.mapNotNull { it.counted() }))
    }

    @Test
    fun `AC-006-19 a percentage charge is that share of the value without travel, rounded once, and another amount is itself`() {
        assertEquals(6_001L, chargeCents(Charge.Percent(5_000), 12_001))
        assertEquals(6_000L, chargeCents(Charge.Percent(5_000), lesson(home).valueWithoutTravel()))
        assertEquals(3_000L, chargeCents(Charge.Amount(3_000), 12_000))
    }

    @Test
    fun `AC-006-20 a class the teacher cancels is never chargeable`() {
        assertEquals(listOf(CancelReason.STUDENT, CancelReason.NO_SHOW), CancelReason.entries.filter { it.chargeable })
        assertEquals(LessonStatus.MISSED, CancelReason.NO_SHOW.status)
    }

    @Test
    fun `AC-006-21 a class without a price has no percentage charge, only another amount`() {
        assertNull(chargeCents(Charge.Percent(10_000), null))
        assertEquals(2_500L, chargeCents(Charge.Amount(2_500), null))
    }
}
