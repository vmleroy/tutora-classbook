package app.tutora.ui.payments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.AlertBanner
import app.tutora.designsystem.components.AlertTone
import app.tutora.designsystem.components.Badge
import app.tutora.designsystem.components.BadgeTone
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.ButtonVariant
import app.tutora.designsystem.components.Card
import app.tutora.designsystem.components.Checkbox
import app.tutora.designsystem.components.Chip
import app.tutora.designsystem.components.ControlSize
import app.tutora.designsystem.components.DateField
import app.tutora.designsystem.components.HeaderAction
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.Money
import app.tutora.designsystem.components.MoneyField
import app.tutora.designsystem.components.MoneySize
import app.tutora.designsystem.components.MoneyTone
import app.tutora.designsystem.components.ProgressBar
import app.tutora.designsystem.components.ProgressTone
import app.tutora.designsystem.components.RowTrailing
import app.tutora.designsystem.components.SectionHeader
import app.tutora.designsystem.components.SegmentedControl
import app.tutora.designsystem.components.StatTone
import app.tutora.designsystem.components.Stepper
import app.tutora.designsystem.components.Switch
import app.tutora.designsystem.components.TextField
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.Balance
import app.tutora.domain.Cents
import app.tutora.domain.CreditRecord
import app.tutora.domain.Language
import app.tutora.domain.Ledger
import app.tutora.domain.Lesson
import app.tutora.domain.OwedClass
import app.tutora.domain.OwedElsewhere
import app.tutora.domain.PackageState
import app.tutora.domain.Paid
import app.tutora.domain.PayMethod
import app.tutora.domain.Payer
import app.tutora.domain.PayerKind
import app.tutora.domain.PayerRef
import app.tutora.domain.PaymentRecord
import app.tutora.domain.Place
import app.tutora.domain.PlaceKind
import app.tutora.domain.RateType
import app.tutora.domain.Who
import app.tutora.domain.balanceOf
import app.tutora.domain.localOf
import app.tutora.domain.payerOf
import app.tutora.state.LocalServices
import app.tutora.ui.GroupDetail
import app.tutora.ui.LessonDetail
import app.tutora.ui.LocalClockDurations
import app.tutora.ui.PayerLessons
import app.tutora.ui.PaymentDetail
import app.tutora.ui.RegistryHome
import app.tutora.ui.ScreenScaffold
import app.tutora.ui.StudentDetail
import app.tutora.ui.catalog.DetailOverview
import app.tutora.ui.catalog.DetailStat
import app.tutora.ui.clockDuration
import app.tutora.ui.localeOf
import app.tutora.ui.minutesText
import app.tutora.ui.placeName
import app.tutora.usecases.PaymentDraft
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import app.tutora.ui.PlaceDetail as PlaceDetailRoute

// Balances, the payment sheet and payments (spec 008).

/** "16/10" / "10/16". */
fun dayMonth(
    date: LocalDate,
    language: Language,
): String =
    DateTimeFormatter
        .ofPattern(
            if (language ==
                Language.PT_BR
            ) {
                "dd/MM"
            } else {
                "MM/dd"
            },
            localeOf(language),
        ).format(date.toJavaLocalDate())

@Composable
fun methodName(method: PayMethod): String =
    stringResource(
        when (method) {
            PayMethod.PIX -> R.string.pay_pix
            PayMethod.CASH -> R.string.pay_cash
            PayMethod.TRANSFER -> R.string.pay_transfer
            PayMethod.CARD -> R.string.pay_card
        },
    )

/** "Deve R$ 240", "Em dia" or "Crédito R$ 40", for list rows (AC-008-4, 13). */
@Composable
fun balanceBadge(cents: Cents): Pair<BadgeTone, String> {
    val language = currentLanguage()
    return when (val b = balanceOf(cents, 0)) {
        is Balance.Owes -> BadgeTone.DANGER to stringResource(R.string.pay_owes, formatMoney(b.cents, language, compact = true))
        is Balance.Credit -> BadgeTone.SUCCESS to stringResource(R.string.pay_credit, formatMoney(b.cents, language, compact = true))
        Balance.Settled -> BadgeTone.SUCCESS to stringResource(R.string.pay_settled)
    }
}

/** "A receber R$ 120" or "Em dia", for a place whose students pay: the place itself owes nothing (AC-008-36). */
@Composable
fun toReceiveBadge(cents: Cents): Pair<BadgeTone, String> =
    if (cents > 0) {
        BadgeTone.DANGER to stringResource(R.string.pay_toReceiveAmount, formatMoney(cents, currentLanguage(), compact = true))
    } else {
        BadgeTone.SUCCESS to stringResource(R.string.pay_settled)
    }

/** Pago, or A receber with what is left when paid in part (AC-008-9). */
@Composable
fun paidBadge(
    ledger: Ledger,
    lesson: OwedClass,
): Pair<BadgeTone, String> = leftBadge(ledger.allocation.left[lesson.lessonId] ?: 0, lesson.dueCents)

@Composable
fun leftBadge(
    left: Cents,
    due: Cents,
): Pair<BadgeTone, String> =
    when {
        left == 0L -> BadgeTone.SUCCESS to stringResource(R.string.pay_paid)
        left < due -> BadgeTone.DANGER to stringResource(R.string.pay_leftPart, formatMoney(left, currentLanguage()))
        else -> BadgeTone.DANGER to stringResource(R.string.pay_toReceive)
    }

private fun OwedClass.localDate(zone: String) = localOf(startAt, zone).date

/**
 * At the top of a payer's detail: what it owes, with its pay button (AC-008-11, 14). With
 * Créditos e bônus on, its credit and bonus minutes, Usar crédito and Dar bônus (AC-008-30, 31).
 */
