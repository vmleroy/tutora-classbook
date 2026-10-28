package app.tutora.adapters.export

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// A minimal .xlsx writer (spec 010): the smallest valid OOXML package, no library. Numbers are
// numbers and dates are dates, so a spreadsheet can sum and sort them.

/** One cell. */
sealed interface Cell {
    data class Text(
        val value: String,
    ) : Cell

    data class Num(
        val value: Long,
    ) : Cell

    /** Integer cents of reais, written as reais with two decimals, exactly. */
    data class Money(
        val cents: Long,
    ) : Cell

    /** A local date as an Excel serial day. */
    data class Day(
        val year: Int,
        val month: Int,
        val day: Int,
    ) : Cell
}

data class Sheet(
    val name: String,
    val header: List<String>,
    val rows: List<List<Cell>>,
)

object Xlsx {
    fun write(sheets: List<Sheet>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun put(
                path: String,
                xml: String,
            ) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(xml.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            put("[Content_Types].xml", contentTypes(sheets.size))
            put("_rels/.rels", ROOT_RELS)
            put("xl/workbook.xml", workbook(sheets))
            put("xl/_rels/workbook.xml.rels", workbookRels(sheets.size))
            put("xl/styles.xml", STYLES)
            sheets.forEachIndexed { i, s -> put("xl/worksheets/sheet${i + 1}.xml", sheet(s)) }
        }
        return out.toByteArray()
    }

    /** Days since 1899-12-30, Excel's epoch for dates after 1900-03-01. */
    fun serial(
        year: Int,
        month: Int,
        day: Int,
    ): Long =
        java.time.LocalDate
            .of(year, month, day)
            .toEpochDay() -
            java.time.LocalDate
                .of(1899, 12, 30)
                .toEpochDay()

    /** "1234.5" style reais from cents, exact, never through a floating point. */
    fun reais(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val abs = Math.abs(cents)
        return "$sign${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
    }

    private fun esc(s: String) =
        s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

    private fun col(i: Int): String {
        var n = i + 1
        val sb = StringBuilder()
        while (n > 0) {
            val r = (n - 1) % 26
            sb.insert(0, ('A' + r))
            n = (n - 1) / 26
        }
        return sb.toString()
    }

    private fun cell(
        ref: String,
        c: Cell,
    ): String =
        when (c) {
            is Cell.Text -> "<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${esc(c.value)}</t></is></c>"
            is Cell.Num -> "<c r=\"$ref\"><v>${c.value}</v></c>"
            is Cell.Money -> "<c r=\"$ref\" s=\"2\"><v>${reais(c.cents)}</v></c>"
            is Cell.Day -> "<c r=\"$ref\" s=\"1\"><v>${serial(c.year, c.month, c.day)}</v></c>"
        }

    private fun sheet(s: Sheet): String {
        val rows = listOf(s.header.map { Cell.Text(it) }) + s.rows
        val body =
            rows
                .mapIndexed { r, cells ->
                    "<row r=\"${r + 1}\">" + cells.mapIndexed { c, v -> cell("${col(c)}${r + 1}", v) }.joinToString("") + "</row>"
                }.joinToString("")
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
            // Wide enough for "Local ou aluno" and a name; the reader can still resize them.
            "<cols><col min=\"1\" max=\"${s.header.size.coerceAtLeast(1)}\" width=\"18\" customWidth=\"1\"/></cols>" +
            "<sheetData>$body</sheetData></worksheet>"
    }

    private fun contentTypes(n: Int) =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
            "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
            "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
            "<Override PartName=\"/xl/workbook.xml\" " +
            "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
            "<Override PartName=\"/xl/styles.xml\" " +
            "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
            (1..n).joinToString("") {
                "<Override PartName=\"/xl/worksheets/sheet$it.xml\" " +
                    "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            } +
            "</Types>"

    private fun workbook(sheets: List<Sheet>) =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
            "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>" +
            sheets
                .mapIndexed {
                    i,
                    s,
                    ->
                    "<sheet name=\"${esc(s.name.take(31))}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>"
                }.joinToString("") +
            "</sheets></workbook>"

    private fun workbookRels(n: Int) =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            (1..n).joinToString("") {
                "<Relationship Id=\"rId$it\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" " +
                    "Target=\"worksheets/sheet$it.xml\"/>"
            } +
            "<Relationship Id=\"rId${n + 1}\" " +
            "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>" +
            "</Relationships>"

    private const val ROOT_RELS =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" " +
            "Target=\"xl/workbook.xml\"/></Relationships>"

    // Style 1 is a date (dd/mm/yyyy, built-in 14), style 2 money with two decimals (built-in 4).
    private const val STYLES =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
            "<fonts count=\"1\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>" +
            "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill " +
            "patternType=\"gray125\"/></fill></fills>" +
            "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
            "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
            "<cellXfs count=\"3\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
            "<xf numFmtId=\"14\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>" +
            "<xf numFmtId=\"4\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/></cellXfs>" +
            "</styleSheet>"
}
