package app.tutora.adapters.export

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.FileProvider
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

/** A titled block of lines under the table: payments received, a list of classes. */
data class PdfSection(
    val title: String,
    val lines: List<Pair<String, String>>,
)

/** One of the four tiles under the header; PAID and OWED tiles are tinted. */
data class PdfStat(
    val label: String,
    val value: String,
    val tone: PdfTone = PdfTone.INK,
)

/** A month of the earnings PDF's six-month bars. */
data class PdfBar(
    val label: String,
    val value: Long,
    val valueLabel: String,
)

/** Everything a report PDF says, in order (AC-010-9, 14). */
data class PdfPlan(
    val brand: String,
    val title: String,
    val subtitle: String,
    val subjectLabel: String?,
    val subjectName: String?,
    val subjectLine: String?,
    val stats: List<PdfStat>,
    val tableTitle: String,
    val columns: List<String>,
    /** Which columns are numbers or a situation, drawn right-aligned. */
    val rightAligned: Set<Int>,
    val rows: List<PdfRow>,
    val totalRow: List<String>?,
    val sections: List<PdfSection>,
    val trendTitle: String? = null,
    val trend: List<PdfBar> = emptyList(),
    /** "Valor pendente: R$ 240,00" and the days still owed, in the dashed box at the foot. */
    val pending: Pair<String, String>? = null,
    val pix: String? = null,
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
    /** The cream of the tiles and payment rows, the tinted tiles, and the bars of earlier months. */
    val ground: Int,
    val paidSoft: Int,
    val owedSoft: Int,
    val primarySoft: Int,
)

/** One page: which table rows, and whether it carries the summary (first) or the totals and sections (last). */
data class PdfPage(
    val rows: IntRange,
    val first: Boolean,
    val last: Boolean,
    val footer: String,
)

// Rows that fit under the summary on page one, and on a page with only the table header.
const val PDF_FIRST_ROWS = 16
const val PDF_OTHER_ROWS = 26

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

/**
 * Draws a plan on A4 portrait pages (595 × 842 points), after the ReportPdf and EarningsPdf
 * mockups: a white header with a rule in the theme's color, four tiles, a table whose header row is
 * filled, payments on cream, and the pending amount with the Pix key in a dashed box at the foot.
 */
