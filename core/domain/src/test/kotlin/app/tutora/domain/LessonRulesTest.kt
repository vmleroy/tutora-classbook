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
    fun `AC-006-16 Previsto counts scheduled and done classes and kept fees, not a cancellation without a fee`() {
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
    fun `AC-006-19 a teacher who never set the rule is never charged a fee, and turning a part on starts at 24h, 50 and 100 percent`() {
        val rule = Settings(Language.PT_BR).feeRule
        assertEquals(FeeSuggestion.RuleOff, suggestFee(rule, LessonStatus.CANCELLED, 12_000, start, start))
        assertEquals(FeeSuggestion.RuleOff, suggestFee(rule, LessonStatus.MISSED, 12_000, start, null))
        assertEquals(24, rule.windowH)
        assertEquals(FeeAmount.Percent(5_000), rule.late)
        assertEquals(FeeAmount.Percent(10_000), rule.miss)
    }

    @Test
    fun `AC-006-21 a class follows the student's rule, then the place's, then the default`() {
        val default = FeeRule(lateOn = true)
        val own = FeeRule(missOn = true)
        assertEquals(own, ruleFor(FeeChoice.Own(own), FeeChoice.Never, default))
        assertNull(ruleFor(FeeChoice.Never, FeeChoice.Own(own), default))
        assertEquals(own, ruleFor(FeeChoice.Default, FeeChoice.Own(own), default))
        assertEquals(default, ruleFor(FeeChoice.Default, FeeChoice.Default, default))
        // A group has no choice of its own.
        assertNull(ruleFor(null, FeeChoice.Never, default))
    }

    @Test
    fun `AC-006-24 a percentage is taken from the value without travel and rounded once, a fixed fee is the rule's amount`() {
        val rule = FeeRule(lateOn = true, missOn = true, late = FeeAmount.Percent(5_000), miss = FeeAmount.Fixed(3_000))
        val notice = "2026-10-26T14:00:00.000Z" // 3h before
        assertEquals(
            FeeSuggestion.Charge(6_001, FeeAmount.Percent(5_000), 24),
            suggestFee(rule, LessonStatus.CANCELLED, 12_001, start, notice),
        )
        assertEquals(FeeSuggestion.Charge(3_000, FeeAmount.Fixed(3_000), null), suggestFee(rule, LessonStatus.MISSED, 12_000, start, null))
        assertEquals(6_000L, lesson(home, status = LessonStatus.SCHEDULED).valueWithoutTravel()?.let { feeCents(rule.late, it) })
    }

    @Test
    fun `AC-006-25 enough notice or a rule that does not charge suggests no fee`() {
        val rule = FeeRule(lateOn = true, windowH = 24)
        val dayBefore = "2026-10-25T16:00:00.000Z" // 25h before
        assertEquals(FeeSuggestion.EnoughNotice, suggestFee(rule, LessonStatus.CANCELLED, 12_000, start, dayBefore))
        assertEquals(FeeSuggestion.RuleOff, suggestFee(rule, LessonStatus.MISSED, 12_000, start, null))
        assertEquals(FeeSuggestion.RuleOff, suggestFee(null, LessonStatus.CANCELLED, 12_000, start, start))
    }

    @Test
    fun `AC-006-26 a class without a price and a percentage rule suggests no fee`() {
        val rule = FeeRule(missOn = true)
        assertEquals(FeeSuggestion.NoPrice, suggestFee(rule, LessonStatus.MISSED, null, start, null))
    }
}
