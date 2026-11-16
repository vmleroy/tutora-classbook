// minSdk is 26: the platform ExifInterface's known bugs are on older versions, so no extra library.
@file:SuppressLint("ExifInterface")

package app.tutora.adapters.receipts

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import app.tutora.domain.ReceiptIntake
import app.tutora.domain.ReceiptKind
import app.tutora.domain.receiptPolicy
import app.tutora.usecases.ReceiptFiles
import app.tutora.usecases.ReceiptRefusal
import app.tutora.usecases.StageResult
import app.tutora.usecases.StagedReceipt
import app.tutora.usecases.receiptExtension
import java.io.File

/**
 * Reads a picked or shared comprovante (a content link, or a file the camera wrote) into Tutora's
 * pending folder. A PDF is checked to open; a photo larger than 2000 px, or one turned by its
 * EXIF tag, is decoded, scaled, turned upright and saved as JPEG (AC-021-2).
 */
class AndroidReceiptFiles(
    private val context: Context,
) : ReceiptFiles {
    override fun stage(
        uri: String,
        dir: File,
        name: String,
    ): StageResult {
        val source = uri.toUri()
        val mime = mimeOf(source) ?: return StageResult.Refused(ReceiptRefusal.UNSUPPORTED)
        val kind = ReceiptKind.ofMime(mime) ?: return StageResult.Refused(ReceiptRefusal.UNSUPPORTED)
        dir.mkdirs()
        val tmp = File(dir, "$name.tmp")
        val copied =
            runCatching {
                checkNotNull(context.contentResolver.openInputStream(source)).use { input -> tmp.outputStream().use { input.copyTo(it) } }
            }
        if (copied.isFailure) {
            tmp.delete()
            return StageResult.Refused(ReceiptRefusal.UNREADABLE)
        }
        val result = if (kind == ReceiptKind.PDF) pdf(tmp, dir, name) else image(tmp, dir, name, mime)
        tmp.delete()
        return result
    }

    private fun pdf(
        tmp: File,
        dir: File,
        name: String,
    ): StageResult {
        if (receiptPolicy(PDF, tmp.length()) == ReceiptIntake.TooLarge) return StageResult.Refused(ReceiptRefusal.TOO_LARGE)
        // A PDF with a password or broken can't be shown or sent later: refuse it now.
        val opens =
            runCatching {
                ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                    PdfRenderer(fd).use { it.pageCount > 0 }
                }
            }.getOrDefault(false)
        if (!opens) return StageResult.Refused(ReceiptRefusal.UNREADABLE)
        val out = File(dir, "$name.pdf")
        tmp.copyTo(out, overwrite = true)
        return StageResult.Ok(StagedReceipt(out.name, PDF, out.length()))
    }

    private fun image(
        tmp: File,
        dir: File,
        name: String,
        mime: String,
    ): StageResult {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(tmp.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return StageResult.Refused(ReceiptRefusal.UNREADABLE)
        val rotation = runCatching { rotationOf(ExifInterface(tmp.path)) }.getOrDefault(0)
        val intake = receiptPolicy(mime, tmp.length(), bounds.outWidth, bounds.outHeight)
        if (intake == ReceiptIntake.Keep && rotation == 0) {
            val out = File(dir, "$name.${receiptExtension(mime)}")
            tmp.copyTo(out, overwrite = true)
            return StageResult.Ok(StagedReceipt(out.name, mime, out.length()))
        }
        val (w, h) = (intake as? ReceiptIntake.Shrink)?.let { it.width to it.height } ?: (bounds.outWidth to bounds.outHeight)
        // Decode no larger than needed, then scale exactly and turn it upright.
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= w && bounds.outHeight / (sample * 2) >= h) sample *= 2
        val decoded =
            BitmapFactory.decodeFile(tmp.path, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: return StageResult.Refused(ReceiptRefusal.UNREADABLE)
        val matrix =
            Matrix().apply {
                postScale(w.toFloat() / decoded.width, h.toFloat() / decoded.height)
                if (rotation != 0) postRotate(rotation.toFloat())
            }
        val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        val out = File(dir, "$name.jpg")
        out.outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        if (upright !== decoded) upright.recycle()
        decoded.recycle()
        return StageResult.Ok(StagedReceipt(out.name, JPEG, out.length()))
    }

    private fun mimeOf(uri: Uri): String? =
        context.contentResolver.getType(uri)
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()).lowercase())

    private fun rotationOf(exif: ExifInterface): Int =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    private companion object {
        const val PDF = "application/pdf"
        const val JPEG = "image/jpeg"
        const val JPEG_QUALITY = 90
    }
}
