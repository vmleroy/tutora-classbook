package app.tutora.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.adapters.export.LocalExports
import app.tutora.adapters.export.MIME_PDF
import app.tutora.adapters.export.MIME_XLSX
import app.tutora.adapters.export.PdfColors
import app.tutora.adapters.export.Xlsx
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.BadgeTone
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.ButtonVariant
import app.tutora.designsystem.components.Card
import app.tutora.designsystem.components.Checkbox
import app.tutora.designsystem.components.Chip
import app.tutora.designsystem.components.DateField
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.Money
import app.tutora.designsystem.components.MoneyTone
import app.tutora.designsystem.components.RowTrailing
import app.tutora.designsystem.components.SectionHeader
import app.tutora.designsystem.components.SegmentedControl
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.EarningsView
import app.tutora.domain.Language
import app.tutora.domain.ReportRange
import app.tutora.domain.SubjectKind
import app.tutora.domain.SubjectReport
import app.tutora.domain.localOf
import app.tutora.state.LocalServices
import app.tutora.ui.LocalClockDurations
import app.tutora.ui.ScreenScaffold
import app.tutora.ui.clockDuration
import app.tutora.ui.kindName
import app.tutora.ui.localeOf
import app.tutora.ui.minutesText
import app.tutora.ui.payments.methodName
import app.tutora.ui.placeName
import app.tutora.ui.placePriceText
import app.tutora.ui.rateMoney
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter

private fun pattern(
    p: String,
    language: Language,
    d: LocalDate,
) = DateTimeFormatter.ofPattern(p, localeOf(language)).format(d.toJavaLocalDate())

/** "6h30", or "06:30" with Durações em HH:MM on. */
fun hoursText(
    minutes: Int,
    clock: Boolean,
): String =
    if (clock) clockDuration(minutes) else "${minutes / 60}h${if (minutes % 60 > 0) (minutes % 60).toString().padStart(2, '0') else ""}"

/** Every word and format a report needs, in the app language (AC-010-17). */
@Composable
fun rememberReportWords(): ReportWords {
    val language = currentLanguage()
    val clock = LocalClockDurations.current
    val classOne = pluralStringResource(R.plurals.pay_lessonsCount, 1, 1)
    val classMany = pluralStringResource(R.plurals.pay_lessonsCount, 2, 2)
    val methods =
        app.tutora.domain.PayMethod.entries
            .associateWith { methodName(it) }
    val minutesTemplate = stringResource(R.string.places_minutes)
    return ReportWords(
        brand = stringResource(R.string.rep_brand),
        subjectTitle = stringResource(R.string.rep_title),
        earningsTitle = stringResource(R.string.rep_earningsTitle),
        teacher = stringResource(R.string.rep_teacher),
        range = stringResource(R.string.rep_range),
        classes = stringResource(R.string.rep_classes),
        total = stringResource(R.string.rep_total),
        paid = stringResource(R.string.rep_paid),
        toReceive = stringResource(R.string.pay_toReceive),
        earned = stringResource(R.string.rep_earned),
        received = stringResource(R.string.earn_received),
        tableClasses = stringResource(R.string.rep_tableClasses),
        tableDays = stringResource(R.string.rep_tableDays),
        date = stringResource(R.string.lesson_date),
        duration = stringResource(R.string.lesson_duration),
        where = stringResource(R.string.rep_where),
        value = stringResource(R.string.lesson_value),
        situation = stringResource(R.string.rep_situation),
        statusPaid = stringResource(R.string.pay_paid),
        statusOwed = stringResource(R.string.pay_toReceive),
        statusCharged = stringResource(R.string.rep_charged),
        statusBonus = stringResource(R.string.rep_bonus),
        paymentsTitle = stringResource(R.string.rep_payments),
        pending = stringResource(R.string.rep_pending),
        pix = stringResource(R.string.rep_pix),
        generated = stringResource(R.string.rep_generated),
        page = stringResource(R.string.rep_page),
        classCount = { n -> (if (n == 1) classOne else classMany).replace(Regex("^\\d+"), n.toString()) },
        methodName = { methods.getValue(it) },
        money = { formatMoney(it, language) },
        day = { pattern("dd/MM/yyyy", language, it) },
        dayMonth = { pattern(if (language == Language.PT_BR) "dd/MM" else "MM/dd", language, it) },
        hours = { hoursText(it, clock) },
        minutes = { if (clock) clockDuration(it) else minutesTemplate.format(it.toString()) },
        bySource = stringResource(R.string.rep_bySource),
        origin = stringResource(R.string.rep_origin),
        type = stringResource(R.string.rep_type),
        trend = stringResource(R.string.earn_trend),
        classList = stringResource(R.string.rep_classList),
        month = { pattern("LLLL yyyy", language, it).replaceFirstChar { c -> c.uppercase() } },
        sheetClasses = stringResource(R.string.rep_sheetClasses),
        sheetPayments = stringResource(R.string.pay_payments),
        sheetSummary = stringResource(R.string.rep_sheetSummary),
        sheetSources = stringResource(R.string.rep_sheetSources),
        method = stringResource(R.string.rep_method),
        amount = stringResource(R.string.pay_amount),
    )
}

