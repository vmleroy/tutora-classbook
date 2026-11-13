package app.tutora.ui.receipts

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.AddThumbnail
import app.tutora.designsystem.components.AlertBanner
import app.tutora.designsystem.components.AlertTone
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.ButtonVariant
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.RowTrailing
import app.tutora.designsystem.components.Thumbnail
import app.tutora.designsystem.components.ThumbnailSize
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.Receipt
import app.tutora.domain.ReceiptKind
import app.tutora.state.LocalServices
import app.tutora.usecases.ReceiptRef
import app.tutora.usecases.ReceiptRefusal
import app.tutora.usecases.StageResult
import app.tutora.usecases.StagedReceipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** The three ways a comprovante comes in (AC-021-1); each answers with the files' links, empty when cancelled. */
interface ReceiptPicker {
    fun camera(onPicked: (List<String>) -> Unit)

    fun gallery(onPicked: (List<String>) -> Unit)

    fun file(onPicked: (List<String>) -> Unit)
}

val LocalReceiptPicker = staticCompositionLocalOf<ReceiptPicker?> { null }

/** The system camera (no permission: the camera app takes the photo), the photo picker, and the file picker. */
@Composable
fun rememberReceiptPicker(): ReceiptPicker {
    LocalReceiptPicker.current?.let { return it }
    val context = LocalContext.current
    var pending by remember { mutableStateOf<((List<String>) -> Unit)?>(null) }
    var shot by rememberSaveable { mutableStateOf<String?>(null) }
    val camera =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
            val uri = shot
            pending?.invoke(if (ok && uri != null) listOf(uri) else emptyList())
            pending = null
        }
    val gallery =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            pending?.invoke(uris.map(Uri::toString))
            pending = null
        }
    val files =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            pending?.invoke(uris.map(Uri::toString))
            pending = null
        }
    return remember(context) {
        object : ReceiptPicker {
            override fun camera(onPicked: (List<String>) -> Unit) {
                // The camera app writes into Tutora's cache through the file provider (spec 021).
                val dir = File(context.cacheDir, "incoming").apply { mkdirs() }
                val file = File(dir, "foto-${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                shot = uri.toString()
                pending = onPicked
                camera.launch(uri)
            }

            override fun gallery(onPicked: (List<String>) -> Unit) {
                pending = onPicked
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }

            override fun file(onPicked: (List<String>) -> Unit) {
                pending = onPicked
                files.launch(arrayOf("image/*", "application/pdf"))
            }
        }
    }
}

/** Pictures of kept comprovantes, for thumbnails and the viewer; drawn on the phone, nothing in tests. */
interface ReceiptImages {
    /** A picture of [file] about [px] wide: an image sampled down, or a PDF's first page. */
    fun thumbnail(
        file: File,
        px: Int,
    ): Painter?

    /** How many pages a PDF has; 1 for an image. */
    fun pages(file: File): Int

    /** One page of a PDF, or the image, about [px] wide. */
    fun page(
        file: File,
        index: Int,
        px: Int,
    ): Painter?

    object None : ReceiptImages {
        override fun thumbnail(
            file: File,
            px: Int,
        ): Painter? = null

        override fun pages(file: File) = 1

        override fun page(
            file: File,
            index: Int,
            px: Int,
        ): Painter? = null
    }
}

val LocalReceiptImages = staticCompositionLocalOf<ReceiptImages> { ReceiptImages.None }

/** A picture loaded off the main thread; null until it is ready, or for good in tests. */
@Composable
fun rememberReceiptPicture(
    file: File,
    page: Int = 0,
    px: Int = THUMB_PX,
    thumbnail: Boolean = true,
): Painter? {
    val images = LocalReceiptImages.current
    val picture by produceState<Painter?>(null, file, page, px) {
        value =
            withContext(Dispatchers.IO) {
                runCatching { if (thumbnail) images.thumbnail(file, px) else images.page(file, page, px) }.getOrNull()
            }
    }
    return picture
}

