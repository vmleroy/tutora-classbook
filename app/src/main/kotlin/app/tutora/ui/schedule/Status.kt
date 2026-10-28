package app.tutora.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.tutora.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.BadgeTone
import app.tutora.designsystem.components.BlockState
import app.tutora.designsystem.components.Chip
import app.tutora.designsystem.components.ControlSize
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.MoneyField
import app.tutora.designsystem.components.RowTrailing
import app.tutora.designsystem.components.SegmentedControl
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.FEE_WINDOWS
import app.tutora.domain.FeeAmount
import app.tutora.domain.FeeChoice
import app.tutora.domain.FeeRule
import app.tutora.domain.FeeSuggestion
import app.tutora.domain.Lesson
import app.tutora.domain.LessonStatus
import app.tutora.state.LocalServices
import app.tutora.ui.catalog.Field

@Composable
fun statusLabel(s: LessonStatus): String =
    stringResource(
        when (s) {
            LessonStatus.SCHEDULED -> R.string.status_scheduled
            LessonStatus.DONE -> R.string.status_done
            LessonStatus.CANCELLED -> R.string.status_cancelled
            LessonStatus.MISSED -> R.string.status_missed
        },
    )

/** What a status means, under Situação (AC-006-9). */
@Composable
fun statusHint(s: LessonStatus): String =
    stringResource(
        when (s) {
            LessonStatus.SCHEDULED -> R.string.status_hint_scheduled
            LessonStatus.DONE -> R.string.status_hint_done
            LessonStatus.CANCELLED -> R.string.status_hint_cancelled
            LessonStatus.MISSED -> R.string.status_hint_missed
        },
    )

/** "Cancelada", or "Cancelada · taxa R$ 60,00" when a fee is kept (AC-006-14, 15). */
@Composable
fun statusWithFee(l: Lesson): String {
    val label = statusLabel(l.status)
    val fee = l.feeCents ?: return label
    return stringResource(R.string.lesson_statusFee, label, formatMoney(fee, currentLanguage()))
}

/** The badge a day list shows for a class that is no longer just scheduled (AC-006-15). */
@Composable
fun statusBadge(l: Lesson): Triple<BadgeTone, String, IconName?>? =
    when (l.status) {
        LessonStatus.SCHEDULED -> null
        LessonStatus.DONE -> Triple(BadgeTone.SUCCESS, statusLabel(l.status), IconName.CHECK)
        LessonStatus.CANCELLED -> Triple(BadgeTone.NEUTRAL, statusWithFee(l), null)
        LessonStatus.MISSED -> Triple(BadgeTone.DANGER, statusWithFee(l), null)
    }

fun blockState(
    l: Lesson,
    nextId: String?,
): BlockState =
    when {
        l.id == nextId -> BlockState.NEXT
        l.status == LessonStatus.DONE -> BlockState.DONE
        l.status == LessonStatus.CANCELLED || l.status == LessonStatus.MISSED -> BlockState.MUTED
        else -> BlockState.DEFAULT
    }

/** "50%" or "R$ 30,00". */
@Composable
fun feeAmountText(a: FeeAmount): String =
    when (a) {
        is FeeAmount.Percent -> stringResource(R.string.fee_percent, a.bp / 100)
        is FeeAmount.Fixed -> formatMoney(a.cents, currentLanguage())
    }

/** Why a fee is suggested: "R$ 60,00 · 50% · menos de 24h" (AC-006-23). */
@Composable
fun feeWhy(c: FeeSuggestion.Charge): String {
    val money = formatMoney(c.cents, currentLanguage())
    val window = c.windowH
    return if (window != null) {
        stringResource(R.string.fee_whyLate, money, feeAmountText(c.amount), window)
    } else {
        stringResource(R.string.fee_whyMiss, money, feeAmountText(c.amount))
    }
}

/** Why there is no fee to keep (AC-006-25, 26). */
@Composable
fun feeReason(
    s: FeeSuggestion,
    status: LessonStatus,
): String =
    stringResource(
        when (s) {
            FeeSuggestion.EnoughNotice -> R.string.fee_enoughNotice
            FeeSuggestion.NoPrice -> R.string.fee_noPrice
            else -> if (status == LessonStatus.MISSED) R.string.fee_ruleOffMiss else R.string.fee_ruleOffLate
        },
    )

/** One line for a rule: "Cobra 50% se avisar com menos de 24h · Cobra 100% da aula". */
@Composable
fun ruleSummary(r: FeeRule): String {
    val parts =
        listOfNotNull(
            if (r.lateOn) stringResource(R.string.fees_lateOn, feeAmountText(r.late), r.windowH) else null,
            if (r.missOn) stringResource(R.string.fees_missOn, feeAmountText(r.miss)) else null,
        )
    return if (parts.isEmpty()) stringResource(R.string.fees_summaryOff) else parts.joinToString(" · ")
}

