package app.tutora.ui.reports

import app.tutora.adapters.export.Cell
import app.tutora.adapters.export.PdfDue
import app.tutora.adapters.export.PdfPix
import app.tutora.adapters.export.PdfPlan
import app.tutora.adapters.export.PdfRow
import app.tutora.adapters.export.PdfRowKind
import app.tutora.adapters.export.PdfSection
import app.tutora.adapters.export.PdfStat
import app.tutora.adapters.export.PdfTone
import app.tutora.adapters.export.Sheet
import app.tutora.adapters.export.qrMatrix
import app.tutora.domain.Invoice
import app.tutora.domain.ReportClass
import app.tutora.domain.SubjectReport
import app.tutora.domain.byPerson
import app.tutora.domain.chronological
import app.tutora.domain.detailTotals
import app.tutora.domain.pixPayload
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

// The Fatura and the Relatório detalhado as PDF plans, text and Excel sheets (spec 020). Like
// ReportDocs, every word comes in through the words classes, so these run on the JVM in tests.

/** The words of the Fatura and the detailed report, in the app language (AC-020-27). */
data class InvoiceWords(
    val invoiceTitle: String,
    val detailTitle: String,
    /** "Para:". */
    val to: String,
    /** "responsável por %1$s". */
    val guardianOf: String,
    /** "Emitida em %1$s". */
    val madeOn: String,
    val name: String,
    val dayCol: String,
    val scheduled: String,
    /** The scheduled end, a column of the Excel. */
    val ends: String,
    val inClass: String,
    val place: String,
    val travel: String,
    val charge: String,
    val subtotal: String,
    val salary: String,
    val periodTotal: String,
    val alreadyPaid: String,
    val toPay: String,
    /** "Pago, obrigado". */
    val settled: String,
    /** "Vencimento: %1$s". */
    val dueOn: String,
    val payWithPix: String,
    val copyPaste: String,
    val lessonsValue: String,
    val extraCharges: String,
    val notes: String,
    // The copied message.
    val hello: String,
    /** "Fatura de %1$s". */
    val invoiceOf: String,
    /** "Total a pagar: %1$s". */
    val totalToPay: String,
    /** "até %1$s". */
    val until: String,
    /** "Pix: %1$s (%2$s)". */
    val pixWithName: String,
    val thanks: String,
    /** "Tudo pago neste período. Obrigado!". */
    val settledText: String,
    /** "seg 05/10". */
    val weekday: (LocalDate) -> String,
    val time: (LocalTime) -> String,
    /** A built-in place's name in the app language, by its id. */
    val builtinPlaces: Map<String, String>,
)

/** Who a document is for: the student, a group or a place, and a student's Responsável (AC-020-6, 17). */
data class Recipient(
    val name: String,
    val guardian: String? = null,
)

/** The teacher's city, needed with the name and key for the Pix QR code (AC-020-18). */
data class PixPlace(
    val city: String,
)

private fun InvoiceWords.placeOf(c: ReportClass) = builtinPlaces[c.placeId] ?: c.placeName.orEmpty()

private fun ReportWords.statusOf(c: ReportClass): Pair<String, PdfTone> =
    when {
        c.charged && c.paid -> "$statusCharged · $statusPaid" to PdfTone.PAID
        c.charged -> "$statusCharged · $statusOwed" to PdfTone.OWED
        c.paid -> statusPaid to PdfTone.PAID
        else -> statusOwed to PdfTone.OWED
    }

/** Time spent in class; a cancelled class has none (AC-020-4, 5). */
private fun ReportWords.inClass(c: ReportClass) = if (c.charged) "—" else minutes(c.minutes)

private fun InvoiceWords.subtitle(
    w: ReportWords,
    t: Teacher,
    made: LocalDate,
    from: LocalDate,
    to: LocalDate,
) = listOfNotNull(
    t.name.takeIf { it.isNotBlank() }?.let { w.teacher.format(it) },
    madeOn.format(w.day(made)),
    w.range.format(w.day(from), w.day(to)),
).joinToString(" · ")