/** A comprovante shown in a form: its file and type, for the thumbnail. */
data class ReceiptTile(
    val key: String,
    val file: File,
    val mime: String,
)

/** Why a file was not taken, in words (AC-021-2). */
@Composable
fun refusalText(reason: ReceiptRefusal): String =
    stringResource(
        when (reason) {
            ReceiptRefusal.UNSUPPORTED -> R.string.rc_refusedUnsupported
            ReceiptRefusal.TOO_LARGE -> R.string.rc_refusedTooLarge
            ReceiptRefusal.UNREADABLE -> R.string.rc_refusedUnreadable
        },
    )

/**
 * The Comprovante part of Registrar / Editar pagamento (AC-021-1): a thumbnail for each with its
 * ×, then Adicionar, which opens the three sources. As many as the teacher wants.
 */
@Composable
fun ReceiptsField(
    tiles: List<ReceiptTile>,
    onRemove: (String) -> Unit,
    onPicked: (List<String>) -> Unit,
    refusal: ReceiptRefusal? = null,
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.testTag("receipts-field"), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Text(
            stringResource(R.string.rc_receipt),
            Modifier.padding(start = Spacing.s1),
            style = TutoraTheme.type.fieldLabel,
            color = TutoraTheme.colors.inkMuted,
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(top = Spacing.s1_5, end = Spacing.s1_5),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            tiles.forEachIndexed { i, t ->
                ReceiptThumbnail(t, i, tiles.size, onRemove = { onRemove(t.key) })
            }
            AddThumbnail(stringResource(R.string.rc_add), { adding = true })
        }
        if (refusal != null) {
            AlertBanner(refusalText(refusal), Modifier.testTag("receipt-refused"), tone = AlertTone.ACCENT, icon = IconName.ALERT)
        }
    }
    if (adding) {
        AddReceiptSheet({ adding = false }) { uris ->
            adding = false
            if (uris.isNotEmpty()) onPicked(uris)
        }
    }
}

/** One comprovante's thumbnail, read as "Comprovante 2 de 3". */
@Composable
fun ReceiptThumbnail(
    tile: ReceiptTile,
    index: Int,
    count: Int,
    size: ThumbnailSize = ThumbnailSize.MD,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    val picture = rememberReceiptPicture(tile.file, px = if (size == ThumbnailSize.LG) THUMB_PX * 4 / 3 else THUMB_PX)
    Thumbnail(
        stringResource(R.string.rc_item, index + 1, count),
        picture,
        size = size,
        kindLabel = if (ReceiptKind.ofMime(tile.mime) == ReceiptKind.PDF) stringResource(R.string.rc_pdf) else null,
        onClick = onClick,
        onRemove = onRemove,
    )
}

/** Adicionar comprovante: Tirar foto, Escolher da galeria, Escolher arquivo (AC-021-1). */
@Composable
fun AddReceiptSheet(
    onDismiss: () -> Unit,
    onPicked: (List<String>) -> Unit,
) {
    val picker = rememberReceiptPicker()
    BottomSheet(
        stringResource(R.string.rc_addTitle),
        onDismiss,
        actions = listOf { m -> Button(stringResource(R.string.rc_cancel), onDismiss, m, variant = ButtonVariant.OUTLINE) },
    ) {
        ListGroup(
            listOf(
                {
                    ListRow(
                        stringResource(R.string.rc_camera),
                        subtitle = stringResource(R.string.rc_cameraSub),
                        icon = IconName.CAMERA,
                        trailing = RowTrailing.NONE,
                        onClick = { picker.camera(onPicked) },
                    )
                },
                {
                    ListRow(
                        stringResource(R.string.rc_gallery),
                        subtitle = stringResource(R.string.rc_gallerySub),
                        icon = IconName.IMAGE,
                        trailing = RowTrailing.NONE,
                        onClick = { picker.gallery(onPicked) },
                    )
                },
                {
                    ListRow(
                        stringResource(R.string.rc_file),
                        subtitle = stringResource(R.string.rc_fileSub),
                        icon = IconName.FILE,
                        trailing = RowTrailing.NONE,
                        onClick = { picker.file(onPicked) },
                    )
                },
            ),
        )
    }
}