/** The theme's colors for the PDF: never raw values (constitution rule 13). */
@Composable
fun pdfColors(): PdfColors {
    val c = TutoraTheme.colors
    return PdfColors(
        c.primary.toArgb(),
        c.onPrimary.toArgb(),
        c.ink.toArgb(),
        c.inkMuted.toArgb(),
        c.success.toArgb(),
        c.danger.toArgb(),
        c.line.toArgb(),
    )
}

/** "26/10/2026, 18:45", for the PDF's foot. */
@Composable
fun generatedAt(): String {
    val services = LocalServices.current
    val local = localOf(services.now(), services.zone)
    val language = currentLanguage()
    return pattern("dd/MM/yyyy", language, local.date) + ", " + local.time.toString().take(5)
}

/** "Ana Souza - outubro 2026", or the dates when the period isn't one month (AC-010-11). */
fun fileName(
    name: String,
    from: LocalDate,
    to: LocalDate,
    language: Language,
): String =
    if (from.year == to.year && from.month == to.month) {
        "$name - ${pattern("LLLL yyyy", language, from)}"
    } else {
        "$name - ${pattern("dd-MM-yyyy", language, from)} a ${pattern("dd-MM-yyyy", language, to)}"
    }

/** Este mês, Mês passado, 3 meses, Outro…, as saved state. */
private fun rangeOf(
    key: String,
    from: String,
    to: String,
): ReportRange =
    when (key) {
        "last" -> ReportRange.LastMonth
        "three" -> ReportRange.ThreeMonths
        "custom" -> ReportRange.Custom(LocalDate.parse(from), LocalDate.parse(to))
        else -> ReportRange.ThisMonth
    }

/**
 * Relatório for one student, group or place: who, the period, the totals, the classes, and the
 * export row (AC-010-1 to 8, 11, 12).
 */
