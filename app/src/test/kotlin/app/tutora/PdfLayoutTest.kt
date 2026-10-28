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
import app.tutora.ui.reports.subjectPlan
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfLayoutTest {
    private val w = testWords()
    private val teacher = Teacher("Victor", "victor@pix.com")

    @Test
    fun `AC-010-9 the PDF has the teacher and period, the classes, payments, pending, Pix and numbered pages`() {
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
        val plan = subjectPlan(r, w, teacher, "Aluna", "Ana Souza", "Matemática · R$ 120,00/h", false, "26/10/2026, 18:45")
        assertEquals("Prof. Victor · 01/10/2026 a 26/10/2026", plan.subtitle)
        assertTrue(plan.sections.single().title == "Pagamentos recebidos")
        assertEquals("Valor pendente: R$ 240,00", plan.pending?.first)
        assertEquals("Pix: victor@pix.com", plan.pix)
        val pages = pdfPages(plan)
        assertEquals(2, pages.size)
        assertEquals("Gerado pela Tutora em 26/10/2026, 18:45 · página 2 de 2", pages.last().footer)
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
}
