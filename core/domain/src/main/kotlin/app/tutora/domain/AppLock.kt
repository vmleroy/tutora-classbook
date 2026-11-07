package app.tutora.domain

// The app lock (spec 015): Tutora asks for the phone's own unlock when opened, and again once it
// was away longer than the teacher chose. Times are the phone's time since boot, in milliseconds,
// so changing the phone's clock never skips the lock.

/** Bloquear após (AC-015-4): 5 minutos at first (Victor, 7 Nov 2026). */
enum class LockAfter(
    val minutes: Int,
) {
    IMMEDIATELY(0),
    ONE_MINUTE(1),
    FIVE_MINUTES(5),
    ;

    companion object {
        val DEFAULT = FIVE_MINUTES

        fun of(minutes: Int?) = entries.firstOrNull { it.minutes == minutes } ?: DEFAULT
    }
}

private const val MINUTE_MS = 60_000L

/**
 * Whether coming back after leaving at [leftAtMs] asks for the unlock again (AC-015-8).
 * Imediatamente asks after any time away; the others once the time away reaches them.
 */
fun shouldLock(
    on: Boolean,
    after: LockAfter,
    leftAtMs: Long?,
    nowMs: Long,
): Boolean {
    if (!on || leftAtMs == null) return false
    val away = nowMs - leftAtMs
    return if (after == LockAfter.IMMEDIATELY) away >= 0 else away >= after.minutes * MINUTE_MS
}

/** The five kinds of Pix key, told apart only to mask each the way banks do (AC-015-16). */
enum class PixKind { CPF, CNPJ, PHONE, EMAIL, RANDOM, OTHER }

private val RANDOM_KEY = Regex("^[0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{12}$")
private val NUMBER_MARKS = Regex("^[+0-9 .\\-/()]+$")
private const val CPF_DIGITS = 11
private const val CNPJ_DIGITS = 14
private const val PIX_SHOWN = 4

/** Whether 11 digits are a CPF by its two check digits; otherwise they are a phone. */
private fun isCpf(d: String): Boolean {
    if (d.length != CPF_DIGITS || d.all { it == d[0] }) return false

    fun check(n: Int): Int {
        val sum = (0 until n).sumOf { (d[it] - '0') * (n + 1 - it) }
        return (sum * 10 % 11) % 10
    }
    return check(9) == d[9] - '0' && check(10) == d[10] - '0'
}

/** CPF, CNPJ, phone, e-mail or random key, typed with or without punctuation. */
fun pixKind(key: String): PixKind {
    val k = key.trim()
    val digits = k.filter(Char::isDigit)
    return when {
        k.contains('@') -> PixKind.EMAIL
        RANDOM_KEY.matches(k) && !NUMBER_MARKS.matches(k) -> PixKind.RANDOM
        !NUMBER_MARKS.matches(k) -> PixKind.OTHER
        k.startsWith('+') -> PixKind.PHONE
        digits.length == CNPJ_DIGITS -> PixKind.CNPJ
        digits.length == CPF_DIGITS && isCpf(digits) -> PixKind.CPF
        digits.length in 10..13 -> PixKind.PHONE
        else -> PixKind.OTHER
    }
}

/**
 * The Pix key with only part of it in sight, as banks show it (AC-015-16): CPF "•••.456.789-••",
 * CNPJ "••.345.678/0001-••", phone "(••) •••••-4321", e-mail "vi•••@gmail.com", random key
 * "123e••••4000", anything else "•••• 9876".
 */
fun maskPix(key: String): String {
    val k = key.trim()
    if (k.isEmpty()) return ""
    val d = k.filter(Char::isDigit)
    return when (pixKind(k)) {
        PixKind.CPF -> "•••.${d.substring(3, 6)}.${d.substring(6, 9)}-••"
        PixKind.CNPJ -> "••.${d.substring(2, 5)}.${d.substring(5, 8)}/${d.substring(8, 12)}-••"
        PixKind.PHONE -> "(••) •••••-${d.takeLast(PIX_SHOWN)}"
        PixKind.EMAIL -> {
            val local = k.substringBefore('@')
            local.take(if (local.length > 2) 2 else 1) + "•••@" + k.substringAfter('@')
        }
        PixKind.RANDOM -> k.take(PIX_SHOWN) + "••••" + k.takeLast(PIX_SHOWN)
        PixKind.OTHER -> if (k.length <= PIX_SHOWN) "••••" else "•••• " + k.takeLast(PIX_SHOWN)
    }
}
