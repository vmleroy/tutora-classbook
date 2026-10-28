package app.tutora.domain

/** How a class ended up; LIVE arrives with the timer (spec 007). */
enum class LessonStatus(
    val key: String,
) {
    SCHEDULED("scheduled"),
    DONE("done"),
    CANCELLED("cancelled"),
    MISSED("missed"),
    ;

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: SCHEDULED
    }
}

/** "O que aconteceu?" in the cancel sheet (AC-006-18). */
enum class CancelReason(
    val key: String,
    val status: LessonStatus,
) {
    STUDENT("student", LessonStatus.CANCELLED),
    NO_SHOW("no_show", LessonStatus.MISSED),
    TEACHER("teacher", LessonStatus.CANCELLED),
    ;

    /** A class the teacher cancels is never charged (AC-006-20). */
    val chargeable: Boolean get() = this != TEACHER

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key }
    }
}

/** What a cancelled or missed class charges: a share of its value, or another amount. */
sealed interface Charge {
    data class Percent(
        val bp: Int,
    ) : Charge

    data class Amount(
        val cents: Cents,
    ) : Charge
}

/** 50% and 100%, the shares the cancel sheet offers. */
val CHARGE_PERCENTS = listOf(5_000, 10_000)

/**
 * The centavos a charge comes to for a class worth [valueWithoutTravel]; a percentage rounds once
 * and needs a price (AC-006-19, 21).
 */
fun chargeCents(
    charge: Charge,
    valueWithoutTravel: Cents?,
): Cents? =
    when (charge) {
        is Charge.Amount -> charge.cents
        is Charge.Percent -> valueWithoutTravel?.let { multiplyRate(it, charge.bp.toLong(), 10_000) }
    }
