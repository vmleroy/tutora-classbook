package app.tutora.ui.reports

import app.tutora.adapters.export.Cell
import app.tutora.adapters.export.PdfPlan
import app.tutora.adapters.export.PdfRow
import app.tutora.adapters.export.PdfSection
import app.tutora.adapters.export.PdfTone
import app.tutora.adapters.export.Sheet
import app.tutora.domain.Cents
import app.tutora.domain.EarningsView
import app.tutora.domain.PayMethod
import app.tutora.domain.SubjectReport
import kotlinx.datetime.LocalDate

// What a report says, as PDF plans, Excel sheets and text (spec 010). Every word and format comes
// in through ReportWords, so these are plain functions the tests run on the JVM.

/** The words and formats of the app language, resolved from resources by the screen. */
data class ReportWords(
    val brand: String,
    val subjectTitle: String,
    val earningsTitle: String,
    /** "Prof. %1$s". */
    val teacher: String,
    /** "%1$s a %2$s". */
    val range: String,
    val classes: String,
    val total: String,
    val paid: String,
    val toReceive: String,
    val earned: String,
    val received: String,
    val tableClasses: String,
    val tableDays: String,
    val date: String,
    val duration: String,
    val where: String,
    val value: String,
    val situation: String,
    val statusPaid: String,
    val statusOwed: String,
    val statusCharged: String,
    val statusBonus: String,
    val paymentsTitle: String,
    /** "Valor pendente: %1$s". */
    val pending: String,
    /** "Pix: %1$s". */
    val pix: String,
    /** "Gerado pela Tutora em %1$s". */
    val generated: String,
    /** "página %1$d de %2$d". */
    val page: String,
    /** "%1$d aulas" for a count. */
    val classCount: (Int) -> String,
    val methodName: (PayMethod) -> String,
    val money: (Cents) -> String,
    val day: (LocalDate) -> String,
    val dayMonth: (LocalDate) -> String,
    /** "6h30" (or "06:30" with Durações em HH:MM). */
    val hours: (Int) -> String,
    val minutes: (Int) -> String,
    // The earnings report.
    val bySource: String,
    val origin: String,
    val type: String,
    val trend: String,
    val classList: String,
    val month: (LocalDate) -> String,
    /** Sheet names: "Aulas", "Pagamentos", "Resumo", "Por aluno e local". */
    val sheetClasses: String,
    val sheetPayments: String,
    val sheetSummary: String,
    val sheetSources: String,
    val method: String,
    val amount: String,
)

/** The teacher's name and Pix key from Ajustes; empty when not set (AC-010-16). */
data class Teacher(
    val name: String,
    val pix: String,
)

private fun ReportWords.subtitle(
    t: Teacher,
    from: LocalDate,
    to: LocalDate,
): String =
    listOfNotNull(
        t.name.takeIf { it.isNotBlank() }?.let { teacher.format(it) },
        range.format(day(from), day(to)),
    ).joinToString(" · ")

private fun ReportWords.status(c: app.tutora.domain.ReportClass): Pair<String, PdfTone> =
    when {
        c.charged && c.paid -> "$statusCharged · $statusPaid" to PdfTone.PAID
        c.charged -> "$statusCharged · $statusOwed" to PdfTone.OWED
        c.paid && c.bonus > 0 -> "$statusPaid · $statusBonus" to PdfTone.PAID
        c.paid -> statusPaid to PdfTone.PAID
        else -> statusOwed to PdfTone.OWED
    }

/**
 * A student's, group's or place's report as a PDF plan: header with the teacher's name and the
 * period, the subject, the totals, the classes (or a school's days), payments received, what is
 * pending and the Pix key (AC-010-9).
 */
