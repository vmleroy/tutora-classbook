package app.tutora.ui.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import app.tutora.designsystem.components.Badge
import app.tutora.designsystem.components.BadgeTone
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.ButtonVariant
import app.tutora.designsystem.components.Card
import app.tutora.designsystem.components.Checkbox
import app.tutora.designsystem.components.Chip
import app.tutora.designsystem.components.DateField
import app.tutora.designsystem.components.EmptyState
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
import app.tutora.designsystem.components.TextField
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.Balance
import app.tutora.domain.Cents
import app.tutora.domain.Language
import app.tutora.domain.Ledger
import app.tutora.domain.Lesson
import app.tutora.domain.OwedClass
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
import app.tutora.ui.PaymentDetail
import app.tutora.ui.RegistryHome
import app.tutora.ui.ScreenScaffold
import app.tutora.ui.StudentDetail
import app.tutora.ui.localeOf
import app.tutora.ui.placeName
import app.tutora.usecases.PaymentDraft
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

/** Pago, or A receber with what is left when paid in part (AC-008-9). */
@Composable
fun paidBadge(
    ledger: Ledger,
    lesson: OwedClass,
): Pair<BadgeTone, String> {
    val left = ledger.allocation.left[lesson.lessonId] ?: 0
    return when {
        left == 0L -> BadgeTone.SUCCESS to stringResource(R.string.pay_paid)
        left < lesson.dueCents ->
            BadgeTone.DANGER to stringResource(R.string.pay_leftPart, formatMoney(left, currentLanguage()))
        else -> BadgeTone.DANGER to stringResource(R.string.pay_toReceive)
    }
}

private fun OwedClass.localDate(zone: String) = localOf(startAt, zone).date

/** At the top of a payer's detail: what it owes, with its pay button (AC-008-11, 14). */
@Composable
fun PayerBalance(
    payer: PayerRef,
    payLabel: String,
    isPlace: Boolean = false,
) {
    val services = LocalServices.current
    val ledger by services.paymentRepo.ledger(payer).collectAsState(initial = null)
    var paying by rememberSaveable { mutableStateOf(false) }
    val l = ledger ?: return
    val balance = l.balance
    Card(Modifier.testTag("balance")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            Column(Modifier.weight(1f)) {
                Text(
                    when (balance) {
                        is Balance.Owes -> stringResource(if (isPlace) R.string.pay_placeOwesLabel else R.string.pay_owesLabel)
                        is Balance.Credit -> stringResource(R.string.pay_creditLabel)
                        Balance.Settled -> stringResource(R.string.pay_settled)
                    },
                    style = TutoraTheme.type.caption,
                    color = TutoraTheme.colors.inkMuted,
                )
                when (balance) {
                    is Balance.Owes -> Money(balance.cents, size = MoneySize.LG, tone = MoneyTone.OWED)
                    is Balance.Credit -> Money(balance.cents, size = MoneySize.LG, tone = MoneyTone.PAID)
                    Balance.Settled -> Money(0, size = MoneySize.LG, tone = MoneyTone.PAID)
                }
            }
        }
        Button(payLabel, { paying = true }, icon = IconName.WALLET, fullWidth = true)
    }
    if (paying) PaymentSheet(payer, l, onDismiss = { paying = false })
}