/** Rows by person (a place's documents) or one plain list, oldest first. */
private fun rowsOf(
    classes: List<ReportClass>,
    grouped: Boolean,
    row: (ReportClass) -> List<PdfRow>,
    subtotal: (List<ReportClass>) -> PdfRow,
): List<PdfRow> =
    if (grouped) {
        byPerson(classes).flatMap { g ->
            listOf(PdfRow(listOf(g.name), kind = PdfRowKind.HEADING)) + g.classes.flatMap(row) + subtotal(g.classes)
        }
    } else {
        chronological(classes).flatMap(row)
    }

/**
 * The Fatura: who it is for and the period, each class with its name, day, scheduled time, time in
 * class, value and situation, then Total do período, Já pago and Valor a pagar large, the
 * Vencimento when set, and the Pix block with its QR code (AC-020-3, 6 to 13).
 */
fun invoicePlan(
    inv: Invoice,
    w: ReportWords,
    iw: InvoiceWords,
    t: Teacher,
    place: PixPlace,
    to: Recipient,
    due: LocalDate?,
    made: LocalDate,
    generatedAt: String,
): PdfPlan {
    val grouped = inv.groups != null
    val rows =
        if (inv.salaryMonths.isNotEmpty()) {
            inv.salaryMonths.map { (m, cents) -> PdfRow(listOf(iw.salary, w.month(m), "", "", w.money(cents), w.statusPaid), PdfTone.PAID) }
        } else {
            rowsOf(
                inv.classes,
                grouped,
                { c ->
                    val (status, tone) = w.statusOf(c)
                    listOf(PdfRow(listOf(c.who, iw.weekday(c.day), iw.time(c.start), w.inClass(c), w.money(c.value), status), tone))
                },
                { cs ->
                    PdfRow(
                        listOf(
                            iw.subtotal,
                            w.classCount(cs.size),
                            "",
                            w.hours(cs.sumOf { it.minutes }),
                            w.money(cs.sumOf { it.value }),
                            "",
                        ),
                        kind = PdfRowKind.SUBTOTAL,
                    )
                },
            )
        }
    val code = if (inv.settled) null else pixPayload(t.pix, t.name, place.city, inv.toPay)
    return PdfPlan(
        brand = w.brand,
        title = iw.invoiceTitle,
        subtitle = iw.subtitle(w, t, made, inv.from, inv.to),
        subjectLabel = iw.to,
        subjectName = to.guardian ?: to.name,
        subjectLine = to.guardian?.let { iw.guardianOf.format(to.name) },
        stats = emptyList(),
        tableTitle = w.tableClasses,
        columns = listOf(iw.name, iw.dayCol, iw.scheduled, iw.inClass, w.value, w.situation),
        rightAligned = setOf(3, 4, 5),
        rows = rows,
        totalRow = null,
        sections = emptyList(),
        due =
            PdfDue(
                lines = listOfNotNull(iw.periodTotal to w.money(inv.total), (iw.alreadyPaid to w.money(inv.paid)).takeIf { inv.paid > 0 }),
                label = if (inv.settled) iw.settled else iw.toPay,
                amount = w.money(if (inv.settled) inv.paid else inv.toPay),
                note = due?.takeIf { !inv.settled }?.let { iw.dueOn.format(w.day(it)) },
                settled = inv.settled,
            ),
        payPix =
            t.pix.takeIf { it.isNotBlank() && !inv.settled }?.let { key ->
                PdfPix(iw.payWithPix, key.trim(), t.name, iw.copyPaste, code, code?.let(::qrMatrix))
            },
        weights = listOf(1.5f, 1f, 0.8f, 0.8f, 1f, 1.1f),
        footer = w.generated.format(generatedAt) + " · " + w.page,
    )
}

