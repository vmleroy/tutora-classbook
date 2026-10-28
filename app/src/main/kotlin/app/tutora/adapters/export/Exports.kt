package app.tutora.adapters.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import app.tutora.domain.paginate
import java.io.ByteArrayOutputStream
import java.io.File

// Turning a report into a PDF, and handing files and text to the phone (spec 010). Screens reach
// these through LocalExports; tests provide a fake that keeps what was shared.

/** A row of a PDF table; its last cell is the situation, drawn in [tone]. */
data class PdfRow(
    val cells: List<String>,
    val tone: PdfTone = PdfTone.INK,
)

enum class PdfTone { INK, PAID, OWED, MUTED }

/** A titled block of lines under the table: payments received, a list of classes, the trend. */
data class PdfSection(
    val title: String,
    val lines: List<Pair<String, String>>,
)

/** Everything a report PDF says, in order (AC-010-9, 14). */
data class PdfPlan(
    val brand: String,
    val title: String,
    val subtitle: String,
    val subjectLabel: String?,
    val subjectName: String?,
    val subjectLine: String?,
    val stats: List<Pair<String, String>>,
    val tableTitle: String,
    val columns: List<String>,
    val rows: List<PdfRow>,
    val totalRow: List<String>?,
    val sections: List<PdfSection>,
    val notes: List<String>,
    /** "Gerado pela Tutora em 26/10/2026, 18:45 · página %1$d de %2$d". */
    val footer: String,
)

/** Colors from the active theme, as ARGB ints. */
data class PdfColors(
    val primary: Int,
    val onPrimary: Int,
    val ink: Int,
    val muted: Int,
    val paid: Int,
    val owed: Int,
    val line: Int,
)

/** One page: which table rows, and whether it carries the summary (first) or the totals and sections (last). */
data class PdfPage(
    val rows: IntRange,
    val first: Boolean,
    val last: Boolean,
    val footer: String,
)

// Rows that fit under the summary on page one, and on a page with only the table header.
const val PDF_FIRST_ROWS = 18
const val PDF_OTHER_ROWS = 34

/** Pages for a plan: the table spread over pages, with the header repeated; the footer numbers each (AC-010-9). */
fun pdfPages(plan: PdfPlan): List<PdfPage> {
    val ranges = paginate(plan.rows.size, PDF_FIRST_ROWS, PDF_OTHER_ROWS)
    return ranges.mapIndexed { i, r ->
        PdfPage(r, first = i == 0, last = i == ranges.lastIndex, footer = plan.footer.format(i + 1, ranges.size))
    }
}

interface Exports {
    fun pdf(
        plan: PdfPlan,
        colors: PdfColors,
    ): ByteArray

    /** Opens the share sheet with the file (AC-010-11). */
    fun share(
        fileName: String,
        mime: String,
        bytes: ByteArray,
    )

    /** Copiar como texto (AC-010-12). */
    fun copy(text: String)

    object None : Exports {
        override fun pdf(
            plan: PdfPlan,
            colors: PdfColors,
        ) = ByteArray(0)

        override fun share(
            fileName: String,
            mime: String,
            bytes: ByteArray,
        ) = Unit

        override fun copy(text: String) = Unit
    }
}

val LocalExports = staticCompositionLocalOf<Exports> { Exports.None }

const val MIME_PDF = "application/pdf"
const val MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

