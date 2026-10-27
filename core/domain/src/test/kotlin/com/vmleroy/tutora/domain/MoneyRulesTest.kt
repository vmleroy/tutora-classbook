package com.vmleroy.tutora.domain

import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyRulesTest {
    @Test
    fun `AC-003-1 an hourly lesson is price x minutes over 60, rounded once`() {
        assertEquals(8333L, lessonValue(10_000, RateType.HOUR, 50))
        assertEquals(7500L, lessonValue(10_000, RateType.HOUR, 45))
        assertEquals(3L, lessonValue(5, RateType.HOUR, 30)) // 2,5 centavos → 3
    }

    @Test
    fun `AC-003-2 a per-lesson price is the same whatever the duration`() {
        for (m in listOf(30, 50, 60, 90)) assertEquals(12_000L, lessonValue(12_000, RateType.LESSON, m))
    }

    @Test
    fun `AC-003-3 with a commission the teacher keeps price x (100 percent minus commission), rounded once`() {
        assertEquals(7000L, afterCommission(10_000, 3000))
        assertEquals(5833L, afterCommission(8333, 3000))
    }

    @Test
    fun `AC-003-4 a total is the exact sum of stored values, with no drift`() {
        runBlocking {
            checkAll(Arb.list(Arb.long(-10_000_000L..10_000_000L), 0..500)) { values -> assertEquals(values.sum(), sum(values)) }
        }
        assertEquals(8_333_000L, sum(List(1000) { 8333L }))
    }

    @Test
    fun `AC-003-7 compact amounts are whole reais, half away from zero`() {
        assertEquals(1181L, wholeReais(118_050))
        assertEquals(1180L, wholeReais(118_049))
        assertEquals(-1181L, wholeReais(-118_050))
    }

    @Test
    fun `AC-003-8 a money field fills from the right as digits are typed`() {
        var value = 0L
        for (d in "12000") value = typedCents("R$ $value$d", value)
        assertEquals(12_000L, value)
    }

    @Test
    fun `AC-003-9 only digits count and deleting drops the last digit`() {
        assertEquals(1200L, typedCents("R$ 120,0a", 12_000))
        assertEquals(1200L, typedCents("R$ 120,0", 12_000))
        assertEquals(0L, typedCents("R$ ,", 1))
    }

    @Test
    fun `AC-003-10 digits past R$ 99999,99 are refused`() {
        assertEquals(9_999_999L, typedCents("R$ 99.999,99", 999_999))
        assertEquals(9_999_999L, typedCents("R$ 99.999,990", 9_999_999))
        assertEquals(1_000_000L, typedCents("R$ 10.000,000", 1_000_000))
    }

    @Test
    fun `AC-004-27 a duration is typed as hh-mm from the right, and over 59 minutes or 5 hours is no duration`() {
        var digits = ""
        var minutes: Int? = 0
        for (d in "130") {
            val next = typedDuration(hhmm(digits) + d, digits)
            digits = next.digits
            minutes = next.minutes
        }
        assertEquals("01:30", hhmm(digits))
        assertEquals(90, minutes)
        assertEquals(null, typedDuration("00:90", "9").minutes)
        assertEquals(null, typedDuration("05:01", "50").minutes)
        assertEquals(300, typedDuration("05:00", "50").minutes)
        assertEquals("1234", typedDuration("12:345", "1234").digits)
    }
}
