package app.tutora.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.adapters.export.Exports
import app.tutora.adapters.export.LocalExports
import app.tutora.adapters.export.MIME_PDF
import app.tutora.adapters.export.MIME_XLSX
import app.tutora.adapters.export.PdfColors
import app.tutora.adapters.export.Xlsx
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.AlertBanner
import app.tutora.designsystem.components.AlertTone
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.ButtonVariant
import app.tutora.designsystem.components.Card
import app.tutora.designsystem.components.ControlSize
import app.tutora.designsystem.components.DateField
import app.tutora.designsystem.components.EmptyState
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.Icon
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.Money
import app.tutora.designsystem.components.MoneyTone
import app.tutora.designsystem.components.RowTrailing
import app.tutora.designsystem.components.SectionHeader
import app.tutora.designsystem.components.SegmentedControl
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.BuiltinPlace
import app.tutora.domain.DueRow
import app.tutora.domain.Language
import app.tutora.domain.Member
import app.tutora.domain.SubjectKind
import app.tutora.domain.SubjectReport
import app.tutora.domain.invoiceOf
import app.tutora.domain.memberReport
import app.tutora.state.LocalServices
import app.tutora.ui.PersonalData
import app.tutora.ui.ScreenScaffold
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

// Sending a Fatura or a Relatório detalhado (spec 020): the Enviar section of Relatório, its two
// sheets, and Faturas do mês from Ganhos.

/** The Fatura's and detailed report's words, in the app language (AC-020-27). */
@Composable
fun rememberInvoiceWords(): InvoiceWords {
    val language = currentLanguage()
    val online = stringResource(R.string.places_builtin_online)
    val home = stringResource(R.string.places_builtin_student_home)
    return InvoiceWords(
        invoiceTitle = stringResource(R.string.inv_invoice),
        detailTitle = stringResource(R.string.inv_detail),
        to = stringResource(R.string.inv_to),
        guardianOf = stringResource(R.string.inv_guardianOf),
        madeOn = stringResource(R.string.inv_madeOn),
        name = stringResource(R.string.inv_name),
        dayCol = stringResource(R.string.inv_day),
        scheduled = stringResource(R.string.inv_scheduled),
        ends = stringResource(R.string.inv_ends),
        inClass = stringResource(R.string.inv_inClass),
        place = stringResource(R.string.inv_place),
        travel = stringResource(R.string.inv_travel),
        charge = stringResource(R.string.inv_charge),
        subtotal = stringResource(R.string.inv_subtotal),
        salary = stringResource(R.string.inv_salary),
        periodTotal = stringResource(R.string.inv_periodTotal),
        alreadyPaid = stringResource(R.string.inv_alreadyPaid),
        toPay = stringResource(R.string.inv_toPay),
        settled = stringResource(R.string.inv_settled),
        dueOn = stringResource(R.string.inv_dueOn),
        payWithPix = stringResource(R.string.inv_payWithPix),
        copyPaste = stringResource(R.string.inv_copyPaste),
        lessonsValue = stringResource(R.string.inv_lessonsValue),
        extraCharges = stringResource(R.string.inv_extraCharges),
        notes = stringResource(R.string.inv_notes),
        hello = stringResource(R.string.inv_hello),
        invoiceOf = stringResource(R.string.inv_invoiceOf),
        totalToPay = stringResource(R.string.inv_totalToPay),
        until = stringResource(R.string.inv_until),
        pixWithName = stringResource(R.string.inv_pixWithName),
        thanks = stringResource(R.string.inv_thanks),
        settledText = stringResource(R.string.inv_settledText),
        weekday = { d ->
            pattern(if (language == Language.PT_BR) "EEE dd/MM" else "EEE MM/dd", language, d).replace(".", "").lowercase()
        },
        time = { t -> "${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}" },
        builtinPlaces = mapOf(BuiltinPlace.ONLINE.id to online, BuiltinPlace.STUDENT_HOME.id to home),
    )
}

/** "outubro 2026" for a whole month, otherwise the dates (the copied Fatura's period). */
fun periodLabel(
    from: LocalDate,
    to: LocalDate,
    language: Language,
): String =
    if (from.day == 1 && from.year == to.year && from.month == to.month) {
        pattern("LLLL yyyy", language, from)
    } else {
        "${pattern("dd/MM/yyyy", language, from)} – ${pattern("dd/MM/yyyy", language, to)}"
    }

