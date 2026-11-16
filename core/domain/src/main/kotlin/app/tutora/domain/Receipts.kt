package app.tutora.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

// Comprovantes (spec 021): the files a teacher keeps on a payment. The phone measures and
// copies them; these rules decide what is kept and how it is described.

enum class ReceiptKind(
    val key: String,
) {
    IMAGE("image"),
    PDF("pdf"),
    ;

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key }

        /** An image or a PDF; anything else is not a comprovante. */
        fun ofMime(mime: String?): ReceiptKind? =
            when {
                mime == null -> null
                mime == "application/pdf" -> PDF
                mime.startsWith("image/") -> IMAGE
                else -> null
            }
    }
}

/** A comprovante on a payment, in the order the teacher added them. */
data class Receipt(
    val id: String,
    val paymentId: String,
    val kind: ReceiptKind,
    val mime: String,
    /** The file's name in Tutora's comprovantes folder. */
    val file: String,
    val bytes: Long,
    val position: Int,
)

/** A photo's longer side is reduced to this before it is kept (AC-021-2). */
const val RECEIPT_MAX_SIDE = 2000

/** A PDF is kept as it is, up to this size (AC-021-2). */
const val RECEIPT_MAX_PDF_BYTES = 10L * 1000 * 1000

/** What happens to a file the teacher picks or shares (AC-021-2). */
sealed interface ReceiptIntake {
    /** Kept as it is. */
    data object Keep : ReceiptIntake

    /** An image scaled to these pixels, its aspect kept, before it is kept. */
    data class Shrink(
        val width: Int,
        val height: Int,
    ) : ReceiptIntake

    /** A PDF over 10 MB. */
    data object TooLarge : ReceiptIntake

    /** Neither an image nor a PDF. */
    data object Unsupported : ReceiptIntake
}

/**
 * Keep, shrink or refuse a file: an image larger than 2000 px on its longer side is scaled to
 * 2000 (the other side rounded half away from zero); a PDF is kept up to 10 MB (AC-021-2).
 */
fun receiptPolicy(
    mime: String?,
    bytes: Long,
    width: Int = 0,
    height: Int = 0,
): ReceiptIntake =
    when (ReceiptKind.ofMime(mime)) {
        null -> ReceiptIntake.Unsupported
        ReceiptKind.PDF -> if (bytes > RECEIPT_MAX_PDF_BYTES) ReceiptIntake.TooLarge else ReceiptIntake.Keep
        ReceiptKind.IMAGE -> {
            val longer = maxOf(width, height)
            if (longer <= RECEIPT_MAX_SIDE) {
                ReceiptIntake.Keep
            } else {
                // Integer half-up: (a × max + longer / 2) / longer, never a float.
                fun scale(side: Int) = ((side.toLong() * RECEIPT_MAX_SIDE + longer / 2) / longer).toInt().coerceAtLeast(1)
                ReceiptIntake.Shrink(scale(width), scale(height))
            }
        }
    }

/** How much room the comprovantes take, for Backup e planilha: "12 comprovantes · 8,4 MB" (AC-021-12). */
data class ReceiptUsage(
    val count: Int,
    /** Megabytes (1 MB = 1 000 000 bytes) in tenths, rounded half away from zero: 84 is 8,4 MB. */
    val tenthsOfMb: Long,
)

fun receiptUsage(
    count: Int,
    bytes: Long,
): ReceiptUsage = ReceiptUsage(count, (bytes + 50_000) / 100_000)

/** Anexar comprovante lists the payments of the last 60 days (AC-021-3). */
const val ATTACH_DAYS = 60

/** A payment Anexar comprovante can take a file: who, when, how, how much, and how many it has. */
data class AttachablePayment(
    val id: String,
    val payer: PayerRef,
    val day: LocalDate,
    /** The instant it was paid, which orders two payments of one day. */
    val paidAt: String,
    val method: PayMethod,
    val cents: Cents,
    val receipts: Int,
)

/** The payments of the last 60 days up to today, newest first (AC-021-3). */
fun attachable(
    payments: List<AttachablePayment>,
    today: LocalDate,
): List<AttachablePayment> {
    val from = today.minus(DatePeriod(days = ATTACH_DAYS))
    return payments.filter { it.day in from..today }.sortedByDescending { it.paidAt }
}