@Composable
fun PayerBalance(
    payer: PayerRef,
    payLabel: String,
    isPlace: Boolean = false,
    stats: List<DetailStat> = emptyList(),
    onReport: () -> Unit = {},
    onNewLesson: () -> Unit = {},
) {
    val services = LocalServices.current
    val language = currentLanguage()
    val ledger by remember(payer) { services.paymentRepo.ledger(payer) }.collectAsState(initial = null)
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    val l = ledger ?: return
    val owes = l.owedCents
    // What is owed is the first of the three figures, red while something is owed (AC-008-11, AC-016-2).
    val owed =
        if (owes > 0) {
            DetailStat(
                stringResource(if (isPlace) R.string.pay_placeOwesLabel else R.string.pay_owesLabel),
                formatMoney(owes, language),
                StatTone.OWED,
            )
        } else {
            DetailStat(stringResource(R.string.pay_settled), formatMoney(0, language), StatTone.PAID)
        }
    Box(Modifier.testTag("balance")) {
        DetailOverview(listOf(owed) + stats, payLabel to { sheet = "pay" }, onReport, onNewLesson)
    }
    if (sheet == "pay") PaymentSheet(payer, l, onDismiss = { sheet = null }, elsewhere = studentElsewhere(payer))
}

/**
 * Créditos e bônus, its own section apart from what is owed (AC-008-37): what credit and minutes
 * are left, Usar crédito, a small + to give a bonus, each bonus (tap to edit or remove it) and each
 * time credit went on a class (tap to give it back). Only with the Ajustes switch on.
 */
@Composable
fun CreditsSection(payer: PayerRef) {
    val services = LocalServices.current
    val language = currentLanguage()
    val settings by services.settings.collectAsState(initial = null)
    val ledger by remember(payer) { services.paymentRepo.ledger(payer) }.collectAsState(initial = null)
    val records by remember(payer) { services.paymentRepo.creditRecords(payer) }.collectAsState(initial = emptyList())
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    if (settings?.credits != true) return
    val l = ledger ?: return
    Column(Modifier.testTag("credits-section"), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        SectionHeader(
            stringResource(R.string.settings_credits),
            action = IconName.PLUS,
            actionLabel = stringResource(R.string.pay_giveBonus),
            onAction = { sheet = "bonus" },
        )
        val left =
            listOfNotNull(
                l.creditCents.takeIf { it > 0 }?.let { stringResource(R.string.pay_creditLine, formatMoney(it, language)) },
                l.creditMinutes.takeIf { it > 0 }?.let { stringResource(R.string.pay_minutesLine, minutesText(it)) },
            ).joinToString(" · ")
        Text(
            left.ifEmpty { stringResource(R.string.pay_noCredit) },
            Modifier.testTag("credit-line"),
            style = TutoraTheme.type.bodyStrong,
            color = if (left.isEmpty()) TutoraTheme.colors.inkMuted else TutoraTheme.colors.successInk,
        )
        if (l.owedCents > 0 && (l.creditCents > 0 || l.creditMinutes > 0)) {
            Button(stringResource(R.string.pay_useCredit), { sheet = "use" }, variant = ButtonVariant.OUTLINE, fullWidth = true)
        }
        if (records.isNotEmpty()) {
            ListGroup(
                records.map { r ->
                    {
                        val amount =
                            listOfNotNull(
                                r.cents.takeIf { it > 0 }?.let { formatMoney(it, language) },
                                r.minutes.takeIf { it > 0 }?.let { minutesText(it) },
                            ).joinToString(" · ")
                        ListRow(
                            if (r.isBonus) {
                                stringResource(R.string.pay_bonusRow, amount)
                            } else {
                                stringResource(R.string.pay_useRow, dayMonth(localOf(r.lessonStart ?: r.at, services.zone).date, language))
                            },
                            subtitle =
                                if (r.isBonus) {
                                    listOfNotNull(dayMonth(localOf(r.at, services.zone).date, language), r.note).joinToString(" · ")
                                } else {
                                    amount
                                },
                            icon = if (r.isBonus) IconName.STAR else IconName.CALENDAR_CHECK,
                            onClick = {
                                editing = r.id
                                sheet = if (r.isBonus) "bonus" else "giveBack"
                            },
                        )
                    }
                },
            )
        }
    }
    val scope = rememberCoroutineScope()
    when (sheet) {
        "use" -> UseCreditSheet(payer, l) { sheet = null }
        "bonus" ->
            BonusSheet(payer, records.firstOrNull { it.id == editing && it.isBonus }) {
                sheet = null
                editing = null
            }
        "giveBack" ->
            BottomSheet(
                stringResource(R.string.pay_giveBackTitle),
                {
                    sheet = null
                    editing = null
                },
                subtitle = stringResource(R.string.pay_giveBackBody),
                actions =
                    listOf(
                        { m -> Button(stringResource(R.string.catalog_cancel), { sheet = null }, m, variant = ButtonVariant.OUTLINE) },
                        { m ->
                            Button(stringResource(R.string.pay_giveBack), {
                                val id = editing
                                scope.launch { id?.let { services.payments.removeCredit(it) } }
                                sheet = null
                                editing = null
                            }, m, variant = ButtonVariant.DANGER)
                        },
                    ),
            )
    }
}

/** A student's classes their places owe instead of them; empty for any other payer (AC-008-26). */
@Composable
private fun studentElsewhere(payer: PayerRef): List<OwedElsewhere> {
    if (payer.kind != PayerKind.STUDENT) return emptyList()
    val services = LocalServices.current
    val elsewhere by remember(payer) { services.paymentRepo.elsewhere(payer.id) }.collectAsState(initial = emptyList())
    return elsewhere
}