class AndroidExports(
    private val context: Context,
) : Exports {
    override fun pdf(
        plan: PdfPlan,
        colors: PdfColors,
    ): ByteArray = AndroidPdf(context, colors).draw(plan)

    override fun share(
        fileName: String,
        mime: String,
        bytes: ByteArray,
    ) {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, fileName.replace('/', '-'))
        file.writeBytes(bytes)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        context.startActivity(Intent.createChooser(send, fileName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun copy(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Tutora", text))
    }
}

/** Draws a plan on A4 portrait pages (595 × 842 points). */
private class AndroidPdf(
    private val context: Context,
    private val c: PdfColors,
) {
    private val width = 595
    private val height = 842
    private val margin = 40f
    private val font: Typeface =
        runCatching { ResourcesCompat.getFont(context, app.tutora.designsystem.R.font.nunito) }.getOrNull() ?: Typeface.SANS_SERIF

    private fun paint(
        size: Float,
        color: Int,
        bold: Boolean = false,
    ) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = Typeface.create(font, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    /** Text cut to [width] with an ellipsis, so long names never run into the next column. */
    private fun fit(
        text: String,
        p: TextPaint,
        width: Float,
    ) = TextUtils.ellipsize(text, p, width, TextUtils.TruncateAt.END).toString()

    fun draw(plan: PdfPlan): ByteArray {
        val doc = PdfDocument()
        val pages = pdfPages(plan)
        pages.forEachIndexed { i, pg ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(width, height, i + 1).create())
            val canvas = page.canvas
            var y: Float
            if (pg.first) {
                canvas.drawRect(0f, 0f, width.toFloat(), 96f, Paint().apply { color = c.primary })
                canvas.drawText(plan.brand, margin, 30f, paint(9f, c.onPrimary, bold = true))
                canvas.drawText(plan.title, margin, 58f, paint(20f, c.onPrimary, bold = true))
                canvas.drawText(fit(plan.subtitle, paint(10f, c.onPrimary), width - 2 * margin), margin, 80f, paint(10f, c.onPrimary))
                y = 124f
                plan.subjectLabel?.let { canvas.drawText(it, margin, y, paint(9f, c.muted)) }
                plan.subjectName?.let {
                    y += 16f
                    canvas.drawText(it, margin, y, paint(14f, c.ink, bold = true))
                }
                plan.subjectLine?.let {
                    y += 14f
                    canvas.drawText(fit(it, paint(10f, c.muted), width - 2 * margin), margin, y, paint(10f, c.muted))
                }
                y += 28f
                val colW = (width - 2 * margin) / plan.stats.size.coerceAtLeast(1)
                plan.stats.forEachIndexed { k, (label, value) ->
                    canvas.drawText(label, margin + k * colW, y, paint(9f, c.muted))
                    canvas.drawText(value, margin + k * colW, y + 18f, paint(13f, c.ink, bold = true))
                }
                y += 48f
                canvas.drawText(plan.tableTitle, margin, y, paint(12f, c.ink, bold = true))
                y += 18f
            } else {
                canvas.drawText(plan.title, margin, 40f, paint(12f, c.primary, bold = true))
                y = 64f
            }
            // The table header repeats on every page.
            val colW = (width - 2 * margin) / plan.columns.size.coerceAtLeast(1)
            plan.columns.forEachIndexed { k, h -> canvas.drawText(h, margin + k * colW, y, paint(9f, c.muted, bold = true)) }
            y += 6f
            canvas.drawLine(margin, y, width - margin, y, Paint().apply { color = c.line })
            y += 16f
            for (r in pg.rows) {
                val row = plan.rows[r]
                row.cells.forEachIndexed { k, cell ->
                    val tone =
                        if (k == row.cells.lastIndex) {
                            when (row.tone) {
                                PdfTone.PAID -> c.paid
                                PdfTone.OWED -> c.owed
                                PdfTone.MUTED -> c.muted
                                PdfTone.INK -> c.ink
                            }
                        } else {
                            c.ink
                        }
                    val p = paint(10f, tone)
                    canvas.drawText(fit(cell, p, colW - 6f), margin + k * colW, y, p)
                }
                y += 18f
            }
            if (pg.last) {
                plan.totalRow?.let { total ->
                    canvas.drawLine(margin, y - 10f, width - margin, y - 10f, Paint().apply { color = c.line })
                    total.forEachIndexed { k, cell -> canvas.drawText(cell, margin + k * colW, y + 4f, paint(10f, c.ink, bold = true)) }
                    y += 28f
                }
                for (s in plan.sections) {
                    if (y > height - 120f) break
                    canvas.drawText(s.title, margin, y, paint(12f, c.ink, bold = true))
                    y += 18f
                    for ((text, value) in s.lines) {
                        if (y > height - 90f) break
                        canvas.drawText(fit(text, paint(10f, c.ink), width - 2 * margin - 110f), margin, y, paint(10f, c.ink))
                        if (value.isNotEmpty()) {
                            val p = paint(10f, c.ink, bold = true)
                            canvas.drawText(value, width - margin - p.measureText(value), y, p)
                        }
                        y += 16f
                    }
                    y += 10f
                }
                for (n in plan.notes) {
                    if (y > height - 70f) break
                    canvas.drawText(fit(n, paint(10f, c.ink, bold = true), width - 2 * margin), margin, y, paint(10f, c.ink, bold = true))
                    y += 16f
                }
            }
            canvas.drawText(pg.footer, margin, height - 28f, paint(8f, c.muted))
            doc.finishPage(page)
        }
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }
}
