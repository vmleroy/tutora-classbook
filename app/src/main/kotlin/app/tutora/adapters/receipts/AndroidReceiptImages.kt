package app.tutora.adapters.receipts

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.core.graphics.createBitmap
import app.tutora.ui.receipts.ReceiptImages
import java.io.File

/** Pictures of kept comprovantes: an image sampled down to the size asked, a PDF page drawn by the phone (AC-021-4, 5). */
class AndroidReceiptImages : ReceiptImages {
    override fun thumbnail(
        file: File,
        px: Int,
    ): Painter? = page(file, 0, px)

    override fun pages(file: File): Int =
        if (!file.isPdf()) {
            1
        } else {
            runCatching { renderer(file) { it.pageCount } }.getOrDefault(1)
        }

    override fun page(
        file: File,
        index: Int,
        px: Int,
    ): Painter? = (if (file.isPdf()) pdfPage(file, index, px) else image(file, px))?.let { BitmapPainter(it.asImageBitmap()) }

    /** A PDF page as a bitmap [px] wide, on white, for the viewer, a thumbnail or the sent document. */
    fun pdfPage(
        file: File,
        index: Int,
        px: Int,
    ): Bitmap? =
        runCatching {
            renderer(file) { r ->
                r.openPage(index.coerceIn(0, r.pageCount - 1)).use { page ->
                    val h = (px.toLong() * page.height / page.width).toInt().coerceAtLeast(1)
                    createBitmap(px, h).also { bmp ->
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
            }
        }.getOrNull()

    /** An image decoded no larger than about [px] wide. */
    fun image(
        file: File,
        px: Int,
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= px) sample *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun <T> renderer(
        file: File,
        use: (PdfRenderer) -> T,
    ): T = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd -> PdfRenderer(fd).use(use) }

    private fun File.isPdf() = extension.equals("pdf", ignoreCase = true)
}
