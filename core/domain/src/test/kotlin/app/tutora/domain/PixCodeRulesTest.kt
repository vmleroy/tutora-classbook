package app.tutora.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PixCodeRulesTest {
    @Test
    fun `AC-020-10 the Pix code matches the Banco Central's example and carries the amount to pay`() {
        // The static BR Code example of the Banco Central's Pix manual, CRC included.
        assertEquals(
            "00020126580014br.gov.bcb.pix0136123e4567-e12b-12d1-a456-426655440000" +
                "5204000053039865802BR5913Fulano de Tal6008BRASILIA62070503***63041D3D",
            pixPayload("123e4567-e12b-12d1-a456-426655440000", "Fulano de Tal", "BRASILIA", null),
        )
        val code = pixPayload("(11) 98765-4321", "Victor Lé Roy", "São Paulo", 24_000)
        assertTrue(code!!.contains("0114+5511987654321")) // a phone key as Pix registers it
        assertTrue(code.contains("5406240.00")) // exactly Valor a pagar
        assertTrue(code.contains("5913Victor Le Roy6009Sao Paulo"))
        assertEquals(crc16(code.dropLast(4)), code.takeLast(4))
    }

    @Test
    fun `AC-020-11 no Pix code without the teacher's name or city`() {
        assertNull(pixPayload("victor@gmail.com", "", "Recife", 1_000))
        assertNull(pixPayload("victor@gmail.com", "Victor", " ", 1_000))
    }
}