/** One class in a payer's list: date (and whose, on a group's or place's page), minutes, value, who pays, Pago or A receber. */
private data class PayerClass(
    val owed: OwedClass,
    val left: Cents,
    val placePays: String?,
)

@Composable
private fun payerClasses(payer: PayerRef): List<PayerClass>? {
    val services = LocalServices.current
    val ledger by remember(payer) { services.paymentRepo.ledger(payer) }.collectAsState(initial = null)
    val elsewhere = studentElsewhere(payer)
    val places by remember { services.catalogRepo.places() }.collectAsState(initial = emptyList())
    val placeNames = places.associate { it.place.id to placeName(it.place) }
    val l = ledger ?: return null
    return (
        l.owed.map { PayerClass(it, l.allocation.left[it.lessonId] ?: 0, null) } +
            elsewhere.map { PayerClass(it.owed, it.left, placeNames[it.payer.id]) }
    ).sortedByDescending { it.owed.startAt }
}

@Composable
private fun PayerClassRow(
    nav: NavHostController,
    c: PayerClass,
    showWho: Boolean,
) {
    val services = LocalServices.current
    val language = currentLanguage()
    val places by remember { services.catalogRepo.places() }.collectAsState(initial = emptyList())
    // "28/10 · Ventura" on a student's or group's page; "28/10 · ENEM" on a place's (AC-008-33).
    val summary = if (showWho) c.owed.who else places.firstOrNull { it.place.id == c.owed.placeId }?.let { placeName(it.place) }
    ListRow(
        listOfNotNull(dayMonth(c.owed.localDate(services.zone), language), summary?.takeIf { it.isNotEmpty() }).joinToString(" · "),
        subtitle =
            listOfNotNull(
                minutesText(c.owed.minutes),
                formatMoney(c.owed.dueCents, language),
                c.placePays?.let { stringResource(R.string.pay_byPlace) },
            ).joinToString(" · "),
        badge = leftBadge(c.left, c.owed.dueCents),
        onClick = { nav.navigate(LessonDetail(c.owed.lessonId)) },
    )
}

/**
 * At the end of a payer's detail, styled like its other sections: Aulas (the last few, with Ver
 * todas) and Pagamentos (AC-008-9, 11, 26, 32).
 */
@Composable
fun PayerHistory(
    nav: NavHostController,
    payer: PayerRef,
) {
    val services = LocalServices.current
    val language = currentLanguage()
    val ledger by remember(payer) { services.paymentRepo.ledger(payer) }.collectAsState(initial = null)
    val classes = payerClasses(payer) ?: return
    val l = ledger ?: return
    val showWho = payer.kind == PayerKind.PLACE
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        SectionHeader(
            "${stringResource(R.string.pay_lessonsAll)} · ${classes.size}",
            aside = if (classes.size > RECENT) stringResource(R.string.pay_seeAll) else null,
            onAside = { nav.navigate(PayerLessons(payer.kind.key, payer.id)) },
        )
        if (classes.isEmpty()) {
            Text(stringResource(R.string.pay_noLessons), style = TutoraTheme.type.body, color = TutoraTheme.colors.inkMuted)
        } else {
            ListGroup(classes.take(RECENT).map { c -> { PayerClassRow(nav, c, showWho) } })
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        SectionHeader("${stringResource(R.string.pay_payments)} · ${l.payments.size}")
        if (l.payments.isEmpty()) {
            Text(stringResource(R.string.pay_none), style = TutoraTheme.type.body, color = TutoraTheme.colors.inkMuted)
        } else {
            ListGroup(
                l.payments.map { p ->
                    {
                        ListRow(
                            "+ " + formatMoney(p.cents, language),
                            subtitle = paymentLine(p, l, services.zone),
                            icon = IconName.WALLET,
                            onClick = { nav.navigate(PaymentDetail(p.id)) },
                        )
                    }
                },
            )
        }
    }
}

private const val RECENT = 3

/** Ver todas: every class of a payer, filtered by A receber or Pagas (AC-008-32). */
@Composable
fun PayerLessonsScreen(
    nav: NavHostController,
    kind: String,
    id: String,
) {
    val payer = PayerRef(PayerKind.fromKey(kind) ?: PayerKind.STUDENT, id)
    var filter by rememberSaveable { mutableStateOf("all") }
    ScreenScaffold(
        stringResource(R.string.pay_lessonsAll),
        "payer-lessons",
        variant = HeaderVariant.PUSH,
        onBack = { nav.popBackStack() },
    ) {
        val classes = payerClasses(payer) ?: return@ScreenScaffold
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            val filters = listOf("all" to R.string.pay_filterAll, "owed" to R.string.pay_filterOwed, "paid" to R.string.pay_filterPaid)
            filters.forEach { (k, label) -> Chip(stringResource(label), filter == k, { filter = k }) }
        }
        val shown =
            classes.filter {
                when (filter) {
                    "owed" -> it.left > 0
                    "paid" -> it.left == 0L
                    else -> true
                }
            }
        if (shown.isEmpty()) {
            Text(stringResource(R.string.pay_noLessons), style = TutoraTheme.type.body, color = TutoraTheme.colors.inkMuted)
        } else {
            ListGroup(shown.map { c -> { PayerClassRow(nav, c, payer.kind == PayerKind.PLACE) } })
        }
    }
}

