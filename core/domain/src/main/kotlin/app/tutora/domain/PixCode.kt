package app.tutora.domain

// The Pix "copia e cola" of a Fatura: a static BR Code (the Banco Central's EMV format), made on
// the phone with no network; the same text is drawn as the Fatura's QR code (AC-020-10, 11).

private const val NAME_MAX = 25
private const val CITY_MAX = 15

private val PLAIN =
    mapOf(
        "áàâãäå" to 'a',
        "éèêë" to 'e',
        "íìîï" to 'i',
        "óòôõö" to 'o',
        "úùûü" to 'u',
        "ç" to 'c',
        "ñ" to 'n',
        "ÁÀÂÃÄÅ" to 'A',
        "ÉÈÊË" to 'E',
        "ÍÌÎÏ" to 'I',
        "ÓÒÔÕÖ" to 'O',
        "ÚÙÛÜ" to 'U',
        "Ç" to 'C',
        "Ñ" to 'N',
    ).flatMap { (from, to) -> from.map { it to to } }.toMap()

/** Accents off and only printable ASCII kept, as bank apps expect in the name and city. */
private fun plain(s: String): String =
    s
        .map { PLAIN[it] ?: it }
        .filter { it.code in 32..126 }
        .joinToString("")
        .trim()

/** The key as Pix registers it: digits for CPF and CNPJ, "+55…" for a phone, an e-mail in lower case. */
fun pixKeyForCode(key: String): String {
    val k = key.trim()
    val digits = k.filter(Char::isDigit)
    return when (pixKind(k)) {
        PixKind.CPF, PixKind.CNPJ -> digits
        PixKind.PHONE ->
            when {
                k.startsWith('+') -> "+$digits"
                digits.length >= 12 && digits.startsWith("55") -> "+$digits"
                else -> "+55$digits"
            }
        PixKind.EMAIL -> k.lowercase()
        PixKind.RANDOM -> k.lowercase()
        PixKind.OTHER -> k
    }
}

private fun field(
    id: String,
    value: String,
): String = id + value.length.toString().padStart(2, '0') + value

/** CRC16-CCITT (polynomial 0x1021, start 0xFFFF), as four upper-case hex digits. */
internal fun crc16(s: String): String {
    var crc = 0xFFFF
    for (b in s.encodeToByteArray()) {
        crc = crc xor ((b.toInt() and 0xFF) shl 8)
        repeat(8) { crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1 }
        crc = crc and 0xFFFF
    }
    return crc.toString(16).uppercase().padStart(4, '0')
}

/**
 * The Pix "copia e cola" paying [cents] to [key], or null when the name or the city is missing
 * (both are required by the format; AC-020-11). With [cents] null the payer types the amount.
 */
fun pixPayload(
    key: String,
    name: String,
    city: String,
    cents: Cents?,
): String? {
    val n = plain(name).take(NAME_MAX).trim()
    val c = plain(city).take(CITY_MAX).trim()
    if (key.isBlank() || n.isEmpty() || c.isEmpty()) return null
    val body =
        field("00", "01") +
            field("26", field("00", "br.gov.bcb.pix") + field("01", pixKeyForCode(key))) +
            field("52", "0000") +
            field("53", "986") +
            (cents?.takeIf { it > 0 }?.let { field("54", "${it / 100}.${(it % 100).toString().padStart(2, '0')}") } ?: "") +
            field("58", "BR") +
            field("59", n) +
            field("60", c) +
            field("62", field("05", "***")) +
            "6304"
    return body + crc16(body)
}