/** Everything the documents need besides the report: words, teacher, theme colors, the share sheet. */
class DocMaker(
    val w: ReportWords,
    val iw: InvoiceWords,
    val teacher: Teacher,
    val city: PixPlace,
    private val colors: PdfColors,
    private val stamp: String,
    private val today: LocalDate,
    private val language: Language,
    private val exports: Exports,
    private val invoiceFile: String,
    private val detailFile: String,
) {
    private fun file(
        name: String,
        kind: String,
        r: SubjectReport,
    ) = fileName("$name - $kind", r.from, r.to, language)

    /** Shares the Fatura PDF, "Ana Souza - fatura - outubro 2026.pdf" (AC-020-2, 6 to 13). */
    fun invoicePdf(
        r: SubjectReport,
        to: Recipient,
        grouped: Boolean,
        due: LocalDate?,
    ) = exports.share(
        file(to.name, invoiceFile, r) + ".pdf",
        MIME_PDF,
        exports.pdf(invoicePlan(invoiceOf(r, grouped), w, iw, teacher, city, to, due, today, stamp), colors),
    )

    /** Copies the Fatura's message (AC-020-14). */
    fun invoiceCopy(
        r: SubjectReport,
        grouped: Boolean,
        due: LocalDate?,
    ) = exports.copy(invoiceText(invoiceOf(r, grouped), w, iw, teacher, city, periodLabel(r.from, r.to, language), due))

    /** Shares the detailed PDF (AC-020-15, 16). */
    fun detailPdf(
        r: SubjectReport,
        to: Recipient,
        label: String,
        line: String?,
        grouped: Boolean,
        notes: Boolean,
    ) = exports.share(
        file(to.name, detailFile, r) + ".pdf",
        MIME_PDF,
        exports.pdf(detailPlan(r, w, iw, teacher, to, label, line, grouped, notes, today, stamp), colors),
    )

    /** Shares the detailed Excel (AC-020-19). */
    fun detailXlsx(
        r: SubjectReport,
        to: Recipient,
        notes: Boolean,
    ) = exports.share(file(to.name, detailFile, r) + ".xlsx", MIME_XLSX, Xlsx.write(detailSheets(r, w, iw, notes)))
}

@Composable
fun rememberDocMaker(): DocMaker {
    val services = LocalServices.current
    val settings by services.settings.collectAsState(initial = null)
    val language = currentLanguage()
    return DocMaker(
        rememberReportWords(),
        rememberInvoiceWords(),
        Teacher(settings?.teacherName.orEmpty(), settings?.pixKey.orEmpty()),
        PixPlace(settings?.pixCity.orEmpty()),
        pdfColors(),
        generatedAt(),
        remember {
            app.tutora.domain
                .localOf(services.now(), services.zone)
                .date
        },
        language,
        LocalExports.current,
        stringResource(R.string.inv_fileInvoice),
        stringResource(R.string.inv_fileDetail),
    )
}

/** One member's share of a group's report, the group named on each row ("Inglês B · 1/3") (AC-020-21). */
fun shareOf(
    group: SubjectReport,
    groupName: String,
    members: Int,
    index: Int,
): SubjectReport {
    val m = memberReport(group, members, index)
    return m.copy(classes = m.classes.map { it.copy(who = "$groupName · 1/$members") })
}

/** A due date, or none (AC-020-9). */
@Composable
private fun DueField(
    label: String,
    due: LocalDate?,
    today: LocalDate,
    onChange: (LocalDate?) -> Unit,
) {
    val language = currentLanguage()
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalAlignment = Alignment.Bottom) {
        DateField(
            label,
            due?.let { pattern("dd/MM/yyyy", language, it) } ?: stringResource(R.string.inv_noDue),
            due ?: today,
            { onChange(it) },
            Modifier.weight(1f).testTag("due-date"),
        )
        if (due != null) {
            Button(stringResource(R.string.inv_noDue), { onChange(null) }, variant = ButtonVariant.GHOST, size = ControlSize.SM)
        }
    }
}

/** A member's line in a split: their name and its two actions. */
@Composable
private fun MemberLine(
    m: Member,
    first: String,
    second: String,
    onFirst: () -> Unit,
    onSecond: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalAlignment = Alignment.CenterVertically) {
        Text(m.name, Modifier.weight(1f), style = TutoraTheme.type.body, color = TutoraTheme.colors.ink)
        Button(first, onFirst, Modifier.testTag("member-first-${m.name}"), variant = ButtonVariant.OUTLINE, size = ControlSize.SM)
        Button(second, onSecond, Modifier.testTag("member-second-${m.name}"), variant = ButtonVariant.OUTLINE, size = ControlSize.SM)
    }
}

