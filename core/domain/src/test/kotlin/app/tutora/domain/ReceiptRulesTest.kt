package app.tutora.domain

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptRulesTest {
    @Test
    fun `AC-021-2 a photo over 2000 px shrinks with its aspect kept, a PDF is kept up to 10 MB, nothing else is taken`() {
        assertEquals(ReceiptIntake.Shrink(2000, 1500), receiptPolicy("image/jpeg", 3_000_000, 4000, 3000))
        assertEquals(ReceiptIntake.Shrink(900, 2000), receiptPolicy("image/png", 900_000, 1080, 2400))
        assertEquals(ReceiptIntake.Keep, receiptPolicy("image/png", 400_000, 1080, 2000))
        assertEquals(ReceiptIntake.Keep, receiptPolicy("application/pdf", RECEIPT_MAX_PDF_BYTES))
        assertEquals(ReceiptIntake.TooLarge, receiptPolicy("application/pdf", RECEIPT_MAX_PDF_BYTES + 1))
        assertEquals(ReceiptIntake.Unsupported, receiptPolicy("text/plain", 10))
    }

    @Test
    fun `AC-021-3 Anexar comprovante lists the last 60 days, newest first`() {
        val today = LocalDate(2026, 11, 13)

        fun pay(
            id: String,
            day: LocalDate,
            at: String,
        ) = AttachablePayment(id, PayerRef(PayerKind.STUDENT, "s"), day, at, PayMethod.PIX, 24000, 0)
        val list =
            listOf(
                pay("old", LocalDate(2026, 9, 13), "2026-09-13T15:00:00.000Z"),
                pay("edge", LocalDate(2026, 9, 14), "2026-09-14T15:00:00.000Z"),
                pay("morning", today, "2026-11-13T10:00:00.000Z"),
                pay("evening", today, "2026-11-13T21:00:00.000Z"),
            )
        assertEquals(listOf("evening", "morning", "edge"), attachable(list, today).map { it.id })
    }

    @Test
    fun `AC-021-12 the space comprovantes take, in tenths of a megabyte`() {
        assertEquals(ReceiptUsage(12, 84), receiptUsage(12, 8_350_000))
        assertEquals(ReceiptUsage(0, 0), receiptUsage(0, 0))
    }
}