/** Usar crédito: the payer's money or bonus minutes on the classes ticked (AC-008-30, 31). */
@Composable
private fun UseCreditSheet(
    payer: PayerRef,
    ledger: Ledger,
    onDismiss: () -> Unit,
) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val language = currentLanguage()
    var minutes by rememberSaveable { mutableStateOf(ledger.creditCents <= 0) }
    var chosen by rememberSaveable { mutableStateOf(emptyList<String>()) }
    BottomSheet(
        stringResource(R.string.pay_useCredit),
        onDismiss,
        subtitle =
            listOfNotNull(
                ledger.creditCents.takeIf { it > 0 }?.let { stringResource(R.string.pay_creditLine, formatMoney(it, language)) },
                ledger.creditMinutes.takeIf { it > 0 }?.let { stringResource(R.string.pay_minutesLine, minutesText(it)) },
            ).joinToString(" · "),
        actions =
            listOf { m ->
                Button(stringResource(R.string.pay_useSave), {
                    scope.launch {
                        services.payments.useCredit(payer, chosen, minutes)
                        onDismiss()
                    }
                }, m, enabled = chosen.isNotEmpty())
            },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            if (ledger.creditCents > 0 && ledger.creditMinutes > 0) {
                SegmentedControl(
                    listOf(false to stringResource(R.string.pay_useMoney), true to stringResource(R.string.pay_useMinutes)),
                    minutes,
                    { minutes = it },
                    stringResource(R.string.pay_useCredit),
                    size = ControlSize.SM,
                )
            }
            ledger.owed
                .filter { (ledger.allocation.left[it.lessonId] ?: 0) > 0 }
                .sortedByDescending { it.startAt }
                .forEach { c ->
                    val label =
                        listOf(
                            dayMonth(localOf(c.startAt, services.zone).date, language),
                            minutesText(c.minutes),
                            stringResource(R.string.pay_leftOn, formatMoney(ledger.allocation.left[c.lessonId] ?: 0, language)),
                        ).joinToString(" · ")
                    Checkbox(label, c.lessonId in chosen, { on -> chosen = if (on) chosen + c.lessonId else chosen - c.lessonId })
                }
        }
    }
}

/** Dar bônus, or edit or remove one (AC-008-31, 37): free money, free minutes, or both. */
@Composable
private fun BonusSheet(
    payer: PayerRef,
    editing: CreditRecord?,
    onDismiss: () -> Unit,
) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    var cents by rememberSaveable { mutableStateOf(editing?.cents?.takeIf { it > 0 }) }
    var minutes by rememberSaveable { mutableStateOf((editing?.minutes ?: 0).toLong()) }
    // A bonus is money or minutes, never both (AC-008-31).
    var inMinutes by rememberSaveable { mutableStateOf((editing?.minutes ?: 0) > 0) }
    val money = if (inMinutes) 0L else cents ?: 0
    val mins = if (inMinutes) minutes.toInt() else 0
    var note by rememberSaveable { mutableStateOf(editing?.note.orEmpty()) }
    // "30 min" or "00:30", as Ajustes says; the stepper's format can't read it itself.
    val clock = LocalClockDurations.current
    val minutesTemplate = stringResource(R.string.places_minutes)
    val save: @Composable (Modifier) -> Unit = { m ->
        Button(stringResource(if (editing == null) R.string.pay_bonusSave else R.string.catalog_save), {
            scope.launch {
                if (editing == null) {
                    services.payments.giveBonus(payer, money, mins, note)
                } else {
                    services.payments.editBonus(editing.id, money, mins, note)
                }
                onDismiss()
            }
        }, m, enabled = money > 0 || mins > 0)
    }
    val remove: @Composable (Modifier) -> Unit = { m ->
        Button(stringResource(R.string.pay_removeBonus), {
            scope.launch {
                editing?.let { services.payments.removeCredit(it.id) }
                onDismiss()
            }
        }, m, variant = ButtonVariant.DANGER)
    }
    BottomSheet(
        stringResource(if (editing == null) R.string.pay_giveBonus else R.string.pay_editBonus),
        onDismiss,
        actions = if (editing == null) listOf(save) else listOf(remove, save),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            SegmentedControl(
                listOf(false to stringResource(R.string.pay_useMoney), true to stringResource(R.string.pay_useMinutes)),
                inMinutes,
                { inMinutes = it },
                stringResource(R.string.pay_bonusKind),
                size = ControlSize.SM,
            )
            if (inMinutes) {
                Stepper(
                    minutes,
                    { minutes = it },
                    step = 15,
                    min = 0,
                    max = 600,
                    format = { if (clock) clockDuration(it.toInt()) else minutesTemplate.format(it.toString()) },
                    valueLabel = stringResource(R.string.pay_bonusMinutes),
                )
            } else {
                MoneyField(stringResource(R.string.pay_bonusMoney), cents, { cents = it })
            }
            TextField(stringResource(R.string.pay_note), note, { note = it })
        }
    }
}

/** "16/10 · Pix · 4 aulas", "· pacote" for a package's purchase, or just "29/10 · Pix" when it paid no class (AC-008-11). */
@Composable
fun paymentLine(
    p: PaymentRecord,
    ledger: Ledger,
    zone: String,
): String {
    val language = currentLanguage()
    val count =
        ledger.allocation.paidBy[p.id]
            .orEmpty()
            .size
    // A payment that paid no class says nothing more: the money is a tip, never credit (AC-016-24).
    val what =
        when {
            p.packageId != null -> stringResource(R.string.pay_packageBought)
            count == 0 -> null
            else -> pluralStringResource(R.plurals.pay_lessonsCount, count, count)
        }
    return listOfNotNull(dayMonth(localOf(p.paidAt, zone).date, language), methodName(p.method), what).joinToString(" · ")
}

