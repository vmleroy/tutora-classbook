package app.tutora.app.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.tutora.app.R
import app.tutora.app.ui.durationText
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.components.BottomSheet
import app.tutora.designsystem.components.Button
import app.tutora.designsystem.components.ButtonVariant
import app.tutora.designsystem.components.DurationField
import app.tutora.designsystem.components.IconName
import app.tutora.designsystem.components.Stepper
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.MAX_DURATION_MIN

private const val PRICE_STEP = 500L // R$ 5

/** A label over a control, as every form field. */
@Composable
fun Field(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1_5)) {
        Text(label, style = TutoraTheme.type.fieldLabel, color = TutoraTheme.colors.inkMuted)
        content()
    }
}

/** A duration stepper in 5-minute steps; tapping the time opens hh:mm entry (AC-004-27). */
@Composable
fun DurationStepper(
    label: String,
    minutes: Int,
    onChange: (Int) -> Unit,
    min: Int = 15,
    max: Int = MAX_DURATION_MIN,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    var exact by rememberSaveable { mutableStateOf<Int?>(minutes) }
    val shown = durationText(minutes)
    Stepper(minutes.toLong(), {
        onChange(
            it.toInt(),
        )
    }, step = 5, min = min.toLong(), max = max.toLong(), format = { shown }, onPressValue = {
        exact = minutes
        open = true
    }, valueLabel = label)
    if (open) {
        val value = exact
        BottomSheet(
            label,
            { open = false },
            subtitle = stringResource(R.string.catalog_exactDuration),
            actions =
                listOf { m ->
                    Button(stringResource(R.string.catalog_done), {
                        if (value != null && value >= min) onChange(value)
                        open = false
                    }, m, enabled = value != null && value >= min)
                },
        ) { DurationField(label, minutes, { exact = it }, maxMinutes = max) }
    }
}

/**
 * Arquivar for an active item; Restaurar and Remover para sempre once archived. Removing asks
 * first and names the item (constitution rule 3, AC-004-24).
 */
@Composable
fun ArchiveActions(
    name: String,
    archived: Boolean,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onRemove: () -> Unit,
    removeBody: String = stringResource(R.string.catalog_removeBody),
) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    if (!archived) {
        Button(stringResource(R.string.catalog_archive), onArchive, variant = ButtonVariant.GHOST, icon = IconName.ARCHIVE)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Button(stringResource(R.string.catalog_restore), onRestore, variant = ButtonVariant.OUTLINE, fullWidth = true)
        Button(stringResource(R.string.catalog_remove), { confirming = true }, variant = ButtonVariant.DANGER, fullWidth = true)
    }
    if (confirming) {
        BottomSheet(
            stringResource(R.string.catalog_removeTitle, name),
            { confirming = false },
            subtitle = removeBody,
            actions =
                listOf(
                    { m -> Button(stringResource(R.string.catalog_cancel), { confirming = false }, m, variant = ButtonVariant.OUTLINE) },
                    { m ->
                        Button(stringResource(R.string.catalog_remove), {
                            confirming = false
                            onRemove()
                        }, m, variant = ButtonVariant.DANGER)
                    },
                ),
        )
    }
}