fun subjectPlan(
    r: SubjectReport,
    w: ReportWords,
    t: Teacher,
    subjectLabel: String,
    subjectName: String,
    subjectLine: String?,
    showWho: Boolean,
    generatedAt: String,
): PdfPlan {
    val salary = r.days != null
    val rows =
        if (salary) {
            r.days.orEmpty().map { PdfRow(listOf(w.day(it.day), w.classCount(it.classes)), PdfTone.INK) }
        } else {
            r.classes.map { c ->
                val (status, tone) = w.status(c)
                PdfRow(
                    listOfNotNull(w.day(c.day), w.minutes(c.minutes), c.who.takeIf { showWho }, w.money(c.value), status),
                    tone,
                )
            }
        }
    val columns =
        if (salary) {
            listOf(w.date, w.tableClasses)
        } else {
            listOfNotNull(w.date, w.duration, w.where.takeIf { showWho }, w.value, w.situation)
        }
    val owedDays = r.classes.filter { !it.paid }.map { w.dayMonth(it.day) }
    return PdfPlan(
        brand = w.brand,
        title = w.subjectTitle,
        subtitle = w.subtitle(t, r.from, r.to),
        subjectLabel = subjectLabel,
        subjectName = subjectName,
        subjectLine = subjectLine,
        stats =
            listOf(
                w.classes to "${r.count} · ${w.hours(r.minutes)}",
                w.total to w.money(r.total),
                w.paid to w.money(r.paid),
                w.toReceive to w.money(r.left),
            ),
        tableTitle = if (salary) w.tableDays else w.tableClasses,
        columns = columns,
        rows = rows,
        totalRow = if (salary) null else listOfNotNull(w.total, w.hours(r.minutes), "".takeIf { showWho }, w.money(r.total), ""),
        sections =
            listOfNotNull(
                r.payments.takeIf { it.isNotEmpty() }?.let { ps ->
                    PdfSection(
                        w.paymentsTitle,
                        ps.map { p ->
                            listOf(w.day(p.day), w.methodName(p.method), w.classCount(p.classDays.size)).joinToString(" · ") to
                                w.money(p.cents)
                        },
                    )
                },
            ),
        notes =
            listOfNotNull(
                r.left.takeIf { it > 0 }?.let {
                    w.pending.format(w.money(it)) + if (owedDays.isEmpty()) "" else " · " + owedDays.joinToString(", ")
                },
                t.pix.takeIf { it.isNotBlank() }?.let { w.pix.format(it) },
            ),
        footer = w.generated.format(generatedAt) + " · " + w.page,
    )
}

/** The report's Excel file: "Aulas" and "Pagamentos", dates as dates and amounts as numbers (AC-010-10). */
fun subjectSheets(
    r: SubjectReport,
    w: ReportWords,
): List<Sheet> =
    listOf(
        Sheet(
            w.sheetClasses,
            listOf(w.date, w.duration, w.where, w.value, w.situation),
            r.classes.map { c ->
                listOf(
                    Cell.Day(c.day.year, c.day.month.ordinal + 1, c.day.day),
                    Cell.Num(c.minutes.toLong()),
                    Cell.Text(c.who),
                    Cell.Money(c.value),
                    Cell.Text(w.status(c).first),
                )
            },
        ),
        Sheet(
            w.sheetPayments,
            listOf(w.date, w.method, w.amount),
            r.payments.map { p ->
                listOf(Cell.Day(p.day.year, p.day.month.ordinal + 1, p.day.day), Cell.Text(w.methodName(p.method)), Cell.Money(p.cents))
            },
        ),
    )

/** Copiar como texto: a short message for WhatsApp (AC-010-12). */
fun subjectText(
    r: SubjectReport,
    w: ReportWords,
    t: Teacher,
    subjectName: String,
): String {
    val owedDays = r.classes.filter { !it.paid }.map { w.dayMonth(it.day) }
    return listOfNotNull(
        "$subjectName · ${w.range.format(w.day(r.from), w.day(r.to))}",
        listOf(
            w.classCount(r.count),
            w.money(r.total),
            "${w.paid.lowercase()} ${w.money(r.paid)}",
            "${w.toReceive.lowercase()} ${w.money(r.left)}",
        ).joinToString(" · "),
        owedDays.takeIf { it.isNotEmpty() }?.let { "${w.toReceive}: ${it.joinToString(", ")}" },
        t.pix.takeIf { it.isNotBlank() }?.let { w.pix.format(it) },
        t.name.takeIf { it.isNotBlank() }?.let { "— ${w.teacher.format(it)}" },
    ).joinToString("\n")
}

/** What the earnings report includes (AC-010-13). */
data class EarningsParts(
    val bySource: Boolean = true,
    val classes: Boolean = true,
    val payments: Boolean = false,
)

private data class SourceRow(
    val name: String,
    val count: Int,
    val minutes: Int,
    val earned: Cents,
    val owed: Cents,
)

private fun sourceRows(
    v: EarningsView,
    owes: (app.tutora.domain.PayerRef) -> Cents,
): List<SourceRow> =
    v.sources.map { s ->
        val mine = v.classes.filter { it.payer == s.payer }
        SourceRow(s.name, mine.size, mine.sumOf { it.minutes }, s.cents, owes(s.payer).coerceAtLeast(0))
    }

/**
 * The earnings report from Ganhos: totals, the table by student, group, place and school, the six
 * months, then the parts chosen; the numbers are Ganhos' own (AC-010-14).
 */