/** At the end of a payer's detail: its classes with Pago or A receber, and Pagamentos (AC-008-9, 11). */
@Composable
fun PayerHistory(
    nav: NavHostController,
    payer: PayerRef,
) {
    val services = LocalServices.current
    val language = currentLanguage()
    val ledger by services.paymentRepo.ledger(payer).collectAsState(initial = null)
    val l = ledger ?: return
    val owed = l.owed.sortedByDescending { it.startAt }
    if (owed.isNotEmpty()) {
        ListGroup(
            owed.take(RECENT).map { lesson ->
                {
                    ListRow(
                        dayMonth(lesson.localDate(services.zone), language),
                        subtitle =
                            stringResource(R.string.places_minutes, lesson.minutes.toString()) + " · " +
                                formatMoney(lesson.dueCents, language),
                        badge = paidBadge(l, lesson),
                        onClick = { nav.navigate(LessonDetail(lesson.lessonId)) },
                    )
                }
            },
            title = stringResource(R.string.pay_lessons),
        )
    }
    SectionHeader(stringResource(R.string.pay_payments))
    if (l.payments.isEmpty()) {
        EmptyState(stringResource(R.string.pay_none), icon = IconName.WALLET)
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

private const val RECENT = 10

/** "16/10 · Pix · 4 aulas", or "· pacote" for a package's purchase (AC-008-11). */
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
    val what =
        if (p.packageId != null) {
            stringResource(R.string.pay_packageBought)
        } else {
            pluralStringResource(R.plurals.pay_lessonsCount, count, count)
        }
    return listOf(dayMonth(localOf(p.paidAt, zone).date, language), methodName(p.method), what).joinToString(" · ")
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
) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val language = currentLanguage()
    val zone = services.zone
    val today = localOf(services.now(), zone).date
    // Without the payment being edited, what each class still owes.
    val others = remember(ledger, editing) { ledger.with(null, without = editing?.id) }
    val owedNow = others.left.values.sum() - others.credit

    fun leftOn(ids: List<String>) = ids.sumOf { others.left[it] ?: 0 }
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
        remember(amount, chosen, date, others) {
            val draft = Paid("draft", if (picked == today) services.now() else "${picked}T15:00:00.000Z", amount, chosen)
            ledger.with(draft, without = editing?.id).paidBy["draft"].orEmpty()
        }
    val byId = ledger.owed.associateBy { it.lessonId }
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
            MoneyField(stringResource(R.string.pay_amount), cents, { cents = it })
            Text(
                if (previewDates.isEmpty()) {
                    stringResource(R.string.pay_previewCredit)
                } else {
                    pluralStringResource(R.plurals.pay_preview, previewDates.size, previewDates.size, joined(previewDates))
                },
                Modifier.testTag("pay-preview"),
                style = TutoraTheme.type.meta,
                color = TutoraTheme.colors.inkMuted,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                PayMethod.entries.forEach { mth -> Chip(methodName(mth), method == mth, { method = mth }) }
            }
            DateField(stringResource(R.string.lesson_date), dayMonth(picked, language), picked, { if (it <= today) date = it.toString() })
            TextField(stringResource(R.string.pay_note), note, { note = it })
            Button(
                stringResource(R.string.pay_choose),
                { choosing = !choosing },
                variant = ButtonVariant.GHOST,
                icon = IconName.CHECK,
            )
            val choosable = ledger.owed.filter { (others.left[it.lessonId] ?: 0) > 0 || it.lessonId in chosen }
            if (choosing && choosable.isEmpty()) {
                Text(stringResource(R.string.pay_noneToChoose), style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
            }
            if (choosing) {
                // Newest first, with what is still owed on each (AC-008-7).
                ledger.owed
                    .sortedByDescending { it.startAt }
                    .filter { (others.left[it.lessonId] ?: 0) > 0 || it.lessonId in chosen }
                    .forEach { lesson ->
                        val label =
                            dayMonth(localOf(lesson.startAt, zone).date, language) + " · " +
                                stringResource(R.string.places_minutes, lesson.minutes.toString()) + " · " +
                                stringResource(R.string.pay_leftOn, formatMoney(others.left[lesson.lessonId] ?: 0, language))
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
    val payment by services.paymentRepo.payment(id).collectAsState(initial = null)
    val p = payment ?: return
    val ledger by services.paymentRepo.ledger(p.payer).collectAsState(initial = null)
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
    val pkg by services.paymentRepo.lastPackage(studentId).collectAsState(initial = null)
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

/** "A receber da salinha: R$ 360" on Locais. */
@Composable
fun placeOwesText(
    kind: PlaceKind,
    cents: Cents,
): String =
    stringResource(
        when (kind) {
            PlaceKind.SALINHA -> R.string.pay_toReceiveSalinha
            PlaceKind.COURSE -> R.string.pay_toReceiveCourse
            else -> R.string.pay_toReceivePlace
        },
        formatMoney(cents, currentLanguage(), compact = true),
    )

/** "Saldo da aluna · 2 aulas a receber · R$ 240,00", opening the payer (AC-008-16). */
@Composable
fun SaldoRow(
    payer: PayerRef,
    title: String,
    onOpen: () -> Unit,
) {
    val services = LocalServices.current
    val ledger by services.paymentRepo.ledger(payer).collectAsState(initial = null)
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
    val ledger by services.paymentRepo.ledger(payer).collectAsState(initial = null)
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
    val balances by services.paymentRepo.balances().collectAsState(initial = emptyMap())
    val students by services.catalogRepo.students().collectAsState(initial = emptyList())
    val groups by services.catalogRepo.groups().collectAsState(initial = emptyList())
    val places by services.catalogRepo.places().collectAsState(initial = emptyList())
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
    val ledger by services.paymentRepo.ledger(payer).collectAsState(initial = null)
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

/**
 * On a student's detail: classes a place owes instead of the student, with what that place still
 * owes for them, opening the place (AC-008-26).
 */
@Composable
fun PaidByPlace(
    nav: NavHostController,
    studentId: String,
) {
    val services = LocalServices.current
    val byPlace by services.paymentRepo.paidByPlace(studentId).collectAsState(initial = emptyMap())
    val places by services.catalogRepo.places().collectAsState(initial = emptyList())
    byPlace.forEach { (placeId, lessonIds) ->
        val place = places.firstOrNull { it.place.id == placeId }?.place ?: return@forEach
        val ledger by services.paymentRepo.ledger(PayerRef(PayerKind.PLACE, placeId)).collectAsState(initial = null)
        val left = ledger?.let { l -> lessonIds.sumOf { l.allocation.left[it] ?: 0 } } ?: return@forEach
        val name = placeName(place)
        ListRow(
            stringResource(R.string.pay_paidByPlace, name),
            Modifier.testTag("paid-by-place"),
            subtitle =
                if (left > 0) {
                    stringResource(R.string.pay_placeLeft, formatMoney(left, currentLanguage()), name)
                } else {
                    stringResource(R.string.pay_paid)
                },
            icon = IconName.PIN,
            onClick = { nav.navigate(PlaceDetailRoute(placeId)) },
        )
    }
}
