package app.tutora

import app.tutora.adapters.export.Cell
import app.tutora.adapters.export.PdfRowKind
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.ReportClass
import app.tutora.domain.SubjectReport
import app.tutora.domain.invoiceOf
import app.tutora.domain.subjectReport
import app.tutora.ui.reports.PixPlace
import app.tutora.ui.reports.Recipient
import app.tutora.ui.reports.Teacher
import app.tutora.ui.reports.detailPlan
import app.tutora.ui.reports.detailSheets
import app.tutora.ui.reports.invoicePlan
import app.tutora.ui.reports.invoiceText
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class InvoiceDocsTest {
    private val w = testWords()
    private val iw = testInvoiceWords()
    private val teacher = Teacher("Victor Le Roy", "victor@gmail.com")
    private val city = PixPlace("Recife")
    private val made = LocalDate(2026, 11, 13)
    private val stamp = "13/11/2026, 10:00"
    private val ana = Recipient("Ana Souza", "Maria Souza")

    private fun cls(
        day: Int,
        left: Long = 0,
        who: String = "Ana Souza",
        travel: Long = 0,
        notes: String? = null,
    ) = ReportClass(
        "l$day$who",
        LocalDate(2026, 10, day),
        60,
        12_000 + travel,
        who,
        false,
        0,
        left,
        LocalTime(14, 0),
        travel,
        0,
        BuiltinPlace.ONLINE.id,
        null,
        notes,
    )

    private fun report(vararg c: ReportClass): SubjectReport =
        subjectReport(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31), c.toList(), emptyList())

    private val owing = report(cls(5), cls(19, left = 12_000), cls(22, left = 12_000))

    private fun fatura(
        r: SubjectReport = owing,
        due: LocalDate? = null,
    ) = invoicePlan(invoiceOf(r, false), w, iw, teacher, city, ana, due, made, stamp)

    @Test
    fun `AC-020-3 each class shows its name, day, scheduled time, time in class and value`() {
        val row = fatura().rows.first().cells
        assertEquals(listOf("Ana Souza", "seg 05/10", "14:00", "60 min", "R$ 120,00"), row.take(5))
        val detail = detailPlan(owing, w, iw, teacher, ana, "Aluna", null, false, true, made, stamp).rows.first().cells
        assertEquals(listOf("Ana Souza", "seg 05/10", "14:00–15:00", "60 min"), detail.take(4))
    }

    @Test
    fun `AC-020-6 the Fatura's header has its title, the teacher, the date made, who it is for and the period`() {
        val plan = fatura()
        assertEquals("Fatura", plan.title)
        assertEquals("Prof. Victor Le Roy · Emitida em 13/11/2026 · 01/10/2026 a 31/10/2026", plan.subtitle)
        assertEquals("Para:", plan.subjectLabel)
        assertEquals("Maria Souza", plan.subjectName)
        assertEquals("responsável por Ana Souza", plan.subjectLine)
    }

    @Test
    fun `AC-020-7 under the table, Total do periodo, Ja pago and Valor a pagar`() {
        val due = fatura().due!!
        assertEquals(listOf("Total do período" to "R$ 360,00", "Já pago" to "R$ 120,00"), due.lines)
        assertEquals("Valor a pagar" to "R$ 240,00", due.label to due.amount)
        assertEquals(listOf("Pago", "A receber", "A receber"), fatura().rows.map { it.cells.last() })
    }

    @Test
    fun `AC-020-9 an optional Vencimento shows on the PDF and in the text`() {
        assertEquals("Vencimento: 05/11/2026", fatura(due = LocalDate(2026, 11, 5)).due?.note)
        assertNull(fatura().due?.note)
        val text = invoiceText(invoiceOf(owing, false), w, iw, teacher, city, "outubro 2026", LocalDate(2026, 11, 5))
        assertTrue(text.contains("Total a pagar: R$ 240,00 (até 05/11)"))
    }

    @Test
    fun `AC-020-12 with nothing owed the Fatura says Pago, obrigado, with no due date and no Pix`() {
        val plan = fatura(report(cls(5), cls(12)), due = LocalDate(2026, 11, 5))
        assertEquals("Pago, obrigado", plan.due?.label)
        assertTrue(plan.due!!.settled)
        assertNull(plan.due.note)
        assertNull(plan.payPix)
        val text = invoiceText(invoiceOf(report(cls(5)), false), w, iw, teacher, city, "outubro 2026", null)
        assertTrue(text.endsWith("Tudo pago neste período. Obrigado!"))
    }

    @Test
    fun `AC-020-14 the copied Fatura has the classes still to pay, the total, the Pix key and the copia e cola`() {
        val text = invoiceText(invoiceOf(owing, false), w, iw, teacher, city, "outubro 2026", null)
        val lines = text.lines()
        assertEquals(
            listOf("Olá!", "Fatura de outubro 2026", "19/10 · 14:00 · 60 min · R$ 120,00", "22/10 · 14:00 · 60 min · R$ 120,00"),
            lines.take(4),
        )
        assertTrue(text.contains("Total a pagar: R$ 240,00"))
        assertTrue(text.contains("Pix: victor@gmail.com (Victor Le Roy)"))
        assertTrue(text.contains("Pix copia e cola:\n000201"))
        assertFalse(text.contains("05/10"))
    }

    @Test
    fun `AC-020-15 the detailed report adds place, travel and the notes under a class when asked`() {
        val r = report(cls(5, travel = 3_000, notes = "Revisão de frações"))
        val with = detailPlan(r, w, iw, teacher, Recipient("Ana Souza"), "Aluna", null, false, true, made, stamp)
        assertEquals("Relatório detalhado", with.title)
        assertEquals(listOf("Online / Remoto", "R$ 30,00", "", "R$ 150,00"), with.rows[0].cells.subList(4, 8))
        assertEquals(PdfRowKind.NOTE to listOf("Revisão de frações"), with.rows[1].kind to with.rows[1].cells)
        val without = detailPlan(r, w, iw, teacher, Recipient("Ana Souza"), "Aluna", null, false, false, made, stamp)
        assertEquals(1, without.rows.size)
    }

    @Test
    fun `AC-020-19 the detailed Excel has every column, with dates and amounts as numbers`() {
        val sheets = detailSheets(report(cls(5, travel = 3_000, notes = "ok")), w, iw, true)
        val aulas = sheets.first()
        assertEquals(
            listOf(
                "Nome",
                "Data",
                "Horário",
                "Término",
                "Em aula",
                "Local",
                "Deslocamento",
                "Cobrança extra",
                "Valor",
                "Situação",
                "Observações",
            ),
            aulas.header,
        )
        val row = aulas.rows.single()
        assertEquals(Cell.Day(2026, 10, 5), row[1])
        assertEquals(Cell.Money(3_000), row[6])
        assertEquals(Cell.Money(15_000), row[8])
        assertEquals("Pagamentos", sheets[1].name)
    }

    @Test
    fun `AC-020-27 every word of the Fatura and the detailed report is in English and Portuguese`() {
        fun names(path: String) =
            Regex("<string name=\"((?:inv|stu)_[A-Za-z]+|rep_settingsCity[A-Za-z]*)\">([^<]*)<")
                .findAll(File(path).readText())
                .associate { it.groupValues[1] to it.groupValues[2] }
        val en = names("src/main/res/values/strings_features.xml")
        val pt = names("src/main/res/values-pt-rBR/strings_features.xml")
        assertTrue(en.size >= 50)
        assertEquals(en.keys, pt.keys)
        assertEquals("Amount due" to "Valor a pagar", en["inv_toPay"] to pt["inv_toPay"])
    }
}
