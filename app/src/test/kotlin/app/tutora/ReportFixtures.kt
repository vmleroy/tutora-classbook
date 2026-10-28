package app.tutora

import app.tutora.adapters.export.Exports
import app.tutora.adapters.export.PdfColors
import app.tutora.adapters.export.PdfPlan
import app.tutora.domain.PayMethod
import app.tutora.ui.reports.ReportWords
import kotlinx.datetime.LocalDate

/** Portuguese words and formats for the report builders, without Android resources. */
fun testWords() =
    ReportWords(
        brand = "TUTORA · CONTROLE DE AULAS",
        subjectTitle = "Relatório de aulas",
        earningsTitle = "Relatório de ganhos",
        teacher = "Prof. %1\$s",
        range = "%1\$s a %2\$s",
        classes = "Aulas",
        total = "Total",
        paid = "Pago",
        toReceive = "A receber",
        earned = "Ganho",
        received = "Recebido",
        tableClasses = "Aulas no período",
        tableDays = "Dias de aula no período (salário mensal)",
        date = "Data",
        duration = "Duração",
        where = "Local ou aluno",
        value = "Valor",
        situation = "Situação",
        statusPaid = "Pago",
        statusOwed = "A receber",
        statusCharged = "Cancelada · cobrada",
        statusBonus = "bônus",
        paymentsTitle = "Pagamentos recebidos",
        pending = "Valor pendente: %1\$s",
        pix = "Pix: %1\$s",
        generated = "Gerado pela Tutora em %1\$s",
        page = "página %1\$d de %2\$d",
        classCount = { n -> if (n == 1) "1 aula" else "$n aulas" },
        methodName = { m -> if (m == PayMethod.PIX) "Pix" else m.name },
        money = { c -> "R$ ${c / 100},${(c % 100).toString().padStart(2, '0')}" },
        day = { d -> "${d.day.toString().padStart(2, '0')}/${(d.month.ordinal + 1).toString().padStart(2, '0')}/${d.year}" },
        dayMonth = { d -> "${d.day.toString().padStart(2, '0')}/${(d.month.ordinal + 1).toString().padStart(2, '0')}" },
        hours = { m -> "${m / 60}h${if (m % 60 > 0) (m % 60).toString().padStart(2, '0') else ""}" },
        minutes = { m -> "$m min" },
        bySource = "Por aluno e por local",
        origin = "Origem",
        type = "Tipo",
        trend = "Últimos 6 meses",
        classList = "Lista de aulas",
        month = { d: LocalDate -> "${d.month.ordinal + 1}/${d.year}" },
        sheetClasses = "Aulas",
        sheetPayments = "Pagamentos",
        sheetSummary = "Resumo",
        sheetSources = "Por aluno e local",
        method = "Forma",
        amount = "Valor",
    )

/** Keeps what was shared and copied; a PDF is its plan's text, so tests can read it. */
class FakeExports : Exports {
    val shared = mutableListOf<Pair<String, ByteArray>>()
    val copied = mutableListOf<String>()

    override fun pdf(
        plan: PdfPlan,
        colors: PdfColors,
    ): ByteArray = plan.toString().toByteArray()

    override fun share(
        fileName: String,
        mime: String,
        bytes: ByteArray,
    ) {
        shared += fileName to bytes
    }

    override fun copy(text: String) {
        copied += text
    }
}