/**
 * Copiar como texto for the Fatura: a greeting, the period, each class still to pay, the total,
 * the due date, the Pix key with the name and the "copia e cola"; or that all is paid (AC-020-14).
 */
fun invoiceText(
    inv: Invoice,
    w: ReportWords,
    iw: InvoiceWords,
    t: Teacher,
    place: PixPlace,
    periodLabel: String,
    due: LocalDate?,
): String {
    val head = listOf(iw.hello, iw.invoiceOf.format(periodLabel))
    if (inv.settled) {
        return (head + "${w.classCount(inv.classes.size)} · ${w.money(inv.total)}" + iw.settledText).joinToString("\n")
    }
    val grouped = inv.groups != null
    val owed =
        chronological(inv.classes).filter { !it.paid }.map { c ->
            listOfNotNull(c.who.takeIf { grouped }, w.dayMonth(c.day), iw.time(c.start), w.inClass(c), w.money(c.left)).joinToString(" · ")
        }
    val total = iw.totalToPay.format(w.money(inv.toPay)) + (due?.let { " (${iw.until.format(w.dayMonth(it))})" } ?: "")
    val key = t.pix.trim()
    val pix =
        key.takeIf { it.isNotEmpty() }?.let {
            if (t.name.isNotBlank()) iw.pixWithName.format(it, t.name.trim()) else w.pix.format(it)
        }
    val code = pixPayload(t.pix, t.name, place.city, inv.toPay)
    return (
        head + owed + total +
            listOfNotNull(pix, code?.let { "${iw.copyPaste}:\n$it" }) + iw.thanks
    ).joinToString("\n")
}

/**
 * The Relatório detalhado: the totals with travel and extra charges, each class with its name,
 * day, scheduled start and end, time in class, place, travel, charge, value and situation, its
 * notes under it when asked, by student for a place, then payments received (AC-020-15, 16).
 */
fun detailPlan(
    r: SubjectReport,
    w: ReportWords,
    iw: InvoiceWords,
    t: Teacher,
    to: Recipient,
    subjectLabel: String,
    subjectLine: String?,
    grouped: Boolean,
    withNotes: Boolean,
    made: LocalDate,
    generatedAt: String,
): PdfPlan {
    val tt = detailTotals(r)
    val extras = tt.travel > 0 || tt.charges > 0
    val stats =
        listOfNotNull(
            PdfStat(w.classes, "${tt.count} · ${w.hours(tt.minutes)}"),
            PdfStat(iw.lessonsValue, w.money(tt.lessons)).takeIf { extras && tt.lessons > 0 },
            PdfStat(iw.travel, w.money(tt.travel)).takeIf { tt.travel > 0 },
            PdfStat(iw.extraCharges, w.money(tt.charges)).takeIf { tt.charges > 0 },
            PdfStat(w.total, w.money(tt.total)),
            PdfStat(w.paid, w.money(tt.paid), PdfTone.PAID).takeIf { tt.paid > 0 },
            PdfStat(w.toReceive, w.money(tt.left), PdfTone.OWED).takeIf { tt.left > 0 },
        )
    val rows =
        rowsOf(
            r.classes,
            grouped,
            { c ->
                val (status, tone) = w.statusOf(c)
                val hours = if (c.charged) iw.time(c.start) else "${iw.time(c.start)}–${iw.time(c.end)}"
                listOfNotNull(
                    PdfRow(
                        listOf(
                            c.who,
                            iw.weekday(c.day),
                            hours,
                            w.inClass(c),
                            iw.placeOf(c),
                            if (c.travel > 0) w.money(c.travel) else "",
                            if (c.charge > 0) w.money(c.charge) else "",
                            w.money(c.value),
                            status,
                        ),
                        tone,
                    ),
                    c.notes?.takeIf { withNotes }?.let { PdfRow(listOf(it), kind = PdfRowKind.NOTE) },
                )
            },
            { cs ->
                PdfRow(
                    listOf(
                        iw.subtotal,
                        w.classCount(cs.size),
                        "",
                        w.hours(cs.sumOf { it.minutes }),
                        "",
                        "",
                        "",
                        w.money(cs.sumOf { it.value }),
                        "",
                    ),
                    kind = PdfRowKind.SUBTOTAL,
                )
            },
        )
    return PdfPlan(
        brand = w.brand,
        title = iw.detailTitle,
        subtitle = iw.subtitle(w, t, made, r.from, r.to),
        subjectLabel = subjectLabel,
        subjectName = to.guardian ?: to.name,
        subjectLine = to.guardian?.let { iw.guardianOf.format(to.name) } ?: subjectLine,
        stats = stats,
        tableTitle = w.tableClasses,
        columns = listOf(iw.name, iw.dayCol, iw.scheduled, iw.inClass, iw.place, iw.travel, iw.charge, w.value, w.situation),
        rightAligned = setOf(3, 5, 6, 7, 8),
        rows = rows,
        totalRow = listOf(w.total, w.classCount(tt.count), "", w.hours(tt.minutes), "", "", "", w.money(tt.total), ""),
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
        weights = listOf(1.3f, 0.95f, 1.25f, 0.8f, 1.1f, 0.85f, 0.85f, 0.95f, 1f),
        footer = w.generated.format(generatedAt) + " · " + w.page,
    )
}

