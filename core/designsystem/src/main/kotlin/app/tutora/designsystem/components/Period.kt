package app.tutora.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import kotlinx.datetime.LocalDate

/**
 * A period as a filter field: its label, the quick periods in one segmented row, and De and Até
 * under them. Picking a date by hand makes the period those dates, and then no quick period is lit
 * ([selected] null). Dates come formatted by the caller, in the app language. [tag] marks the row;
 * the date fields are "$tag-from" and "$tag-to" (spec 020).
 */
@Composable
fun <T> PeriodField(
    label: String,
    presets: List<Pair<T, String>>,
    selected: T?,
    onPreset: (T) -> Unit,
    from: LocalDate,
    fromText: String,
    onFrom: (LocalDate) -> Unit,
    to: LocalDate,
    toText: String,
    onTo: (LocalDate) -> Unit,
    fromLabel: String,
    toLabel: String,
    modifier: Modifier = Modifier,
    tag: String = "period",
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        FieldLabel(label)
        SegmentedControl<T?>(
            presets,
            selected,
            { p -> p?.let(onPreset) },
            label,
            Modifier.testTag(tag),
            size = ControlSize.SM,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            DateField(fromLabel, fromText, from, onFrom, Modifier.weight(1f).testTag("$tag-from"))
            DateField(toLabel, toText, to, onTo, Modifier.weight(1f).testTag("$tag-to"))
        }
    }
}