/**
 * Registrar pagamento: amount (starting at what is owed), method, date, note, which classes it
 * pays, and "Escolher aulas" (AC-008-5 to 7). [editing] reuses it for Editar (AC-008-12);
 * [preChosen] opens it with those classes ticked (AC-008-18).
 */
@Composable
fun PaymentSheet(
    payer: PayerRef,
    ledger: Ledger,
    onDismiss: () -> Unit,
    editing: PaymentRecord? = null,
    preChosen: List<String> = emptyList(),
    /** On a student's sheet: their classes a place owes, which this payment can pay too (AC-008-27). */
    elsewhere: List<OwedElsewhere> = emptyList(),
) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val language = currentLanguage()
    val zone = services.zone
    val places by remember { services.catalogRepo.places() }.collectAsState(initial = emptyList())
    val placeNames = places.associate { it.place.id to placeName(it.place) }
    val outside = elsewhere.associateBy { it.owed.lessonId }
    val today = localOf(services.now(), zone).date
    // Without the payment being edited, what each class still owes.
    val others = remember(ledger, editing) { ledger.with(null, without = editing?.id) }
    val owedNow = others.left.values.sum()

    fun leftOn(ids: List<String>) = ids.sumOf { others.left[it] ?: outside[it]?.left ?: 0 }
    var cents by rememberSaveable {
        mutableStateOf<Cents?>(editing?.cents ?: if (preChosen.isNotEmpty()) leftOn(preChosen) else owedNow.coerceAtLeast(0))
    }
    var method by rememberSaveable { mutableStateOf(editing?.method ?: PayMethod.PIX) }
    var date by rememberSaveable { mutableStateOf((editing?.paidAt?.let { localOf(it, zone).date } ?: today).toString()) }
    var note by rememberSaveable { mutableStateOf(editing?.note.orEmpty()) }
    var chosen by rememberSaveable { mutableStateOf(editing?.chosen ?: preChosen) }
    var choosing by rememberSaveable { mutableStateOf(preChosen.isNotEmpty() || editing?.chosen?.isNotEmpty() == true) }
    if (editing == null) {
        LaunchedEffect(payer) { method = services.payments.lastMethod(payer) }
    }
    val amount = cents ?: 0
    val picked = LocalDate.parse(date)
    // Which classes this payment would pay, as the teacher types (AC-008-6).
    val preview =
        remember(amount, chosen, date, others, elsewhere) {
            // Classes a place owes are paid first, as the place's payment; the rest goes to this payer.
            val there = chosen.filter { (outside[it]?.left ?: 0) > 0 }
            val toPlaces = minOf(amount, there.sumOf { outside[it]?.left ?: 0 })
            val draft =
                Paid("draft", if (picked == today) services.now() else "${picked}T15:00:00.000Z", amount - toPlaces, chosen - there.toSet())
            there + ledger.with(draft, without = editing?.id).paidBy["draft"].orEmpty()
        }
    val byId = ledger.owed.associateBy { it.lessonId } + elsewhere.associate { it.owed.lessonId to it.owed }
    // What the classes it pays still owe; anything above is extra, a tip, never credit (AC-016-24).
    val covers = preview.sumOf { others.left[it] ?: outside[it]?.left ?: 0 }
    val extra = (amount - covers).coerceAtLeast(0)
    val previewDates = preview.mapNotNull { byId[it]?.let { l -> dayMonth(localOf(l.startAt, zone).date, language) } }
    BottomSheet(
        stringResource(if (editing == null) R.string.pay_record else R.string.pay_edit),
        onDismiss,
        actions =
            listOf { m ->
                Button(
                    stringResource(R.string.pay_save, formatMoney(amount, language)),
                    {
                        scope.launch {
                            val draft = PaymentDraft(payer, amount, method, picked, note, chosen)
                            if (editing == null) services.payments.record(draft) else services.payments.edit(editing.id, draft)
                            onDismiss()
                        }
                    },
                    m,
                    enabled = amount > 0,
                )
            },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            // Where the payer stands before this payment: Em dia, or Deve with how much (AC-016-25).
            val owing = owedNow + elsewhere.sumOf { it.left }
            Box(Modifier.testTag("pay-status")) {
                if (owing > 0) {
                    Badge(stringResource(R.string.pay_owesTotal, formatMoney(owing, language)), tone = BadgeTone.DANGER)
                } else {
                    Badge(stringResource(R.string.pay_settled), tone = BadgeTone.SUCCESS)
                }
            }
            MoneyField(stringResource(R.string.pay_amount), cents, { cents = it })
            if (previewDates.isNotEmpty()) {
                Text(
                    pluralStringResource(R.plurals.pay_preview, previewDates.size, previewDates.size, joined(previewDates)),
                    Modifier.testTag("pay-preview"),
                    style = TutoraTheme.type.meta,
                    color = TutoraTheme.colors.inkMuted,
                )
            }
            // A caution, never a block: the payment pays no class, or is more than they owe (AC-016-24).
            if (amount > 0 && previewDates.isEmpty()) {
                AlertBanner(stringResource(R.string.pay_noClassPaid), Modifier.testTag("pay-caution"), tone = AlertTone.ACCENT)
            } else if (extra > 0) {
                AlertBanner(
                    stringResource(R.string.pay_extra, formatMoney(extra, language)),
                    Modifier.testTag("pay-caution"),
                    tone = AlertTone.ACCENT,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                PayMethod.entries.forEach { mth -> Chip(methodName(mth), method == mth, { method = mth }) }
            }
            DateField(stringResource(R.string.lesson_date), dayMonth(picked, language), picked, { if (it <= today) date = it.toString() })
            TextField(stringResource(R.string.pay_note), note, { note = it })
            // Escolher aulas is a switch: off pays the oldest first, on lists the classes (Victor, 29 Oct 2026).
            Row(
                // The whole line flips it, words included.
                Modifier.fillMaxWidth().clickable { choosing = !choosing },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            ) {
                Text(
                    stringResource(R.string.pay_choose),
                    style = TutoraTheme.type.bodyStrong,
                    color = TutoraTheme.colors.ink,
                    modifier = Modifier.weight(1f),
                )
                Switch(choosing, { choosing = it }, stringResource(R.string.pay_choose))
            }
            // Newest first, with what is still owed on each (AC-008-7); a place's classes say so.
            val choosable =
                (
                    ledger.owed.map { Triple(it, others.left[it.lessonId] ?: 0, null as String?) } +
                        elsewhere.map { Triple(it.owed, it.left, placeNames[it.payer.id]) }
                ).filter { (c, left, _) -> left > 0 || c.lessonId in chosen }
                    .sortedByDescending { it.first.startAt }
            if (choosing && choosable.isEmpty()) {
                Text(stringResource(R.string.pay_noneToChoose), style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
            }
            if (choosing) {
                choosable.forEach { (lesson, left, place) ->
                    val label =
                        listOfNotNull(
                            dayMonth(localOf(lesson.startAt, zone).date, language),
                            minutesText(lesson.minutes),
                            place?.let { stringResource(R.string.pay_placePays, it) },
                            stringResource(R.string.pay_leftOn, formatMoney(left, language)),
                        ).joinToString(" · ")
                    Checkbox(label, lesson.lessonId in chosen, { on ->
                        chosen = if (on) chosen + lesson.lessonId else chosen - lesson.lessonId
                        cents = leftOn(chosen)
                    })
                }
            }
        }
    }
}