/**
 * The detailed Excel: "Aulas" with name, date, scheduled start and end, minutes in class, place,
 * travel, charge, value, situation and the notes when asked; "Pagamentos" as 010 (AC-020-19).
 */
fun detailSheets(
    r: SubjectReport,
    w: ReportWords,
    iw: InvoiceWords,
    withNotes: Boolean,
): List<Sheet> =
    listOf(
        Sheet(
            w.sheetClasses,
            listOfNotNull(
                iw.name,
                w.date,
                iw.scheduled,
                iw.ends,
                iw.inClass,
                iw.place,
                iw.travel,
                iw.charge,
                w.value,
                w.situation,
                iw.notes.takeIf { withNotes },
            ),
            chronological(r.classes).map { c ->
                listOfNotNull(
                    Cell.Text(c.who),
                    Cell.Day(c.day.year, c.day.month.ordinal + 1, c.day.day),
                    Cell.Text(iw.time(c.start)),
                    Cell.Text(if (c.charged) "" else iw.time(c.end)),
                    Cell.Num(c.minutes.toLong()),
                    Cell.Text(iw.placeOf(c)),
                    Cell.Money(c.travel),
                    Cell.Money(c.charge),
                    Cell.Money(c.value),
                    Cell.Text(w.statusOf(c).first),
                    Cell.Text(c.notes.orEmpty()).takeIf { withNotes },
                )
            },
        ),
        subjectSheets(r, w)[1],
    )

/**
 * The comprovantes of the document's payments, oldest payment first, each headed "16/10/2026 ·
 * Pix · R$ 540,00" (AC-021-7). [path] finds a kept file; [pages] counts a PDF's pages.
 */
fun receiptsOf(
    r: SubjectReport,
    w: ReportWords,
    path: (app.tutora.domain.Receipt) -> String,
    pages: (app.tutora.domain.Receipt) -> Int,
): List<app.tutora.adapters.export.PdfReceipt> =
    r.payments.sortedBy { it.day }.flatMap { p ->
        val heading = listOf(w.day(p.day), w.methodName(p.method), w.money(p.cents)).joinToString(" · ")
        p.receipts.map { rc ->
            app.tutora.adapters.export.PdfReceipt(
                heading,
                path(rc),
                rc.kind == app.tutora.domain.ReceiptKind.PDF,
                pages(rc).coerceAtLeast(1),
            )
        }
    }

/** Whether a document's period has a payment with a comprovante, so "Incluir comprovantes" shows (AC-021-7). */
fun hasReceipts(r: SubjectReport) = r.payments.any { it.receipts.isNotEmpty() }
