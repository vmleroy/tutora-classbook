package app.tutora

import app.tutora.adapters.export.Xlsx
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
import app.tutora.ui.reports.earningsSheets
import app.tutora.ui.reports.subjectSheets
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class XlsxWriterTest {
    private fun unzip(bytes: ByteArray): Map<String, String> {
        val out = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                out[e.name] = z.readBytes().toString(Charsets.UTF_8)
            }
        }
        return out
    }

    @Test
    fun `AC-010-10 the report's Excel has Aulas and Pagamentos, with dates as dates and amounts as numbers`() {
        val oct22 = LocalDate(2026, 10, 22)
        val r =
            subjectReport(
                LocalDate(2026, 10, 1),
                LocalDate(2026, 10, 26),
                listOf(ReportClass("l", oct22, 60, 12_050, "Online", false, 0, 0)),
                listOf(ReportPayment(LocalDate(2026, 10, 23), PayMethod.PIX, 12_050, listOf(oct22))),
            )
        val files = unzip(Xlsx.write(subjectSheets(r, testWords())))
        assertTrue(files.getValue("xl/workbook.xml").contains("name=\"Aulas\""))
        assertTrue(files.getValue("xl/workbook.xml").contains("name=\"Pagamentos\""))
        val aulas = files.getValue("xl/worksheets/sheet1.xml")
        // 22/10/2026 is Excel day 46317; R$ 120,50 is the number 120.50; the minutes are a number.
        assertTrue(aulas.contains("<c r=\"A2\" s=\"1\"><v>46317</v></c>"))
        assertTrue(aulas.contains("<c r=\"B2\"><v>60</v></c>"))
        assertTrue(aulas.contains("<c r=\"D2\" s=\"2\"><v>120.50</v></c>"))
        assertTrue(files.containsKey("[Content_Types].xml") && files.containsKey("xl/styles.xml"))
    }

    @Test
    fun `AC-010-15 the earnings Excel has Resumo and a sheet for each part chosen`() {
        val ana = PayerRef(PayerKind.STUDENT, "ana")
        val v =
            EarningsView(
                Summary(12_000, 12_000, 0, 0, 0),
                listOf(MonthPoint(LocalDate(2026, 10, 1), 12_000, 12_000)),
                listOf(Source(ana, "Ana Souza", 12_000)),
                listOf(EarnedClass("l", LocalDate(2026, 10, 22), EarningsType.PRIVATE, ana, "Ana Souza", 12_000, 0, 0, 60)),
            )
        val sheets = earningsSheets(v, testWords(), EarningsParts(bySource = true, classes = false, payments = true)) { 0 }
        assertEquals(listOf("Resumo", "Por aluno e local", "Pagamentos"), sheets.map { it.name })
        val files = unzip(Xlsx.write(sheets))
        assertTrue(files.getValue("xl/worksheets/sheet1.xml").contains("<v>120.00</v>"))
    }
}