/** "15/10 e 22/10" / "10/15 and 10/22". */
@Composable
private fun joined(items: List<String>): String =
    if (items.size == 1) items.first() else items.dropLast(1).joinToString(", ") + stringResource(R.string.pay_and) + items.last()

/** A payment: amount, method, date, note and the classes it paid, with Editar and Excluir (AC-008-12). */
@Composable
fun PaymentDetailScreen(
    nav: NavHostController,
    id: String,
) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val language = currentLanguage()
    val payment by remember(id) { services.paymentRepo.payment(id) }.collectAsState(initial = null)
    val p = payment ?: return
    val ledger by remember(p.payer) { services.paymentRepo.ledger(p.payer) }.collectAsState(initial = null)
    val l = ledger ?: return
    var editing by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    val full = l.payments.firstOrNull { it.id == id } ?: p
    val date = localOf(p.paidAt, services.zone).date
    ScreenScaffold(
        stringResource(R.string.pay_detail),
        "payment-detail",
        variant = HeaderVariant.PUSH,
        onBack = { nav.popBackStack() },
        actions =
            listOfNotNull(
                HeaderAction(IconName.TRASH, stringResource(R.string.pay_delete)) { deleting = true },
                if (p.packageId == null) HeaderAction(IconName.PENCIL, stringResource(R.string.catalog_edit)) { editing = true } else null,
            ),
    ) {
        Card {
            Money(p.cents, size = MoneySize.LG, tone = MoneyTone.PAID)
            Text(paymentLine(full, l, services.zone), style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
            p.note?.let { Text(it, style = TutoraTheme.type.body, color = TutoraTheme.colors.ink) }
        }
        val paid =
            l.allocation.paidBy[id]
                .orEmpty()
                .mapNotNull { lid -> l.owed.firstOrNull { it.lessonId == lid } }
        if (paid.isNotEmpty()) {
            ListGroup(
                paid.map { lesson ->
                    {
                        ListRow(
                            dayMonth(localOf(lesson.startAt, services.zone).date, language),
                            subtitle = formatMoney(lesson.dueCents, language),
                            badge = paidBadge(l, lesson),
                            onClick = { nav.navigate(LessonDetail(lesson.lessonId)) },
                        )
                    }
                },
                title = stringResource(R.string.pay_paidLessons),
            )
        }
    }
    if (editing) PaymentSheet(p.payer, l, onDismiss = { editing = false }, editing = full)
    if (deleting) {
        BottomSheet(
            stringResource(R.string.pay_deleteTitle, formatMoney(p.cents, language), dayMonth(date, language)),
            { deleting = false },
            subtitle = stringResource(R.string.pay_deleteBody),
            actions =
                listOf(
                    { m -> Button(stringResource(R.string.catalog_cancel), { deleting = false }, m, variant = ButtonVariant.OUTLINE) },
                    { m ->
                        Button(stringResource(R.string.pay_delete), {
                            scope.launch {
                                services.payments.delete(id)
                                deleting = false
                                nav.popBackStack()
                            }
                        }, m, variant = ButtonVariant.DANGER)
                    },
                ),
        )
    }
}

/** "7 de 10 usadas", "Resta 1 aula no pacote" (AC-008-21). */
@Composable
fun packageUseText(pkg: PackageState): String = pluralStringResource(R.plurals.pay_packageUse, pkg.total, pkg.used, pkg.total)

