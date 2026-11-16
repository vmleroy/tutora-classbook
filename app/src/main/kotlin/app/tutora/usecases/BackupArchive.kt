package app.tutora.usecases

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * The full backup since spec 021: a zip with `tutora.json` (every table, as before) and each
 * comprovante under `comprovantes/` (AC-021-10). A file that is plain JSON is a backup made
 * before, and still reads.
 */
object BackupArchive {
    const val JSON = "tutora.json"
    private const val FILES = "comprovantes/"

    /** Writes the JSON, then every comprovante that is still on disk. */
    fun write(
        json: String,
        receipts: List<File>,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry(JSON))
            zip.write(json.encodeToByteArray())
            zip.closeEntry()
            receipts.filter { it.isFile }.forEach { f ->
                zip.putNextEntry(ZipEntry(FILES + f.name))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    /** A zip starts with "PK"; anything else is read as a JSON backup. */
    fun isArchive(bytes: ByteArray) = bytes.size >= 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()

    /** The JSON inside a zip, or the bytes themselves for a JSON backup; null for a zip without it. */
    fun json(bytes: ByteArray): ByteArray? {
        if (!isArchive(bytes)) return bytes
        return entries(bytes) { name, input -> if (name == JSON) input.readBytes() else null }.firstOrNull()
    }

    /** The comprovantes' names in a zip. */
    fun receiptNames(bytes: ByteArray): Set<String> =
        if (!isArchive(bytes)) emptySet() else entries(bytes) { name, _ -> name.receiptName() }.toSet()

    /** Writes the zip's comprovantes into [dir], only those in [wanted]; returns the names written. */
    fun extract(
        bytes: ByteArray,
        dir: File,
        wanted: Set<String>,
    ): Set<String> {
        if (!isArchive(bytes)) return emptySet()
        dir.mkdirs()
        return entries(bytes) { name, input ->
            name.receiptName()?.takeIf { it in wanted }?.also { n -> File(dir, n).outputStream().use { input.copyTo(it) } }
        }.toSet()
    }

    // Only a plain file name under comprovantes/, never a path that climbs out of the folder.
    private fun String.receiptName(): String? =
        removePrefix(FILES).takeIf { startsWith(FILES) && it.isNotEmpty() && '/' !in it && '\\' !in it && it != "." && it != ".." }

    private fun <T : Any> entries(
        bytes: ByteArray,
        each: (String, ZipInputStream) -> T?,
    ): List<T> =
        runCatching {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                generateSequence { zip.nextEntry }.mapNotNull { e -> if (e.isDirectory) null else each(e.name, zip) }.toList()
            }
        }.getOrDefault(emptyList())
}
