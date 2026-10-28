package app.tutora.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import app.tutora.designsystem.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.motionSpec
import app.tutora.designsystem.tokens.TutoraTokens.Opacity
import app.tutora.designsystem.tokens.TutoraTokens.Radius
import app.tutora.designsystem.tokens.TutoraTokens.Size
import app.tutora.designsystem.tokens.TutoraTokens.Spacing

/** Two to four exclusive options; the selected one is a surface thumb that slides (AC-002-17). */
@Composable
fun <T> SegmentedControl(
    options: List<Pair<T, String>>,
    value: T,
    onChange: (T) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    size: ControlSize = ControlSize.MD,
) {
    val c = TutoraTheme.colors
    val index = options.indexOfFirst { it.first == value }.coerceAtLeast(0)
    val height = if (size == ControlSize.SM) Size.controlSm else Size.touch
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.md))
                .background(c.surfaceSunken)
                .padding(Spacing.s1)
                .semantics { contentDescription = label }
                .selectableGroup(),
    ) {
        val segment = (maxWidth - Spacing.s1 * (options.size - 1)) / options.size
        val x by animateDpAsState((segment + Spacing.s1) * index, motionSpec(TutoraTheme.reduceMotion), label = "thumb")
        // The thumb is the control's height; each segment's hit area is at least 44 (AC-002-12).
        Box(
            Modifier
                .offset { IntOffset(x.roundToPx(), ((Size.touch - height) / 2).roundToPx()) }
                .width(segment)
                .height(height)
                .clip(RoundedCornerShape(Radius.sm))
                .background(c.surface),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            options.forEachIndexed { i, (v, text) ->
                Box(
                    modifier =
                        Modifier
                            .width(segment)
                            .height(Size.touch)
                            .selectable(selected = i == index, role = Role.RadioButton, onClick = { onChange(v) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text,
                        style = if (size == ControlSize.SM) TutoraTheme.type.caption else TutoraTheme.type.rowTitle,
                        color = if (i == index) c.btnPrimary else c.inkMuted,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** A filter or choice pill; selected is filled with the main color. */
@Composable
fun Chip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = TutoraTheme.colors
    val shape = RoundedCornerShape(Radius.full)
    Box(
        modifier =
            modifier
                // The hit area is the full 44; the pill drawn inside is 36 (AC-002-12).
                .sizeIn(minWidth = Size.touch, minHeight = Size.touch)
                .selectable(selected = selected, role = Role.Checkbox, onClick = onClick)
                .padding(vertical = (Size.touch - Size.controlSm) / 2)
                .height(Size.controlSm)
                .clip(shape)
                .background(if (selected) c.btnPrimary else c.surface, shape)
                .border(if (selected) Size.borderStrong else Size.border, if (selected) c.btnPrimary else c.line, shape)
                .padding(horizontal = Spacing.s3_5),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = TutoraTheme.type.caption, color = if (selected) c.onBtnPrimary else c.ink, maxLines = 1)
    }
}

/** On or off for one setting; only the switch itself is pressable. */
@Composable
fun Switch(
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = TutoraTheme.colors
    val reduce = TutoraTheme.reduceMotion
    val travel = Size.switchWidth - Size.switchThumb - (Size.switchHeight - Size.switchThumb)
    val x by animateDpAsState(travel * (if (checked) 1f else 0f), motionSpec(reduce), label = "knob")
    val track by animateColorAsState(if (checked) c.btnPrimary else c.lineStrong, motionSpec(reduce), label = "track")
    Box(
        modifier =
            modifier
                .sizeIn(minWidth = Size.touch, minHeight = Size.touch)
                .alpha(if (enabled) 1f else Opacity.disabled)
                .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
                .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(Size.switchWidth)
                .height(Size.switchHeight)
                .clip(CircleShape)
                .background(track)
                .padding((Size.switchHeight - Size.switchThumb) / 2),
        ) {
            Box(
                Modifier
                    .offset { IntOffset(x.roundToPx(), 0) }
                    .size(Size.switchThumb)
                    .clip(CircleShape)
                    .background(c.surface),
            )
        }
    }
}

/** A labelled box to tick; the whole row is the hit area. */
@Composable
fun Checkbox(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = TutoraTheme.colors
    val shape = RoundedCornerShape(Radius.xs)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .sizeIn(minHeight = Size.touch)
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(Size.checkbox)
                .clip(shape)
                .background(if (checked) c.btnPrimary else c.surface)
                .border(Size.borderStrong, if (checked) c.btnPrimary else c.lineStrong, shape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(IconName.CHECK, tint = c.onBtnPrimary, size = Size.iconSm)
        }
        Text(label, style = TutoraTheme.type.body, color = c.ink)
    }
}

/** Minus / value / plus, for durations and small amounts; tapping the value can open exact entry. */
@Composable
fun Stepper(
    value: Long,
    onChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    step: Long = 15,
    min: Long = 0,
    max: Long = Long.MAX_VALUE,
    format: (Long) -> String = { it.toString() },
    onPressValue: (() -> Unit)? = null,
    valueLabel: String? = null,
    decrementLabel: String = stringResource(R.string.ds_decrease),
    incrementLabel: String = stringResource(R.string.ds_increase),
    size: ControlSize = ControlSize.MD,
) {
    val c = TutoraTheme.colors
    val shape = RoundedCornerShape(Radius.xl)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(if (size == ControlSize.SM) Size.controlMd else Size.controlLg)
                .clip(shape)
                .background(c.surface)
                .border(Size.border, c.line, shape)
                .padding(horizontal = Spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(IconName.MINUS, decrementLabel, { onChange((value - step).coerceIn(min, max)) }, enabled = value > min)
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(
                    if (onPressValue != null) {
                        Modifier
                            .clickable(role = Role.Button, onClick = onPressValue)
                            .semantics { contentDescription = listOfNotNull(valueLabel, format(value)).joinToString(", ") }
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(format(value), style = TutoraTheme.type.moneyMd, color = c.ink, maxLines = 1)
        }
        IconButton(IconName.PLUS, incrementLabel, { onChange((value + step).coerceIn(min, max)) }, enabled = value < max)
    }
}
