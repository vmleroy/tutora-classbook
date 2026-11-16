package app.tutora.ui.receipts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import app.tutora.R
import app.tutora.adapters.export.LocalExports
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.AlertBanner
import app.tutora.designsystem.components.AlertTone
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.ButtonVariant
import app.tutora.designsystem.components.EmptyState
import app.tutora.designsystem.components.HeaderAction
import app.tutora.designsystem.components.HeaderVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.SectionHeader
import app.tutora.designsystem.components.TextField
import app.tutora.designsystem.components.Thumbnail
import app.tutora.designsystem.components.ThumbnailSize
import app.tutora.designsystem.components.ZoomImage
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.AttachablePayment
import app.tutora.domain.PayerKind
import app.tutora.domain.PayerRef
import app.tutora.domain.Receipt
import app.tutora.domain.ReceiptKind
import app.tutora.domain.attachable
import app.tutora.domain.localOf
import app.tutora.state.LocalServices
import app.tutora.ui.PaymentDetail
import app.tutora.ui.ScreenScaffold
import app.tutora.ui.payments.PaymentSheet
import app.tutora.ui.payments.dayMonth
import app.tutora.ui.payments.methodName
import app.tutora.ui.placeName
import app.tutora.usecases.StageResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A payment's comprovantes full screen, one after another (AC-021-5): an image zooms, a PDF shows
 * its pages one under the other. Compartilhar, Substituir and Remover (which asks first) in the
 * header. Behind the app lock like every page (AC-021-11).
 */
