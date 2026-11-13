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
        shortMonth = { d: LocalDate -> "M${d.month.ordinal + 1}" },
        compact = { c -> "R$ ${c / 100}" },
        sheetClasses = "Aulas",
        sheetPayments = "Pagamentos",
        sheetSummary = "Resumo",
        sheetSources = "Por aluno e local",
        method = "Forma",
        amount = "Valor",
        receiptColumn = "Comprovante",
        yes = "Sim",
        no = "Não",
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

/** Portuguese words of the Fatura and the detailed report, without Android resources. */
fun testInvoiceWords() =
    app.tutora.ui.reports.InvoiceWords(
        invoiceTitle = "Fatura",
        detailTitle = "Relatório detalhado",
        to = "Para:",
        guardianOf = "responsável por %1\$s",
        madeOn = "Emitida em %1\$s",
        name = "Nome",
        dayCol = "Dia",
        scheduled = "Horário",
        ends = "Término",
        inClass = "Em aula",
        place = "Local",
        travel = "Deslocamento",
        charge = "Cobrança extra",
        subtotal = "Subtotal",
        salary = "Salário",
        periodTotal = "Total do período",
        alreadyPaid = "Já pago",
        toPay = "Valor a pagar",
        settled = "Pago, obrigado",
        dueOn = "Vencimento: %1\$s",
        payWithPix = "Pague com Pix",
        copyPaste = "Pix copia e cola",
        lessonsValue = "Aulas (valor)",
        extraCharges = "Cobranças extras",
        notes = "Observações",
        hello = "Olá!",
        invoiceOf = "Fatura de %1\$s",
        totalToPay = "Total a pagar: %1\$s",
        until = "até %1\$s",
        pixWithName = "Pix: %1\$s (%2\$s)",
        thanks = "Obrigado!",
        settledText = "Tudo pago neste período. Obrigado!",
        weekday = { d ->
            val names = listOf("seg", "ter", "qua", "qui", "sex", "sáb", "dom")
            "${names[d.dayOfWeek.ordinal]} ${d.day.toString().padStart(2, '0')}/${(d.month.ordinal + 1).toString().padStart(2, '0')}"
        },
        time = { t -> "${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}" },
        builtinPlaces = mapOf(app.tutora.domain.BuiltinPlace.ONLINE.id to "Online / Remoto"),
    )