// A fixed fee starts here when the teacher picks Valor fixo (the mockup's R$ 30).
private const val FIXED_START_CENTS = 3_000L

/** The amount chips: 50%, 100% or a fixed amount the teacher types. */
@Composable
private fun AmountPicker(
    amount: FeeAmount,
    onChange: (FeeAmount) -> Unit,
) {
    Field(stringResource(R.string.fees_charge)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            listOf(5_000, 10_000).forEach { bp ->
                Chip(stringResource(R.string.fee_percent, bp / 100), amount == FeeAmount.Percent(bp), { onChange(FeeAmount.Percent(bp)) })
            }
            Chip(stringResource(R.string.fee_fixed), amount is FeeAmount.Fixed, {
                if (amount !is FeeAmount.Fixed) onChange(FeeAmount.Fixed(FIXED_START_CENTS))
            })
        }
    }
    if (amount is FeeAmount.Fixed) {
        MoneyField(stringResource(R.string.fee_fixedAmount), amount.cents, { onChange(FeeAmount.Fixed(it ?: 0)) })
    }
}

/** Cancelou em cima da hora and Faltou sem avisar, for Ajustes and a student's or place's own rule (AC-006-18, 20). */
@Composable
fun FeeRuleEditor(
    rule: FeeRule,
    onChange: (FeeRule) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        ListGroup(
            listOf {
                ListRow(
                    stringResource(R.string.fees_late),
                    subtitle =
                        if (rule.lateOn) {
                            stringResource(R.string.fees_lateOn, feeAmountText(rule.late), rule.windowH)
                        } else {
                            stringResource(R.string.fees_off)
                        },
                    trailing = RowTrailing.SWITCH,
                    switchValue = rule.lateOn,
                    onSwitchChange = { onChange(rule.copy(lateOn = it)) },
                )
            },
        )
        if (rule.lateOn) {
            Column(Modifier.padding(horizontal = Spacing.s1), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
                Field(stringResource(R.string.fees_window)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                        FEE_WINDOWS.forEach { h ->
                            Chip(stringResource(R.string.fees_windowH, h), rule.windowH == h, { onChange(rule.copy(windowH = h)) })
                        }
                    }
                }
                AmountPicker(rule.late) { onChange(rule.copy(late = it)) }
            }
        }
        ListGroup(
            listOf {
                ListRow(
                    stringResource(R.string.fees_miss),
                    subtitle =
                        if (rule.missOn) {
                            stringResource(R.string.fees_missOn, feeAmountText(rule.miss))
                        } else {
                            stringResource(R.string.fees_off)
                        },
                    trailing = RowTrailing.SWITCH,
                    switchValue = rule.missOn,
                    onSwitchChange = { onChange(rule.copy(missOn = it)) },
                )
            },
        )
        if (rule.missOn) {
            Column(Modifier.padding(horizontal = Spacing.s1)) {
                AmountPicker(rule.miss) { onChange(rule.copy(miss = it)) }
            }
        }
    }
}

/** A muted line of explanation under a field. */
@Composable
fun HintLine(text: String) {
    Text(text, style = TutoraTheme.type.meta, color = TutoraTheme.colors.inkMuted)
}

private enum class ChoiceKind { DEFAULT, OWN, NEVER }

/** "Cancelamentos e faltas" on a student or place: Padrão, Própria or Não cobrar (AC-006-20). */
@Composable
fun FeeChoiceField(
    choice: FeeChoice,
    onChange: (FeeChoice) -> Unit,
) {
    val settings by LocalServices.current.settings.collectAsState(initial = null)
    val default = settings?.feeRule ?: FeeRule()
    val kind =
        when (choice) {
            FeeChoice.Default -> ChoiceKind.DEFAULT
            is FeeChoice.Own -> ChoiceKind.OWN
            FeeChoice.Never -> ChoiceKind.NEVER
        }
    Field(stringResource(R.string.fees_title)) {
        SegmentedControl(
            listOf(
                ChoiceKind.DEFAULT to stringResource(R.string.fees_choice_default),
                ChoiceKind.OWN to stringResource(R.string.fees_choice_own),
                ChoiceKind.NEVER to stringResource(R.string.fees_choice_never),
            ),
            kind,
            {
                onChange(
                    when (it) {
                        ChoiceKind.DEFAULT -> FeeChoice.Default
                        // An own rule starts as a copy of the default.
                        ChoiceKind.OWN -> choice as? FeeChoice.Own ?: FeeChoice.Own(default)
                        ChoiceKind.NEVER -> FeeChoice.Never
                    },
                )
            },
            stringResource(R.string.fees_title),
            size = ControlSize.SM,
        )
        when (choice) {
            FeeChoice.Default -> HintLine(ruleSummary(default))
            is FeeChoice.Own -> FeeRuleEditor(choice.rule) { onChange(FeeChoice.Own(it)) }
            FeeChoice.Never -> HintLine(stringResource(R.string.fees_neverHint))
        }
    }
}