fun earningsPlan(
    v: EarningsView,
    w: ReportWords,
    t: Teacher,
    periodLine: String,
    parts: EarningsParts,
    owes: (app.tutora.domain.PayerRef) -> Cents,
    generatedAt: String,
): PdfPlan {
    val rows = if (parts.bySource) sourceRows(v, owes) else emptyList()
    val minutes = v.classes.sumOf { it.minutes }
    return PdfPlan(
        brand = w.brand,
        title = w.earningsTitle,
        subtitle = listOfNotNull(t.name.takeIf { it.isNotBlank() }?.let { w.teacher.format(it) }, periodLine).joinToString(" · "),
        subjectLabel = null,
        subjectName = null,
        subjectLine = null,
        stats =
            listOf(
                w.classes to "${v.classes.size} · ${w.hours(minutes)}",
                w.earned to w.money(v.summary.earned),
                w.received to w.money(v.summary.received),
                w.toReceive to w.money(v.summary.toReceive),
            ),
        tableTitle = w.bySource,
        columns = listOf(w.origin, w.classes, w.duration, w.earned, w.toReceive),
        rows =
            rows.map {
                PdfRow(
                    listOf(
                        it.name,
                        it.count.toString(),
                        w.hours(it.minutes),
                        w.money(it.earned),
                        if (it.owed >
                            0
                        ) {
                            w.money(it.owed)
                        } else {
                            "—"
                        },
                    ),
                    if (it.owed > 0) PdfTone.OWED else PdfTone.MUTED,
                )
            },
        totalRow =
            if (rows.isEmpty()) {
                null
            } else {
                listOf(w.total, v.classes.size.toString(), w.hours(minutes), w.money(v.summary.earned), w.money(v.summary.toReceive))
            },
        sections =
            listOfNotNull(
                PdfSection(w.trend, v.trend.map { w.month(it.month) to w.money(it.earned) }),
                if (parts.classes) {
                    PdfSection(
                        w.classList,
                        v.classes.sortedByDescending { it.day }.map { c ->
                            "${w.day(c.day)} · ${c.payerName} · ${w.minutes(c.minutes)}" to w.money(c.earned)
                        },
                    )
                } else {
                    null
                },
                if (parts.payments) {
                    PdfSection(
                        w.paymentsTitle,
                        v.received.sortedByDescending { it.day }.map {
                            w.day(it.day) to
                                w.money(it.cents)
                        },
                    )
                } else {
                    null
                },
            ),
        notes = emptyList(),
        footer = w.generated.format(generatedAt) + " · " + w.page,
    )
}

/** The earnings Excel: "Resumo" with the totals and six months, then one sheet per part chosen (AC-010-15). */
fun earningsSheets(
    v: EarningsView,
    w: ReportWords,
    parts: EarningsParts,
    owes: (app.tutora.domain.PayerRef) -> Cents,
): List<Sheet> {
    val summary =
        Sheet(
            w.sheetSummary,
            listOf("", w.amount),
            listOf(
                listOf(Cell.Text(w.earned), Cell.Money(v.summary.earned)),
                listOf(Cell.Text(w.received), Cell.Money(v.summary.received)),
                listOf(Cell.Text(w.toReceive), Cell.Money(v.summary.toReceive)),
            ) + v.trend.map { listOf(Cell.Text(w.month(it.month)), Cell.Money(it.earned)) },
        )
    return listOfNotNull(
        summary,
        if (parts.bySource) {
            Sheet(
                w.sheetSources,
                listOf(w.origin, w.classes, w.duration, w.earned, w.toReceive),
                sourceRows(v, owes).map {
                    listOf(
                        Cell.Text(it.name),
                        Cell.Num(it.count.toLong()),
                        Cell.Num(it.minutes.toLong()),
                        Cell.Money(it.earned),
                        Cell.Money(it.owed),
                    )
                },
            )
        } else {
            null
        },
        if (parts.classes) {
            Sheet(
                w.sheetClasses,
                listOf(w.date, w.origin, w.duration, w.earned),
                v.classes.sortedByDescending { it.day }.map { c ->
                    listOf(
                        Cell.Day(c.day.year, c.day.month.ordinal + 1, c.day.day),
                        Cell.Text(c.payerName),
                        Cell.Num(c.minutes.toLong()),
                        Cell.Money(c.earned),
                    )
                },
            )
        } else {
            null
        },
        if (parts.payments) {
            Sheet(
                w.sheetPayments,
                listOf(w.date, w.amount),
                v.received.sortedByDescending { it.day }.map {
                    listOf(
                        Cell.Day(it.day.year, it.day.month.ordinal + 1, it.day.day),
                        Cell.Money(it.cents),
                    )
                },
            )
        } else {
            null
        },
    )
}
