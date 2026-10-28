package app.tutora.domain

import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MoneyTest {
    private val cents = Arb.long(-1_000_000_000L..1_000_000_000L)

    @Test
    fun `AC-001-17 rounding is half away from zero`() {
        assertEquals(3, roundHalfAwayFromZero(5, 2)) // 2.5
        assertEquals(-3, roundHalfAwayFromZero(-5, 2))
        assertEquals(2, roundHalfAwayFromZero(7, 3)) // 2.33
        assertEquals(0, roundHalfAwayFromZero(1, 3))
        assertEquals(8333, multiplyRate(10_000, 50, 60)) // R$ 100/h for 50 min
        assertThrows(ArithmeticException::class.java) { add(Long.MAX_VALUE, 1) }
    }

    @Test
    fun `AC-001-17 addition is exact and rounding lands within half a cent`() {
        runBlocking {
            checkAll(cents, cents) { a, b -> assertEquals(a + b, add(a, b)) }
            checkAll(cents, Arb.long(1L..1_000_000L)) { n, d ->
                val r = roundHalfAwayFromZero(n, d)
                val twiceError = abs(2 * n - 2 * r * d)
                assertTrue(twiceError <= d)
                assertEquals(-r, roundHalfAwayFromZero(-n, d))
            }
        }
    }
}