/** What Relatório knows about who the documents are for. */
data class SendSubject(
    val kind: SubjectKind,
    val name: String,
    val line: String?,
    val label: String,
    val guardian: String?,
    /** A group's members, sorted by name, when it can be split (AC-020-20). */
    val members: List<Member>?,
)

/**
 * Enviar: Fatura and Relatório detalhado, each opening its sheet; the hint to add the Pix key when
 * there is none (AC-020-1, 2, 11, 20, 21).
 */
@Composable
fun SendSection(
    nav: NavHostController,
    r: SubjectReport,
    s: SendSubject,
) {
    val maker = rememberDocMaker()
    val today = remember { LocalDate(r.to.year, r.to.month, r.to.day) }
    var open by rememberSaveable { mutableStateOf("") }
    SectionHeader(stringResource(R.string.inv_send))
    ListGroup(
        listOf(
            {
                ListRow(
                    stringResource(R.string.inv_invoice),
                    Modifier.testTag("send-invoice"),
                    subtitle = stringResource(R.string.inv_invoiceHint),
                    icon = IconName.WALLET,
                    onClick = { open = "invoice" },
                )
            },
            {
                ListRow(
                    stringResource(R.string.inv_detail),
                    Modifier.testTag("send-detail"),
                    subtitle = stringResource(R.string.inv_detailHint),
                    icon = IconName.FILE,
                    onClick = { open = "detail" },
                )
            },
        ),
    )
    if (maker.teacher.pix.isBlank()) {
        AlertBanner(stringResource(R.string.inv_addPix), tone = AlertTone.ACCENT, onClick = { nav.navigate(PersonalData) })
    }
    if (open.isEmpty()) return
    var split by rememberSaveable { mutableStateOf(false) }
    var due by rememberSaveable { mutableStateOf<String?>(null) }
    var notes by rememberSaveable { mutableStateOf(true) }
    val dueDate = due?.let(LocalDate::parse)
    val invoice = open == "invoice"
    val grouped = s.kind == SubjectKind.PLACE
    val to = Recipient(s.name, s.guardian)
    val members = s.members
    val close = { open = "" }
    BottomSheet(
        stringResource(if (invoice) R.string.inv_invoice else R.string.inv_detail),
        close,
        subtitle = s.name,
        actions =
            if (split && members != null) {
                emptyList()
            } else if (invoice) {
                listOf(
                    { m ->
                        Button(stringResource(R.string.rep_copy), {
                            maker.invoiceCopy(r, grouped, dueDate)
                            close()
                        }, m.testTag("sheet-copy"), variant = ButtonVariant.OUTLINE)
                    },
                    { m ->
                        Button(stringResource(R.string.rep_pdf), {
                            maker.invoicePdf(r, to, grouped, dueDate)
                            close()
                        }, m.testTag("sheet-pdf"))
                    },
                )
            } else {
                listOf(
                    { m ->
                        Button(stringResource(R.string.rep_excel), {
                            maker.detailXlsx(r, to, notes)
                            close()
                        }, m.testTag("sheet-excel"), variant = ButtonVariant.OUTLINE)
                    },
                    { m ->
                        Button(stringResource(R.string.rep_pdf), {
                            maker.detailPdf(r, to, s.label, s.line, grouped, notes)
                            close()
                        }, m.testTag("sheet-pdf"))
                    },
                )
            },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            if (members != null) {
                SegmentedControl(
                    listOf(false to stringResource(R.string.inv_whole), true to stringResource(R.string.inv_split)),
                    split,
                    { split = it },
                    stringResource(R.string.inv_split),
                )
            }
            if (invoice) {
                DueField(stringResource(R.string.inv_dueDate), dueDate, today) { due = it?.toString() }
            } else {
                ListRow(
                    stringResource(R.string.inv_withNotes),
                    trailing = RowTrailing.SWITCH,
                    switchValue = notes,
                    onSwitchChange = { notes = it },
                )
            }
            if (split && members != null) {
                members.forEachIndexed { i, m ->
                    val share = shareOf(r, s.name, members.size, i)
                    val mine = Recipient(m.name, m.guardian)
                    if (invoice) {
                        MemberLine(m, stringResource(R.string.rep_pdf), stringResource(R.string.inv_text), {
                            maker.invoicePdf(share, mine, false, dueDate)
                        }, { maker.invoiceCopy(share, false, dueDate) })
                    } else {
                        MemberLine(m, stringResource(R.string.rep_pdf), stringResource(R.string.rep_excel), {
                            maker.detailPdf(share, mine, s.label, s.name, false, notes)
                        }, { maker.detailXlsx(share, mine, notes) })
                    }
                }
            }
        }
    }
}