/** A package student's package: its use, the warning when one class is left, and Novo pacote (AC-008-20, 21). */
@Composable
fun PackageCard(
    studentId: String,
    priceCents: Cents,
) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val language = currentLanguage()
    val pkg by remember(studentId) { services.paymentRepo.lastPackage(studentId) }.collectAsState(initial = null)
    var buying by rememberSaveable { mutableStateOf(false) }
    Card(Modifier.testTag("package"), label = stringResource(R.string.pay_package)) {
        Text(stringResource(R.string.pay_package), style = TutoraTheme.type.caption, color = TutoraTheme.colors.inkMuted)
        val p = pkg
        if (p != null) {
            ProgressBar(p.used.toFloat() / p.total.coerceAtLeast(1), packageUseText(p), tone = ProgressTone.ACCENT)
            Text(packageUseText(p), style = TutoraTheme.type.bodyStrong, color = TutoraTheme.colors.ink)
            if (p.lastOne) {
                Text(
                    stringResource(R.string.pay_packageLastOne),
                    style = TutoraTheme.type.meta,
                    color = TutoraTheme.colors.dangerInk,
                )
            }
        } else {
            Text(stringResource(R.string.pay_packageNone), style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
        }
        Button(stringResource(R.string.pay_packageNew), { buying = true }, variant = ButtonVariant.OUTLINE, fullWidth = true)
    }
    if (buying) {
        val today = localOf(services.now(), services.zone).date
        var method by rememberSaveable { mutableStateOf(PayMethod.PIX) }
        var date by rememberSaveable { mutableStateOf(today.toString()) }
        val picked = LocalDate.parse(date)
        BottomSheet(
            stringResource(R.string.pay_packageNew),
            { buying = false },
            actions =
                listOf { m ->
                    Button(stringResource(R.string.pay_save, formatMoney(priceCents, language)), {
                        scope.launch {
                            services.payments.buyPackage(studentId, method, picked)
                            buying = false
                        }
                    }, m)
                },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                    PayMethod.entries.forEach { mth -> Chip(methodName(mth), method == mth, { method = mth }) }
                }
                DateField(stringResource(R.string.lesson_date), dayMonth(picked, language), picked, {
                    if (it <=
                        today
                    ) {
                        date = it.toString()
                    }
                })
            }
        }
    }
}

/** A place that pays the teacher, which has its own balance (AC-008-14). */
fun paysTeacher(place: Place): Boolean = place.payer == Payer.PLACE && place.kind != PlaceKind.SCHOOL

/** Receber da salinha / do cursinho / do local. */
@Composable
fun receiveLabel(kind: PlaceKind): String =
    stringResource(
        when (kind) {
            PlaceKind.SALINHA -> R.string.pay_receiveSalinha
            PlaceKind.COURSE -> R.string.pay_receiveCourse
            else -> R.string.pay_receivePlace
        },
    )

/** "Saldo da aluna · 2 aulas a receber · R$ 240,00", opening the payer (AC-008-16). */
@Composable
fun SaldoRow(
    payer: PayerRef,
    title: String,
    onOpen: () -> Unit,
) {
    val services = LocalServices.current
    val ledger by remember(payer) { services.paymentRepo.ledger(payer) }.collectAsState(initial = null)
    val l = ledger ?: return
    val count = l.owed.count { !l.allocation.isPaid(it.lessonId) }
    val (tone, text) = balanceBadge(l.dueCents - l.paidCents)
    ListRow(
        title,
        Modifier.testTag("saldo"),
        subtitle = pluralStringResource(R.plurals.pay_toReceiveCount, count, count),
        icon = IconName.WALLET,
        badge = tone to text,
        trailing = RowTrailing.CHEVRON,
        onClick = onOpen,
    )
}

/** Who a class's money belongs to, and their name; null when a salary covers it. */
@Composable
fun payerOfLesson(lesson: Lesson): Pair<PayerRef, String>? {
    if (lesson.place.kind == PlaceKind.SCHOOL) return null
    val payer = payerOf(lesson.place, lesson.who)
    val name =
        when {
            payer.kind == PayerKind.PLACE -> placeName(lesson.place)
            else ->
                when (val w = lesson.who) {
                    is Who.OfStudent -> w.student.name
                    is Who.OfGroup -> w.group.name
                }
        }
    return payer to name
}

/** On a lesson's detail: Pago or A receber for this class, and the payer's Saldo (AC-008-16). */
@Composable
fun LessonMoney(
    nav: NavHostController,
    lesson: Lesson,
) {
    val services = LocalServices.current
    val (payer, name) = payerOfLesson(lesson) ?: return
    val ledger by remember(payer) { services.paymentRepo.ledger(payer) }.collectAsState(initial = null)
    val l = ledger ?: return
    var paying by rememberSaveable { mutableStateOf(false) }
    val mine = l.owed.firstOrNull { it.lessonId == lesson.id }
    if (mine != null) {
        val (tone, text) = paidBadge(l, mine)
        Row(
            Modifier.testTag("lesson-paid"),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Badge(text, tone = tone, icon = if (tone == BadgeTone.SUCCESS) IconName.CHECK else null)
            // Marcar como paga: a payment from whoever owes it, with this class ticked (AC-008-25).
            if ((l.allocation.left[mine.lessonId] ?: 0) > 0) {
                Button(stringResource(R.string.pay_markPaid), { paying = true }, variant = ButtonVariant.OUTLINE, icon = IconName.WALLET)
            }
        }
    }
    if (paying && mine != null) PaymentSheet(payer, l, onDismiss = { paying = false }, preChosen = listOf(mine.lessonId))
    SaldoRow(payer, stringResource(R.string.pay_saldoOf, name)) { nav.navigate(payerRoute(payer)) }
}

/** The detail that holds a payer's balance. */
fun payerRoute(payer: PayerRef): Any =
    when (payer.kind) {
        PayerKind.STUDENT -> StudentDetail(payer.id)
        PayerKind.GROUP -> GroupDetail(payer.id)
        PayerKind.PLACE -> PlaceDetailRoute(payer.id)
    }