/**
 * A comprovante in a form's saved state: an existing one by id, or a staged file as
 * "name|mime|bytes" (spec 021), so turning the phone or the lock keeps what was picked.
 */
fun encodeStaged(s: StagedReceipt) = "n:${s.name}|${s.mime}|${s.bytes}"

fun decodeStaged(key: String): StagedReceipt? =
    key
        .removePrefix("n:")
        .takeIf { key.startsWith("n:") }
        ?.split('|')
        ?.takeIf { it.size == 3 }
        ?.let { StagedReceipt(it[0], it[1], it[2].toLongOrNull() ?: 0) }

const val THUMB_PX = 216

/** The comprovantes a form will save, from its keys: "e:<id>" kept, "n:…" just picked. */
fun receiptRefs(keys: List<String>): List<ReceiptRef> =
    keys.mapNotNull { k ->
        when {
            k.startsWith("e:") -> ReceiptRef.Existing(k.removePrefix("e:"))
            else -> decodeStaged(k)?.let { ReceiptRef.New(it) }
        }
    }

/**
 * The Comprovante part of a payment form, with its state: [keys] are what will be saved, null
 * until the payment's own comprovantes have loaded. Picked files are copied into Tutora at once
 * (AC-021-2); one that can't be a comprovante is explained and left out.
 */
@Composable
fun ReceiptsPart(
    keys: List<String>,
    onKeys: (List<String>) -> Unit,
    existing: List<Receipt>,
) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    var refusal by remember { mutableStateOf<ReceiptRefusal?>(null) }
    val byId = existing.associateBy { it.id }
    val tiles =
        keys.mapNotNull { k ->
            if (k.startsWith("e:")) {
                byId[k.removePrefix("e:")]?.let { ReceiptTile(k, services.receipts.store.file(it.file), it.mime) }
            } else {
                decodeStaged(k)?.let { ReceiptTile(k, File(services.receipts.store.pending, it.name), it.mime) }
            }
        }
    ReceiptsField(tiles, { key -> onKeys(keys - key) }, { uris ->
        scope.launch {
            val results = withContext(Dispatchers.IO) { uris.map { services.receipts.stage(it) } }
            refusal = results.filterIsInstance<StageResult.Refused>().firstOrNull()?.reason
            onKeys(keys + results.filterIsInstance<StageResult.Ok>().map { encodeStaged(it.staged) })
        }
    }, refusal)
}

/** The files a Compartilhar › Tutora intent carries: one, or several. */
fun sharedUris(intent: android.content.Intent?): List<String> {
    if (intent == null) return emptyList()
    return when (intent.action) {
        android.content.Intent.ACTION_SEND ->
            listOfNotNull(
                androidx.core.content.IntentCompat
                    .getParcelableExtra(intent, android.content.Intent.EXTRA_STREAM, Uri::class.java)
                    ?.toString(),
            )
        android.content.Intent.ACTION_SEND_MULTIPLE ->
            androidx.core.content.IntentCompat
                .getParcelableArrayListExtra(intent, android.content.Intent.EXTRA_STREAM, Uri::class.java)
                .orEmpty()
                .map(Uri::toString)
        else -> emptyList()
    }
}

/** Anexar comprovante for what was copied in, and how many files could not be used (AC-021-3). */
fun attachRoute(results: List<StageResult>) =
    app.tutora.ui.AttachReceipt(
        results.filterIsInstance<StageResult.Ok>().map { encodeStaged(it.staged) },
        results.count { it is StageResult.Refused },
    )