private class AndroidPdf(
    private val context: Context,
    private val c: PdfColors,
) {
    private val width = 595f
    private val height = 842f
    private val left = 44f
    private val right = width - 44f
    private val top = 40f
    private val rowH = 26f

    // Nunito is a variable font: unless a weight is asked for, a PDF draws its lightest. So it is
    // copied out once and opened at 400 (text) and 800 (bold), the weights of the mockups.
    // A font resource is a plain .ttf file; reading its bytes as a raw resource is what we want.
    @SuppressLint("ResourceType")
    private val fontFile: File? =
        runCatching {
            File(context.cacheDir, "nunito.ttf").also { f ->
                if (!f.exists()) {
                    context.resources.openRawResource(app.tutora.designsystem.R.font.nunito).use { input ->
                        f.outputStream().use { input.copyTo(it) }
                    }
                }
            }
        }.getOrNull()

    private fun face(weight: Int): Typeface =
        fontFile?.let { runCatching { Typeface.Builder(it).setFontVariationSettings("'wght' $weight").build() }.getOrNull() }
            ?: Typeface.create(Typeface.SANS_SERIF, if (weight >= 700) Typeface.BOLD else Typeface.NORMAL)

    private val regular = face(400)
    private val heavy = face(800)

    private fun paint(
        size: Float,
        color: Int,
        bold: Boolean = false,
    ) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) heavy else regular
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    /** Text cut to [width] with an ellipsis, so long names never run into the next column. */
    private fun fit(
        text: String,
        p: TextPaint,
        width: Float,
    ) = TextUtils.ellipsize(text, p, width, TextUtils.TruncateAt.END).toString()

    private fun Canvas.text(
        s: String,
        x: Float,
        y: Float,
        p: TextPaint,
        alignRight: Boolean = false,
        maxWidth: Float = right - x,
    ) {
        val t = fit(s, p, maxWidth)
        drawText(t, if (alignRight) x - p.measureText(t) else x, y, p)
    }

    /** Column edges for the table: the first is widest, as in the mockup. */
    private fun columns(n: Int): List<Pair<Float, Float>> {
        val weights = List(n) { if (it == 0 || (n == 5 && it == 2)) 1.4f else 1f }
        val unit = (right - left - 20f) / weights.sum()
        var x = left + 10f
        return weights.map { w -> (x to x + w * unit).also { x += w * unit } }
    }

    private fun Canvas.row(
        cells: List<String>,
        y: Float,
        plan: PdfPlan,
        cols: List<Pair<Float, Float>>,
        p: (Int) -> TextPaint,
    ) {
        cells.forEachIndexed { k, cell ->
            val (from, to) = cols[k]
            if (k in
                plan.rightAligned
            ) {
                text(cell, to, y, p(k), alignRight = true, maxWidth = to - from - 6f)
            } else {
                text(
                    cell,
                    from,
                    y,
                    p(k),
                    maxWidth =
                        to - from - 6f,
                )
            }
        }
    }

    fun draw(plan: PdfPlan): ByteArray {
        val doc = PdfDocument()
        val pages = pdfPages(plan)
        val cols = columns(plan.columns.size)
        pages.forEachIndexed { i, pg ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(width.toInt(), height.toInt(), i + 1).create())
            val canvas = page.canvas
            var y = top
            if (pg.first) {
                // Header: brand, title and period on the left; who it is for on the right; then the rule.
                val brand = paint(9f, c.primary, bold = true).apply { letterSpacing = 0.12f }
                canvas.text(plan.brand, left, y + 9f, brand)
                canvas.text(plan.title, left, y + 34f, paint(22f, c.ink, bold = true), maxWidth = 300f)
                canvas.text(plan.subtitle, left, y + 52f, paint(10f, c.muted), maxWidth = 300f)
                plan.subjectLabel?.let { canvas.text(it, right, y + 9f, paint(9f, c.muted), alignRight = true, maxWidth = 200f) }
                plan.subjectName?.let {
                    canvas.text(
                        it,
                        right,
                        y + 28f,
                        paint(13f, c.ink, bold = true),
                        alignRight = true,
                        maxWidth = 200f,
                    )
                }
                plan.subjectLine?.let { canvas.text(it, right, y + 44f, paint(9f, c.muted), alignRight = true, maxWidth = 200f) }
                y += 64f
                canvas.drawRect(left, y, right, y + 2.5f, fill(c.primary))
                y += 18f
                // Four tiles: neutral on cream, Pago on green, A receber on red.
                val gap = 8f
                val tileW = (right - left - gap * (plan.stats.size - 1)) / plan.stats.size.coerceAtLeast(1)
                plan.stats.forEachIndexed { k, st ->
                    val x = left + k * (tileW + gap)
                    val (bg, fg) =
                        when (st.tone) {
                            PdfTone.PAID -> c.paidSoft to c.paid
                            PdfTone.OWED -> c.owedSoft to c.owed
                            else -> c.ground to c.ink
                        }
                    canvas.drawRoundRect(RectF(x, y, x + tileW, y + 44f), 8f, 8f, fill(bg))
                    canvas.text(
                        st.label,
                        x + 10f,
                        y + 15f,
                        paint(
                            8.5f,
                            if (st.tone ==
                                PdfTone.INK
                            ) {
                                c.muted
                            } else {
                                fg
                            },
                            bold = true,
                        ),
                        maxWidth = tileW - 20f,
                    )
                    canvas.text(st.value, x + 10f, y + 34f, paint(13f, fg, bold = true), maxWidth = tileW - 20f)
                }
                y += 62f
                canvas.text(plan.tableTitle, left, y, paint(11f, c.ink, bold = true))
                y += 8f
            }
            // The table's header row, filled, repeated on every page.
            if (plan.columns.isNotEmpty() && (pg.rows.count() > 0 || pg.first)) {
                val head =
                    Path().apply {
                        addRoundRect(
                            RectF(left, y, right, y + 22f),
                            floatArrayOf(6f, 6f, 6f, 6f, 0f, 0f, 0f, 0f),
                            Path.Direction.CW,
                        )
                    }
                canvas.drawPath(head, fill(c.primary))
                canvas.row(plan.columns, y + 15f, plan, cols) { paint(9f, c.onPrimary, bold = true) }
                y += 22f
            }
            for (r in pg.rows) {
                val row = plan.rows[r]
                canvas.row(row.cells, y + 17f, plan, cols) { k ->
                    when {
                        k == row.cells.lastIndex && row.tone == PdfTone.PAID -> paint(9.5f, c.paid, bold = true)
                        k == row.cells.lastIndex && row.tone == PdfTone.OWED -> paint(9.5f, c.owed, bold = true)
                        k == row.cells.lastIndex && row.tone == PdfTone.MUTED -> paint(9.5f, c.muted)
                        k == 0 -> paint(9.5f, c.ink, bold = true)
                        else -> paint(9.5f, c.ink)
                    }
                }
                y += rowH
                canvas.drawLine(left, y, right, y, Paint().apply { color = c.line })
            }
            if (pg.last) {
                plan.totalRow?.let { total ->
                    canvas.row(total, y + 17f, plan, cols) { paint(9.5f, c.ink, bold = true) }
                    y += rowH + 10f
                }
                if (plan.trend.isNotEmpty() && plan.trendTitle != null) {
                    y += 6f
                    canvas.text(plan.trendTitle, left, y, paint(11f, c.ink, bold = true))
                    y += 14f
                    val max = plan.trend.maxOf { it.value }.coerceAtLeast(1)
                    val barGap = 12f
                    val barW = (right - left - barGap * (plan.trend.size - 1)) / plan.trend.size
                    val chart = 70f
                    plan.trend.forEachIndexed { k, b ->
                        val x = left + k * (barW + barGap)
                        val h = chart * b.value / max
                        canvas.text(
                            b.valueLabel,
                            x + barW / 2 - paint(8f, c.muted).measureText(b.valueLabel) / 2,
                            y + chart - h + 8f,
                            paint(8f, c.muted),
                        )
                        canvas.drawRoundRect(
                            RectF(x, y + chart - h + 12f, x + barW, y + chart + 12f),
                            4f,
                            4f,
                            fill(
                                if (k ==
                                    plan.trend.lastIndex
                                ) {
                                    c.primary
                                } else {
                                    c.primarySoft
                                },
                            ),
                        )
                        val l = paint(8.5f, c.muted, bold = true)
                        canvas.text(b.label, x + barW / 2 - l.measureText(b.label) / 2, y + chart + 26f, l)
                    }
                    y += chart + 40f
                }
                for (s in plan.sections) {
                    if (y > height - 150f) break
                    canvas.text(s.title, left, y + 4f, paint(11f, c.ink, bold = true))
                    y += 12f
                    for ((text, value) in s.lines) {
                        if (y > height - 140f) break
                        canvas.drawRoundRect(RectF(left, y, right, y + 22f), 6f, 6f, fill(c.ground))
                        canvas.text(text, left + 10f, y + 15f, paint(9.5f, c.ink), maxWidth = right - left - 110f)
                        if (value.isNotEmpty()) {
                            canvas.text(
                                value,
                                right - 10f,
                                y + 15f,
                                paint(9.5f, c.ink, bold = true),
                                alignRight = true,
                                maxWidth = 120f,
                            )
                        }
                        y += 26f
                    }
                    y += 8f
                }
                // The pending amount and the Pix key, in a dashed box above the foot.
                if (plan.pending != null || plan.pix != null) {
                    val boxTop = height - 110f
                    val dash =
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = c.line
                            style = Paint.Style.STROKE
                            strokeWidth = 1.5f
                            pathEffect = DashPathEffect(floatArrayOf(5f, 4f), 0f)
                        }
                    canvas.drawRoundRect(RectF(left, boxTop, right, boxTop + 46f), 10f, 10f, dash)
                    plan.pending?.let { (title, detail) ->
                        canvas.text(title, left + 12f, boxTop + 19f, paint(10.5f, c.ink, bold = true), maxWidth = 280f)
                        canvas.text(detail, left + 12f, boxTop + 34f, paint(9f, c.muted), maxWidth = 280f)
                    }
                    plan.pix?.let {
                        canvas.text(
                            it,
                            right - 12f,
                            boxTop + 27f,
                            paint(9.5f, c.ink, bold = plan.pending == null),
                            alignRight = true,
                            maxWidth = 220f,
                        )
                    }
                }
            }
            val foot = paint(8f, c.muted)
            canvas.text(pg.footer, width / 2 - foot.measureText(pg.footer).coerceAtMost(right - left) / 2, height - 30f, foot)
            doc.finishPage(page)
        }
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }
}