/**
 * Hoje: "3 alunos com pagamento pendente · R$ 560", hidden when nobody owes (AC-008-15). It opens
 * whoever owes: their detail when only one does, Alunos › Devendo when all are students, and
 * otherwise a list of everyone who owes.
 */
@Composable
fun PendingAlert(nav: NavHostController) {
    val services = LocalServices.current
    val language = currentLanguage()
    val balances by remember { services.paymentRepo.balances() }.collectAsState(initial = emptyMap())
    val students by remember { services.catalogRepo.students() }.collectAsState(initial = emptyList())
    val groups by remember { services.catalogRepo.groups() }.collectAsState(initial = emptyList())
    val places by remember { services.catalogRepo.places() }.collectAsState(initial = emptyList())
    var listing by rememberSaveable { mutableStateOf(false) }
    val owing = balances.filterValues { it > 0 }
    if (owing.isEmpty()) return
    val count = owing.size
    val onlyStudents = owing.keys.all { it.kind == PayerKind.STUDENT }
    val message =
        if (onlyStudents) {
            pluralStringResource(R.plurals.pay_pendingStudents, count, count)
        } else {
            pluralStringResource(R.plurals.pay_pendingAlert, count, count)
        }
    AlertBanner(
        message,
        Modifier.testTag("pending-alert"),
        value = formatMoney(owing.values.sum(), language, compact = true),
        onClick = {
            val only = owing.keys.singleOrNull()
            when {
                only != null -> nav.navigate(payerRoute(only))
                onlyStudents -> nav.navigate(RegistryHome(owing = true))
                else -> listing = true
            }
        },
    )
    if (listing) {
        val names =
            students.associate { PayerRef(PayerKind.STUDENT, it.student.id) to it.student.name } +
                groups.associate { PayerRef(PayerKind.GROUP, it.id) to it.name } +
                places.associate { PayerRef(PayerKind.PLACE, it.place.id) to placeName(it.place) }
        BottomSheet(message, { listing = false }) {
            ListGroup(
                owing.entries.sortedByDescending { it.value }.map { (payer, cents) ->
                    {
                        ListRow(
                            names[payer].orEmpty(),
                            icon =
                                when (payer.kind) {
                                    PayerKind.STUDENT -> IconName.USERS
                                    PayerKind.GROUP -> IconName.USERS
                                    PayerKind.PLACE -> IconName.PIN
                                },
                            badge = balanceBadge(cents),
                            onClick = {
                                listing = false
                                nav.navigate(payerRoute(payer))
                            },
                        )
                    }
                },
            )
        }
    }
}

/**
 * Aula concluída: "Adicionado ao saldo da aluna. Saldo pendente agora: R$ 360,00" and Registrar
 * pagamento with this class ticked (AC-008-18), and "Resta 1 aula no pacote" (AC-008-21).
 */
@Composable
fun DoneMoney(lesson: Lesson) {
    val services = LocalServices.current
    val language = currentLanguage()
    val (payer, name) = payerOfLesson(lesson) ?: return
    val ledger by remember(payer) { services.paymentRepo.ledger(payer) }.collectAsState(initial = null)
    val student = (lesson.who as? Who.OfStudent)?.student
    val pkg =
        student?.let { services.paymentRepo.lastPackage(it.id) }?.collectAsState(initial = null)?.value
    var paying by rememberSaveable { mutableStateOf(false) }
    val l = ledger ?: return
    val owed = (l.balance as? Balance.Owes)?.cents ?: 0
    Text(
        stringResource(R.string.pay_addedToBalance, name, formatMoney(owed, language)),
        Modifier.testTag("done-balance"),
        style = TutoraTheme.type.body,
        color = TutoraTheme.colors.inkMuted,
    )
    if (pkg?.lastOne == true && student.rateType == RateType.PACKAGE) {
        Text(stringResource(R.string.pay_packageLastOne), style = TutoraTheme.type.bodyStrong, color = TutoraTheme.colors.dangerInk)
    }
    Button(stringResource(R.string.pay_record), { paying = true }, icon = IconName.WALLET, variant = ButtonVariant.ACCENT, fullWidth = true)
    if (paying) {
        val mine = l.owed.filter { it.lessonId == lesson.id }.map { it.lessonId }
        PaymentSheet(payer, l, onDismiss = { paying = false }, preChosen = mine)
    }
}

/** What the teacher chose to use on a class as it is marked done: money (cents) or minutes (AC-008-39). */
data class BonusChoice(
    val payer: PayerRef,
    val minutes: Boolean,
    val amount: Long,
)

/** The credit and bonus minutes a class's payer can use on it, with a label; null when there is none or the switch is off. */
data class BonusOffer(
    val payer: PayerRef,
    val cents: Cents,
    val minutes: Int,
    val label: String,
)

@Composable
fun bonusOffer(lesson: Lesson): BonusOffer? {
    val services = LocalServices.current
    val language = currentLanguage()
    val settings by services.settings.collectAsState(initial = null)
    val payer = payerOfLesson(lesson)?.first
    val ledger by remember(payer) { payer?.let { services.paymentRepo.ledger(it) } ?: flowOf(null) }.collectAsState(initial = null)
    val l = ledger ?: return null
    if (settings?.credits != true || payer == null || payer.kind == PayerKind.PLACE) return null
    if (l.creditCents <= 0 && l.creditMinutes <= 0) return null
    val label =
        listOfNotNull(
            l.creditCents.takeIf { it > 0 }?.let { formatMoney(it, language) },
            l.creditMinutes.takeIf { it > 0 }?.let { minutesText(it) },
        ).joinToString(" · ")
    return BonusOffer(payer, l.creditCents.coerceAtLeast(0), l.creditMinutes.coerceAtLeast(0), label)
}
