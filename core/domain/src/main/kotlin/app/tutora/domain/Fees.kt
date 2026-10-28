package app.tutora.domain

import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

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

/** What a fee charges: a share of the class's value, or a fixed amount. */
sealed interface FeeAmount {
    data class Percent(
        val bp: Int,
    ) : FeeAmount

    data class Fixed(
        val cents: Cents,
    ) : FeeAmount
}

/** The notice windows a late cancellation can use, in hours. */
val FEE_WINDOWS = listOf(2, 12, 24, 48)

/** Cancelou em cima da hora and Faltou sem avisar (Fees mockup). */
data class FeeRule(
    val lateOn: Boolean = false,
    val windowH: Int = 24,
    val late: FeeAmount = FeeAmount.Percent(5_000),
    val missOn: Boolean = false,
    val miss: FeeAmount = FeeAmount.Percent(10_000),
)

/** A student's or place's choice: follow the default, its own rule, or never charge. */
sealed interface FeeChoice {
    data object Default : FeeChoice

    data class Own(
        val rule: FeeRule,
    ) : FeeChoice

    data object Never : FeeChoice
}

/** The rule a class follows; null means it never charges (AC-006-21). */
fun ruleFor(
    student: FeeChoice?,
    place: FeeChoice,
    default: FeeRule,
): FeeRule? {
    for (choice in listOfNotNull(student, place)) {
        when (choice) {
            is FeeChoice.Own -> return choice.rule
            FeeChoice.Never -> return null
            FeeChoice.Default -> Unit
        }
    }
    return default
}

sealed interface FeeSuggestion {
    data class Charge(
        val cents: Cents,
        val amount: FeeAmount,
        /** The window, for a late cancellation; null for a no-show. */
        val windowH: Int?,
    ) : FeeSuggestion

    data object EnoughNotice : FeeSuggestion

    data object RuleOff : FeeSuggestion

    data object NoPrice : FeeSuggestion
}

/** A fee amount for a class worth [value] without travel; a percentage rounds once (AC-006-24). */
fun feeCents(
    amount: FeeAmount,
    value: Cents?,
): Cents? =
    when (amount) {
        is FeeAmount.Fixed -> amount.cents
        is FeeAmount.Percent -> value?.let { multiplyRate(it, amount.bp.toLong(), 10_000) }
    }

/**
 * What a cancelled or missed class should charge (AC-006-22 to 26). A cancellation is late when
 * the notice came less than the window before the start, or after it.
 */
fun suggestFee(
    rule: FeeRule?,
    status: LessonStatus,
    valueWithoutTravel: Cents?,
    startAt: String,
    noticeAt: String?,
): FeeSuggestion? {
    if (status != LessonStatus.CANCELLED && status != LessonStatus.MISSED) return null
    val late = status == LessonStatus.CANCELLED
    if (rule == null || !(if (late) rule.lateOn else rule.missOn)) return FeeSuggestion.RuleOff
    if (late) {
        val notice = noticeAt?.let(Instant::parse) ?: return FeeSuggestion.EnoughNotice
        if (Instant.parse(startAt) - notice >= rule.windowH.hours) return FeeSuggestion.EnoughNotice
    }
    val amount = if (late) rule.late else rule.miss
    val cents = feeCents(amount, valueWithoutTravel) ?: return FeeSuggestion.NoPrice
    return FeeSuggestion.Charge(cents, amount, if (late) rule.windowH else null)
}