/**
 * Faturas do mês: for Ganhos' period, who still owes, largest first, with the total; each row
 * shares its Fatura or copies its text and keeps a check while the list is open; one due date for
 * all; groups split per student on request (AC-020-23 to 26).
 */
@Composable
fun InvoicesScreen(
    nav: NavHostController,
    from: LocalDate,
    to: LocalDate,
    label: String,
) {
    val services = LocalServices.current
    val maker = rememberDocMaker()
    val scope = rememberCoroutineScope()
    val zone = services.zone
    val today =
        remember {
            app.tutora.domain
                .localOf(services.now(), zone)
                .date
        }
    var split by rememberSaveable { mutableStateOf(false) }
    var due by rememberSaveable { mutableStateOf<String?>(null) }
    var done by rememberSaveable { mutableStateOf("") }
    val rows by remember(from, to, split) { services.reportsRepo.due(from, to, zone, split) }.collectAsState(initial = null)
    val dueDate = due?.let(LocalDate::parse)
    val until = minOf(to, today)
    ScreenScaffold(
        stringResource(R.string.inv_monthTitle),
        "invoices-screen",
        variant = HeaderVariant.PUSH,
        onBack = { nav.popBackStack() },
    ) {
        Text(label, style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
        DueField(stringResource(R.string.inv_dueAll), dueDate, today) { due = it?.toString() }
        ListRow(
            stringResource(R.string.inv_splitGroups),
            trailing = RowTrailing.SWITCH,
            switchValue = split,
            onSwitchChange = { split = it },
        )
        val list = rows ?: return@ScreenScaffold
        if (list.isEmpty()) {
            EmptyState(stringResource(R.string.inv_nobody), icon = IconName.CHECK)
            return@ScreenScaffold
        }
        Card(Modifier.testTag("invoices-total")) {
            Text(stringResource(R.string.inv_monthTotal), style = TutoraTheme.type.caption, color = TutoraTheme.colors.inkMuted)
            Money(list.sumOf { it.toPay }, tone = MoneyTone.OWED)
        }
        list.forEach { row ->
            val key = "${row.kind}:${row.id}:${row.member?.id.orEmpty()}"
            val sent = key in done.split('|')
            val act = { copy: Boolean ->
                scope.launch {
                    val report = services.reportsRepo.subjectNow(app.tutora.data.Subject(row.kind, row.id), from, until, today, zone)
                    val r =
                        if (row.member !=
                            null
                        ) {
                            shareOf(report, report.classes.firstOrNull()?.who ?: row.name, row.members, row.memberIndex)
                        } else {
                            report
                        }
                    val grouped = row.kind == SubjectKind.PLACE
                    if (copy) {
                        maker.invoiceCopy(
                            r,
                            grouped,
                            dueDate,
                        )
                    } else {
                        maker.invoicePdf(r, Recipient(row.name, row.guardian), grouped, dueDate)
                    }
                    done = (done.split('|').filter { it.isNotEmpty() } + key).joinToString("|")
                }
                Unit
            }
            DueLine(row, sent, { act(false) }, { act(true) })
        }
    }
}

@Composable
private fun DueLine(
    row: DueRow,
    sent: Boolean,
    onPdf: () -> Unit,
    onText: () -> Unit,
) {
    Card(Modifier.testTag("due-${row.name}")) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(row.name, style = TutoraTheme.type.bodyStrong, color = TutoraTheme.colors.ink)
                row.guardian?.let { Text(it, style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted) }
            }
            if (sent) Icon(IconName.CHECK, tint = TutoraTheme.colors.successInk, label = stringResource(R.string.inv_sent))
            Money(row.toPay, tone = MoneyTone.OWED)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Button(
                stringResource(R.string.rep_pdf),
                onPdf,
                Modifier.weight(1f).testTag("due-pdf-${row.name}"),
                variant = ButtonVariant.OUTLINE,
                size = ControlSize.SM,
            )
            Button(
                stringResource(R.string.inv_text),
                onText,
                Modifier.weight(1f).testTag("due-text-${row.name}"),
                variant = ButtonVariant.OUTLINE,
                size = ControlSize.SM,
            )
        }
    }
}
