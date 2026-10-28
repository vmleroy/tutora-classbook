package app.tutora.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import app.tutora.R
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.ButtonVariant
import app.tutora.designsystem.components.Chip
import app.tutora.designsystem.components.ListGroup
import app.tutora.designsystem.components.ListRow
import app.tutora.designsystem.components.MoneyField
import app.tutora.designsystem.components.RowTrailing
import app.tutora.designsystem.components.TextField
import app.tutora.designsystem.components.currentLanguage
import app.tutora.designsystem.components.formatMoney
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.CHARGE_PERCENTS
import app.tutora.domain.CancelReason
import app.tutora.domain.Charge
import app.tutora.domain.Lesson
import app.tutora.domain.chargeCents
import app.tutora.domain.valueWithoutTravel
import app.tutora.state.LocalServices
import app.tutora.ui.catalog.Field
import app.tutora.usecases.Cancellation
import kotlinx.coroutines.launch

/**
 * Cancelar aula: what happened, an optional reason, and whether it is charged: 50%, 100% or
 * another amount. Cancelling is not removing; nothing changes until it is confirmed (AC-006-18 to 22).
 */
@Composable
fun CancelSheet(
    l: Lesson,
    started: Boolean,
    onDismiss: () -> Unit,
) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val language = currentLanguage()
    var reason by remember { mutableStateOf(CancelReason.STUDENT) }
    var note by remember { mutableStateOf("") }
    var charging by remember { mutableStateOf(false) }
    val value = l.valueWithoutTravel()
    // A class without a price can only be charged another amount (AC-006-21).
    var charge by remember { mutableStateOf<Charge>(if (value != null) Charge.Percent(CHARGE_PERCENTS.first()) else Charge.Amount(0)) }
    val cents = if (charging && reason.chargeable) chargeCents(charge, value) else null
    val reasons = CancelReason.entries.filter { it != CancelReason.NO_SHOW || started }
    BottomSheet(
        stringResource(R.string.cancel_action),
        onDismiss,
        actions =
            listOf(
                { m -> Button(stringResource(R.string.cancel_back), onDismiss, m, variant = ButtonVariant.OUTLINE) },
                { m ->
                    Button(
                        if (cents != null && cents > 0) {
                            stringResource(R.string.cancel_confirmCharge, formatMoney(cents, language))
                        } else {
                            stringResource(R.string.cancel_action)
                        },
                        {
                            val c = Cancellation(reason, note, charge.takeIf { charging && reason.chargeable })
                            scope.launch { services.lessons.cancel(l.id, c) }
                            onDismiss()
                        },
                        m,
                        variant = ButtonVariant.DANGER,
                    )
                },
            ),
    ) {
        Field(stringResource(R.string.cancel_what)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                reasons.forEach { r -> Chip(reasonLabel(r), reason == r, { reason = r }) }
            }
        }
        TextField(stringResource(R.string.cancel_note), note, { note = it }, imeAction = ImeAction.Done)
        // A class the teacher cancels is never charged (AC-006-20).
        if (reason.chargeable) {
            ListGroup(
                listOf {
                    ListRow(
                        stringResource(R.string.cancel_charge),
                        subtitle = cents?.let { formatMoney(it, language) },
                        trailing = RowTrailing.SWITCH,
                        switchValue = charging,
                        onSwitchChange = { charging = it },
                    )
                },
            )
            if (charging) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                    if (value != null) {
                        CHARGE_PERCENTS.forEach { bp ->
                            Chip(
                                stringResource(R.string.fee_percent, bp / 100),
                                charge == Charge.Percent(bp),
                                { charge = Charge.Percent(bp) },
                            )
                        }
                    }
                    Chip(stringResource(R.string.cancel_other), charge is Charge.Amount, {
                        if (charge !is Charge.Amount) charge = Charge.Amount(cents ?: 0)
                    })
                }
                (charge as? Charge.Amount)?.let { a ->
                    MoneyField(stringResource(R.string.cancel_amount), a.cents, { charge = Charge.Amount(it ?: 0) })
                }
            }
        }
    }
}

@Composable
private fun reasonLabel(r: CancelReason): String =
    stringResource(
        when (r) {
            CancelReason.STUDENT -> R.string.cancel_reason_student
            CancelReason.NO_SHOW -> R.string.cancel_reason_noShow
            CancelReason.TEACHER -> R.string.cancel_reason_teacher
        },
    )