@Composable
fun ReceiptViewerScreen(
    nav: NavHostController,
    paymentId: String,
    index: Int,
) {
    val services = LocalServices.current
    val exports = LocalExports.current
    val scope = rememberCoroutineScope()
    val loaded by remember(paymentId) { services.receiptRepo.forPayment(paymentId) }.collectAsState(initial = null)
    val receipts = loaded ?: return
    // The last one removed: nothing left to show.
    LaunchedEffect(receipts.isEmpty()) { if (receipts.isEmpty()) nav.popBackStack() }
    if (receipts.isEmpty()) return
    val pager = rememberPagerState(initialPage = index.coerceIn(0, receipts.lastIndex)) { receipts.size }
    val current = receipts[pager.currentPage.coerceIn(0, receipts.lastIndex)]
    var removing by rememberSaveable { mutableStateOf(false) }
    var replacing by rememberSaveable { mutableStateOf(false) }
    var refusal by remember { mutableStateOf<app.tutora.usecases.ReceiptRefusal?>(null) }
    ScreenScaffold(
        stringResource(R.string.rc_viewer),
        "receipt-viewer",
        variant = HeaderVariant.PUSH,
        onBack = { nav.popBackStack() },
        scroll = false,
        actions =
            listOf(
                HeaderAction(IconName.SHARE, stringResource(R.string.rc_share)) {
                    scope.launch {
                        val file = services.receipts.store.file(current.file)
                        val bytes = withContext(Dispatchers.IO) { runCatching { file.readBytes() }.getOrNull() }
                        if (bytes != null) exports.share(current.file, current.mime, bytes)
                    }
                },
                HeaderAction(IconName.REPLACE, stringResource(R.string.rc_replace)) { replacing = true },
                HeaderAction(IconName.TRASH, stringResource(R.string.rc_remove)) { removing = true },
            ),
    ) {
        Column(Modifier.fillMaxSize()) {
            refusal?.let { AlertBanner(refusalText(it), Modifier.padding(Spacing.s4), tone = AlertTone.ACCENT) }
            HorizontalPager(
                pager,
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(TutoraTheme.colors.surfaceSunken),
            ) { page -> ReceiptPage(receipts[page], page, receipts.size) }
            Text(
                listOf(
                    stringResource(R.string.rc_nOfM, pager.currentPage + 1, receipts.size),
                    if (current.kind == ReceiptKind.IMAGE) stringResource(R.string.rc_pinch) else null,
                ).filterNotNull().joinToString(" · "),
                Modifier.fillMaxWidth().padding(Spacing.s4).testTag("receipt-position"),
                style = TutoraTheme.type.meta,
                color = TutoraTheme.colors.inkMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
    if (replacing) {
        AddReceiptSheet({ replacing = false }) { uris ->
            replacing = false
            val uri = uris.firstOrNull() ?: return@AddReceiptSheet
            scope.launch {
                when (val r = withContext(Dispatchers.IO) { services.receipts.stage(uri) }) {
                    is StageResult.Ok -> {
                        refusal = null
                        services.receipts.replace(current.id, r.staged)
                    }
                    is StageResult.Refused -> refusal = r.reason
                }
            }
        }
    }
    if (removing) {
        BottomSheet(
            stringResource(R.string.rc_removeTitle),
            { removing = false },
            subtitle = stringResource(R.string.rc_removeBody),
            actions =
                listOf(
                    { m -> Button(stringResource(R.string.rc_cancel), { removing = false }, m, variant = ButtonVariant.OUTLINE) },
                    { m ->
                        Button(stringResource(R.string.rc_remove), {
                            val id = current.id
                            removing = false
                            scope.launch { services.receipts.remove(id) }
                        }, m.testTag("receipt-remove-confirm"), variant = ButtonVariant.DANGER)
                    },
                ),
        )
    }
}

/** One comprovante in the viewer: a zoomable image, or a PDF's pages one under the other. */
@Composable
private fun ReceiptPage(
    r: Receipt,
    index: Int,
    count: Int,
) {
    val services = LocalServices.current
    val images = LocalReceiptImages.current
    val file = services.receipts.store.file(r.file)
    val px =
        LocalWindowInfo.current.containerSize.width
            .coerceIn(THUMB_PX, MAX_PAGE_PX)
    val label = stringResource(R.string.rc_item, index + 1, count)
    if (r.kind == ReceiptKind.IMAGE) {
        val picture = rememberReceiptPicture(file, px = px, thumbnail = false)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (picture != null) {
                ZoomImage(picture, label, Modifier.fillMaxSize())
            } else {
                Thumbnail(label, null, size = ThumbnailSize.LG)
            }
        }
    } else {
        val pages by produceState(1, file) { value = withContext(Dispatchers.IO) { images.pages(file) } }
        LazyColumn(
            Modifier.fillMaxSize().testTag("receipt-pdf"),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(Spacing.s4),
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(pages) { page ->
                val picture = rememberReceiptPicture(file, page, px, thumbnail = false)
                val pageLabel = stringResource(R.string.rc_page, page + 1, pages)
                if (picture != null) {
                    androidx.compose.foundation.Image(picture, pageLabel, Modifier.fillMaxWidth())
                } else {
                    Thumbnail(pageLabel, null, size = ThumbnailSize.LG, kindLabel = stringResource(R.string.rc_pdf))
                }
            }
        }
    }
}

private const val MAX_PAGE_PX = 1440

/**
 * Compartilhar › Tutora: "Anexar comprovante" (AC-021-3). The files are already in Tutora; the
 * teacher picks the payment of the last 60 days they belong to, or records a new one with them.
 */
@Composable
fun AttachReceiptScreen(
    nav: NavHostController,
    files: List<String>,
    refused: Int,
) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val language = currentLanguage()
    val staged = remember(files) { files.mapNotNull(::decodeStaged) }
    val today = localOf(services.now(), services.zone).date
    val names by remember {
        combine(services.catalogRepo.students(), services.catalogRepo.groups(), services.catalogRepo.places()) { s, g, p ->
            Triple(
                s.associate { it.student.id to it.student.name },
                g.associate { it.id to it.name },
                p.associate { it.place.id to it.place },
            )
        }
    }.collectAsState(initial = null)
    val all by remember { services.paymentRepo.allPayments() }.collectAsState(initial = null)
    var choosingPayer by rememberSaveable { mutableStateOf(false) }
    var payer by remember { mutableStateOf<PayerRef?>(null) }
    val n = names ?: return
    val placeNames = n.third.mapValues { placeName(it.value) }

    fun nameOf(p: PayerRef) =
        when (p.kind) {
            PayerKind.STUDENT -> n.first[p.id]
            PayerKind.GROUP -> n.second[p.id]
            PayerKind.PLACE -> placeNames[p.id]
        }.orEmpty()
    val recent =
        attachable(
            all.orEmpty().map {
                val day = localOf(it.paidAt, services.zone).date
                AttachablePayment(it.id, it.payer, day, it.paidAt, it.method, it.cents, it.receipts)
            },
            today,
        )
    ScreenScaffold(
        stringResource(R.string.rc_attachTitle),
        "attach-receipt",
        variant = HeaderVariant.PUSH,
        onBack = { nav.popBackStack() },
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3_5),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            staged.firstOrNull()?.let { s ->
                ReceiptThumbnail(ReceiptTile(s.name, java.io.File(services.receipts.store.pending, s.name), s.mime), 0, staged.size)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    pluralStringResource(R.plurals.rc_received, staged.size.coerceAtLeast(1), staged.size),
                    style = TutoraTheme.type.bodyStrong,
                    color = TutoraTheme.colors.ink,
                )
                Text(stringResource(R.string.rc_which), style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
            }
        }
        if (refused > 0 || staged.isEmpty()) {
            AlertBanner(
                if (staged.isEmpty()) stringResource(R.string.rc_nothingShared) else stringResource(R.string.rc_refusedUnreadable),
                Modifier.testTag("attach-refused"),
                tone = AlertTone.ACCENT,
            )
        }
        if (staged.isNotEmpty()) {
            SectionHeader(stringResource(R.string.rc_last60))
            if (recent.isEmpty()) {
                EmptyState(stringResource(R.string.rc_noneRecent), icon = IconName.WALLET)
            } else {
                ListGroup(
                    recent.map { p ->
                        {
                            ListRow(
                                nameOf(p.payer),
                                subtitle =
                                    listOf(dayMonth(p.day, language), methodName(p.method), formatMoney(p.cents, language))
                                        .joinToString(" · "),
                                icon = IconName.WALLET,
                                onClick = {
                                    scope.launch {
                                        if (services.receipts.attach(p.id, staged)) {
                                            nav.navigate(PaymentDetail(p.id)) {
                                                popUpTo(app.tutora.ui.AttachReceipt::class) { inclusive = true }
                                            }
                                        }
                                    }
                                },
                                mark = if (p.receipts > 0) IconName.PAPERCLIP to stringResource(R.string.rc_withReceipt) else null,
                            )
                        }
                    },
                    Modifier.testTag("attach-list"),
                )
            }
            Button(
                stringResource(R.string.rc_newPayment),
                { choosingPayer = true },
                Modifier.testTag("attach-new"),
                variant = ButtonVariant.OUTLINE,
                fullWidth = true,
            )
        }
    }
    if (choosingPayer) {
        WhoPaidSheet(n.first, n.second, placeNames, { choosingPayer = false }) {
            choosingPayer = false
            payer = it
        }
    }
    payer?.let { p ->
        val ledger by remember(p) { services.paymentRepo.ledger(p) }.collectAsState(initial = null)
        ledger?.let { l ->
            PaymentSheet(p, l, onDismiss = {
                payer = null
                nav.popBackStack()
            }, shared = staged)
        }
    }
}