@Composable
fun ReportScreen(
    nav: NavHostController,
    startKind: String,
    startId: String,
) {
    val services = LocalServices.current
    val exports = LocalExports.current
    val language = currentLanguage()
    val zone = services.zone
    val today = remember { localOf(services.now(), zone).date }
    var kind by rememberSaveable { mutableStateOf(SubjectKind.valueOf(startKind)) }
    var id by rememberSaveable { mutableStateOf(startId) }
    var range by rememberSaveable { mutableStateOf("month") }
    var customFrom by rememberSaveable { mutableStateOf(today.toString()) }
    var customTo by rememberSaveable { mutableStateOf(today.toString()) }
    var copied by rememberSaveable { mutableStateOf(false) }
    val (from, to) = rangeOf(range, customFrom, customTo).dates(today)
    val students by remember { services.catalogRepo.students() }.collectAsState(initial = emptyList())
    val groups by remember { services.catalogRepo.groups() }.collectAsState(initial = emptyList())
    val places by remember { services.catalogRepo.places() }.collectAsState(initial = emptyList())
    val settings by services.settings.collectAsState(initial = null)
    val report by remember(kind, id, from, to) {
        services.reportsRepo.subject(app.tutora.data.Subject(kind, id), from, to, today, zone)
    }.collectAsState(initial = null)
    val words = rememberReportWords()
    val colors = pdfColors()
    val stamp = generatedAt()
    val teacher = Teacher(settings?.teacherName.orEmpty(), settings?.pixKey.orEmpty())
    // Who it is for, and how they are charged (AC-010-4).
    val options: List<Triple<String, String, String?>> =
        when (kind) {
            SubjectKind.STUDENT -> students.map { Triple(it.student.id, it.student.name, it.student.subject) }
            SubjectKind.GROUP ->
                groups.map { g ->
                    Triple(
                        g.id,
                        g.name,
                        listOfNotNull(
                            g.subject,
                            g.rateCents?.let {
                                stringResource(R.string.places_perLesson, rateMoney(it, language))
                            },
                        ).joinToString(" · "),
                    )
                }
            SubjectKind.PLACE ->
                places.map { p ->
                    Triple(p.place.id, placeName(p.place), "${kindName(p.place.kind)} · ${placePriceText(p.place)}")
                }
        }
    val subject = options.firstOrNull { it.first == id }
    val subjectLabel =
        stringResource(
            when (kind) {
                SubjectKind.STUDENT -> R.string.rep_student
                SubjectKind.GROUP -> R.string.rep_group
                SubjectKind.PLACE -> R.string.rep_place
            },
        )
    ScreenScaffold(
        stringResource(R.string.rep_screen),
        "report-screen",
        variant = HeaderVariant.PUSH,
        onBack = { nav.popBackStack() },
    ) {
        SegmentedControl(
            listOf(
                SubjectKind.STUDENT to stringResource(R.string.rep_student),
                SubjectKind.GROUP to stringResource(R.string.rep_group),
                SubjectKind.PLACE to stringResource(R.string.rep_place),
            ),
            kind,
            { k ->
                if (k != kind) {
                    kind = k
                    id = ""
                }
            },
            stringResource(R.string.rep_screen),
        )
        // Which one: switching keeps the period (AC-010-2).
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            options.forEach { (oid, name, _) -> Chip(name, oid == id, { id = oid }) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            listOf(
                "month" to R.string.rep_thisMonth,
                "last" to R.string.rep_lastMonth,
                "three" to R.string.rep_threeMonths,
                "custom" to R.string.rep_custom,
            ).forEach { (k, label) -> Chip(stringResource(label), range == k, { range = k }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            DateField(stringResource(R.string.rep_from), pattern("dd/MM/yyyy", language, from), from, {
                range = "custom"
                customFrom = it.toString()
                customTo = to.toString()
            }, Modifier.weight(1f))
            DateField(stringResource(R.string.rep_to), pattern("dd/MM/yyyy", language, to), to, {
                range = "custom"
                customFrom = from.toString()
                customTo = it.toString()
            }, Modifier.weight(1f))
        }
        val r = report
        if (subject == null || r == null) return@ScreenScaffold
        Card(Modifier.testTag("report-summary")) {
            Text(subject.second, style = TutoraTheme.type.titleMd, color = TutoraTheme.colors.ink)
            subject.third?.takeIf { it.isNotBlank() }?.let { Text(it, style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted) }
            Text("${words.classCount(r.count)} · ${words.hours(r.minutes)}", style = TutoraTheme.type.body, color = TutoraTheme.colors.ink)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                listOf(
                    Triple(words.total, r.total, MoneyTone.EARNINGS),
                    Triple(words.paid, r.paid, MoneyTone.PAID),
                    Triple(words.toReceive, r.left, MoneyTone.OWED),
                ).forEach { (label, cents, tone) ->
                    Column(Modifier.weight(1f)) {
                        Text(label, style = TutoraTheme.type.caption, color = TutoraTheme.colors.inkMuted)
                        Money(cents, tone = tone)
                    }
                }
            }
        }
        ReportList(r, words, showWho = kind == SubjectKind.PLACE)
        // Exportar este relatório: PDF and Excel side by side, and Copiar como texto (AC-010-8).
        SectionHeader(stringResource(R.string.rep_export))
        val name = subject.second
        val plan = { subjectPlan(r, words, teacher, subjectLabel, name, subject.third, kind == SubjectKind.PLACE, stamp) }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            Button("PDF", {
                exports.share(fileName(name, from, to, language) + ".pdf", MIME_PDF, exports.pdf(plan(), colors))
            }, Modifier.weight(1f), variant = ButtonVariant.OUTLINE, icon = IconName.FILE)
            Button("Excel", {
                exports.share(fileName(name, from, to, language) + ".xlsx", MIME_XLSX, Xlsx.write(subjectSheets(r, words)))
            }, Modifier.weight(1f), variant = ButtonVariant.OUTLINE, icon = IconName.FILE)
        }
        Button(
            stringResource(if (copied) R.string.rep_copied else R.string.rep_copy),
            {
                exports.copy(subjectText(r, words, teacher, name))
                copied = true
            },
            variant = ButtonVariant.GHOST,
            fullWidth = true,
        )
    }
}

