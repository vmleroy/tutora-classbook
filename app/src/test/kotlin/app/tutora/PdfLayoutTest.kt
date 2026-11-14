package app.tutora

import app.tutora.adapters.export.pdfPages
import app.tutora.domain.EarnedClass
import app.tutora.domain.EarningsType
import app.tutora.domain.EarningsView
import app.tutora.domain.MonthPoint
import app.tutora.domain.PayMethod
import app.tutora.domain.PayerKind
import app.tutora.domain.PayerRef
import app.tutora.domain.ReportClass
import app.tutora.domain.ReportPayment
import app.tutora.domain.Source
import app.tutora.domain.Summary
import app.tutora.domain.subjectReport
import app.tutora.ui.reports.EarningsParts
import app.tutora.ui.reports.Teacher
import app.tutora.ui.reports.earningsPlan
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfLayoutTest {
    private val w = testWords()
    private val teacher = Teacher("Victor", "victor@pix.com")

    /** The one document with the switches given, for Ana (AC-021-15). */
    private fun doc(
        r: app.tutora.domain.SubjectReport,
        parts: app.tutora.domain.DocParts,
    ) = app.tutora.ui.reports.documentPlan(
        r,
        parts,
        w,
        testInvoiceWords(),
        teacher,
        app.tutora.ui.reports
            .PixPlace("Recife"),
        app.tutora.ui.reports
            .Recipient("Ana Souza"),
        "Aluna",
        "Matemática · R$ 120,00/h",
        false,
        null,
        LocalDate(2026, 10, 26),
        "26/10/2026, 18:45",
    )

    @Test
    fun `AC-010-9 the PDF has the teacher and period, the classes, payments, totals and numbered pages`() {
        val classes =
            (1..40).map { i ->
                ReportClass("l$i", LocalDate(2026, 10, 1 + i % 26), 60, 12_000, "Online", false, 0, if (i <= 2) 12_000 else 0)
            }
        val r =
            subjectReport(
                LocalDate(2026, 10, 1),
                LocalDate(2026, 10, 26),
                classes,
                listOf(ReportPayment(LocalDate(2026, 10, 16), PayMethod.PIX, 54_000, listOf(LocalDate(2026, 10, 5)))),
            )
        val plan = doc(r, app.tutora.domain.DocParts(pix = false, payments = true))
        assertTrue(plan.subtitle.startsWith("Prof. Victor"))
        assertTrue(plan.subtitle.endsWith("01/10/2026 a 26/10/2026"))
        assertEquals("Pagamentos recebidos", plan.sections.single().title)
        assertEquals(listOf("Aulas", "Total", "Pago", "A receber"), plan.stats.map { it.label })
        val pages = pdfPages(plan)
        assertEquals(2, pages.size)
        assertTrue(pages.last().footer.endsWith("página 2 de 2"))
        assertTrue(pages.first().first && pages.last().last)
    }

    @Test
    fun `AC-010-14 the earnings PDF has Ganhos' totals, the table by source and the six months`() {
        val ana = PayerRef(PayerKind.STUDENT, "ana")
        val v =
            EarningsView(
                Summary(12_000, 8_000, 4_000, 0, 0),
                listOf(MonthPoint(LocalDate(2026, 10, 1), 12_000, 8_000)),
                listOf(Source(ana, "Ana Souza", 12_000)),
                listOf(EarnedClass("l", LocalDate(2026, 10, 22), EarningsType.PRIVATE, ana, "Ana Souza", 12_000, 0, 0, 90)),
            )
        val plan = earningsPlan(v, w, teacher, "10/2026 · Tudo", EarningsParts(), { 4_000 }, "26/10/2026, 18:45")
        assertEquals(
            listOf("Aulas" to "1 · 1h30", "Ganho" to "R$ 120,00", "Recebido" to "R$ 80,00", "A receber" to "R$ 40,00"),
            plan.stats.map {
                it.label to
                    it.value
            },
        )
        assertEquals(listOf("Ana Souza", "1", "1h30", "R$ 120,00", "R$ 40,00"), plan.rows.single().cells)
        assertEquals("Últimos 6 meses", plan.trendTitle)
        assertEquals(listOf("M10"), plan.trend.map { it.label })
    }

    @Test
    fun `AC-020-10 AC-021-15 the Pix block has the full key, the name and a QR code of the copia e cola, on page one before the classes`() {
        val owed = (1..14).map { i -> ReportClass("l$i", LocalDate(2026, 10, i), 60, 12_000, "Ana", false, 0, 12_000) }
        val r = subjectReport(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31), owed, emptyList())
        val plan = doc(r, app.tutora.domain.DocParts())
        val pix = plan.payPix!!
        assertEquals("victor@pix.com" to "Victor", pix.key to pix.name)
        assertEquals(app.tutora.domain.pixPayload("victor@pix.com", "Victor", "Recife", 168_000), pix.code)
        assertEquals(
            app.tutora.adapters.export
                .qrMatrix(pix.code!!)
                .size,
            pix.qr!!.size,
        )
        // The summary tiles, the amount due and the Pix box come first on page one, so fewer classes fit under them (AC-021-15).
        assertEquals(listOf("Total do período", "Já pago", "Valor a pagar"), plan.stats.map { it.label })
        assertEquals(emptyList<Any>(), plan.due?.lines)
        val pages = pdfPages(plan)
        assertEquals(listOf(6, 8), pages.map { it.rows.count() })
    }

    @Test
    fun `AC-021-7 the comprovantes add one page per image and per PDF page, and the footer counts them`() {
        val r = subjectReport(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31), emptyList(), emptyList())
        val plan =
            doc(r, app.tutora.domain.DocParts(pix = false)).copy(
                receiptsTitle = "Comprovantes",
                receipts =
                    listOf(
                        app.tutora.adapters.export
                            .PdfReceipt("16/10/2026 · Pix · R$ 540,00", "/a.jpg", false),
                        app.tutora.adapters.export
                            .PdfReceipt("16/10/2026 · Pix · R$ 540,00", "/b.pdf", true, 2),
                    ),
            )
        assertEquals(
            3,
            app.tutora.adapters.export
                .receiptPageCount(plan),
        )
        assertEquals("Gerado pela Tutora em 26/10/2026, 18:45 · página 1 de 4", pdfPages(plan).single().footer)
    }
}