/** Registrar novo pagamento › "Quem pagou?": students, groups and places, searchable (AC-021-3). */
@Composable
private fun WhoPaidSheet(
    students: Map<String, String>,
    groups: Map<String, String>,
    places: Map<String, String>,
    onDismiss: () -> Unit,
    onPick: (PayerRef) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val kindStudent = stringResource(R.string.rc_kindStudent)
    val kindGroup = stringResource(R.string.rc_kindGroup)
    val kindPlace = stringResource(R.string.rc_kindPlace)
    val everyone =
        students.map { Triple(PayerRef(PayerKind.STUDENT, it.key), it.value, kindStudent) } +
            groups.map { Triple(PayerRef(PayerKind.GROUP, it.key), it.value, kindGroup) } +
            places.map { Triple(PayerRef(PayerKind.PLACE, it.key), it.value, kindPlace) }
    val shown =
        everyone.filter {
            app.tutora.usecases
                .foldName(it.second)
                .contains(app.tutora.usecases.foldName(query.trim()))
        }
    BottomSheet(
        stringResource(R.string.rc_newPayment),
        onDismiss,
        Modifier.testTag("who-paid"),
        subtitle = stringResource(R.string.rc_whoHint),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            TextField(stringResource(R.string.rc_whoPaid), query, { query = it }, placeholder = stringResource(R.string.rc_search))
            ListGroup(
                shown.sortedBy { app.tutora.usecases.foldName(it.second) }.map { (ref, name, kind) ->
                    { ListRow(name, subtitle = kind, onClick = { onPick(ref) }) }
                },
            )
        }
    }
}