@Composable
private fun ReportList(
    r: SubjectReport,
    w: ReportWords,
    showWho: Boolean,
) {
    val days = r.days
    SectionHeader(if (days != null) w.tableDays else w.tableClasses)
    if (days != null) {
        if (days.isNotEmpty()) {
            ListGroup(
                days.map { d ->
                    { ListRow(w.day(d.day), subtitle = w.classCount(d.classes), trailing = RowTrailing.NONE) }
                },
            )
        }
        return
    }
    if (r.classes.isEmpty()) {
        Text(stringResource(R.string.pay_noLessons), style = TutoraTheme.type.body, color = TutoraTheme.colors.inkMuted)
        return
    }
    ListGroup(
        r.classes.map { c ->
            {
                ListRow(
                    listOfNotNull(w.dayMonth(c.day), c.who.takeIf { showWho && it.isNotEmpty() }).joinToString(" · "),
                    subtitle = listOf(minutesText(c.minutes), w.money(c.value)).joinToString(" · "),
                    badge =
                        when {
                            c.charged -> BadgeTone.NEUTRAL to w.statusCharged
                            c.paid -> BadgeTone.SUCCESS to w.statusPaid
                            else -> BadgeTone.DANGER to w.statusOwed
                        },
                    trailing = RowTrailing.NONE,
                )
            }
        },
    )
}

/**
 * Relatório de ganhos, from Ganhos: the period and type on screen, PDF or Excel, and what to
 * include (AC-010-13 to 15).
 */
@Composable
fun EarningsReportSheet(
    view: EarningsView,
    periodLine: String,
    fileBase: String,
    onDismiss: () -> Unit,
) {
    val services = LocalServices.current
    val exports = LocalExports.current
    val words = rememberReportWords()
    val colors = pdfColors()
    val stamp = generatedAt()
    val settings by services.settings.collectAsState(initial = null)
    val balances by remember { services.paymentRepo.balances() }.collectAsState(initial = emptyMap())
    var pdf by rememberSaveable { mutableStateOf(true) }
    var parts by remember { mutableStateOf(EarningsParts()) }
    val teacher = Teacher(settings?.teacherName.orEmpty(), settings?.pixKey.orEmpty())
    val owes = { p: app.tutora.domain.PayerRef -> balances[p] ?: 0L }
    BottomSheet(
        stringResource(R.string.rep_earningsTitle),
        onDismiss,
        subtitle = stringResource(R.string.rep_earningsSubtitle, periodLine),
        actions =
            listOf(
                { m -> Button(stringResource(R.string.catalog_cancel), onDismiss, m, variant = ButtonVariant.OUTLINE) },
                { m ->
                    Button(stringResource(if (pdf) R.string.rep_makePdf else R.string.rep_makeExcel), {
                        if (pdf) {
                            exports.share(
                                "$fileBase.pdf",
                                MIME_PDF,
                                exports.pdf(earningsPlan(view, words, teacher, periodLine, parts, owes, stamp), colors),
                            )
                        } else {
                            exports.share("$fileBase.xlsx", MIME_XLSX, Xlsx.write(earningsSheets(view, words, parts, owes)))
                        }
                        onDismiss()
                    }, m)
                },
            ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                Chip(stringResource(R.string.rep_pdfChoice), pdf, { pdf = true })
                Chip(stringResource(R.string.rep_excelChoice), !pdf, { pdf = false })
            }
            Checkbox(stringResource(R.string.rep_partSources), parts.bySource, { parts = parts.copy(bySource = it) })
            Checkbox(stringResource(R.string.rep_partClasses), parts.classes, { parts = parts.copy(classes = it) })
            Checkbox(stringResource(R.string.rep_partPayments), parts.payments, { parts = parts.copy(payments = it) })
        }
    }
}
