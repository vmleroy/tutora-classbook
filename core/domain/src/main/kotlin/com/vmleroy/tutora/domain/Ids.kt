package com.vmleroy.tutora.domain

/**
 * A UUIDv7: 48 bits of Unix milliseconds, then random bits, so ids sort by creation time.
 * [random] must hold at least 10 bytes; the caller supplies them, keeping this pure.
 */
fun uuidv7(
    nowMs: Long,
    random: ByteArray,
): String {
    val bytes = ByteArray(16)
    var ms = nowMs
    for (i in 5 downTo 0) {
        bytes[i] = (ms and 0xFF).toByte()
        ms = ms shr 8
    }
    random.copyInto(bytes, 6, 0, 10)
    bytes[6] = (0x70 or (bytes[6].toInt() and 0x0F)).toByte() // version 7
    bytes[8] = (0x80 or (bytes[8].toInt() and 0x3F)).toByte() // RFC 4122 variant
    val hex = bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}"
}
