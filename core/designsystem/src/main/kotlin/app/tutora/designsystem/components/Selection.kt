package app.tutora.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import app.tutora.designsystem.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.motionSpec
import app.tutora.designsystem.tokens.TutoraTokens.Opacity
import app.tutora.designsystem.tokens.TutoraTokens.Radius
import app.tutora.designsystem.tokens.TutoraTokens.Size
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.typedCents

/**
 * Two to four exclusive options as joined segments (spec 016): the outer ends are round, the
 * inner corners small, and the chosen one is filled with the main button color (AC-002-17
 * fades it unless the phone asks for reduced motion).
 */
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
    val last = options.lastIndex
    // A label too long for its segment wraps and every segment grows with it, so nothing is
    // clipped (AC-002-16). Each segment's hit area is at least the touch size (AC-002-12).
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .semantics { contentDescription = label }
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
    ) {
        options.forEachIndexed { i, (v, text) ->
            val on = i == index
            val outer = Radius.x2xl
            val inner = Radius.xs
            val shape =
                RoundedCornerShape(
                    topStart = if (i == 0) outer else inner,
                    bottomStart = if (i == 0) outer else inner,
                    topEnd = if (i == last) outer else inner,
                    bottomEnd = if (i == last) outer else inner,
                )
            val bg by animateColorAsState(
                if (on) c.btnPrimary else c.surfaceSunken,
                motionSpec(TutoraTheme.reduceMotion),
                label = "segment",
            )
            val drawn = if (size == ControlSize.SM) Size.buttonSm else Size.buttonMd
            // Pressed on the full touch size, drawn at the button size inside it (Victor, 4 Nov 2026).
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = Size.touch)
                        .selectable(selected = on, role = Role.RadioButton, onClick = { onChange(v) })
                        .padding(vertical = (Size.touch - drawn) / 2),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .clip(shape)
                        .background(bg)
                        // Small segments keep 4 dp a side, so seven short day names fit in a row (AC-014-26).
                        .padding(horizontal = if (size == ControlSize.SM) Spacing.s1 else Spacing.s2, vertical = Spacing.s1),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text,
                        style = if (size == ControlSize.SM) TutoraTheme.type.caption else TutoraTheme.type.button,
                        color = if (on) c.onBtnPrimary else c.ink,
                        textAlign = TextAlign.Center,
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

/** How a [Stepper]'s value is typed when tapped (AC-014-29). */
enum class StepperInput {
    /** A whole number; the value is it times the stepper's `typedScale` (30 → 3000 for 30%). */
    NUMBER,

    /** An amount of money, filling from the right like [MoneyField] (1, 2, 0, 0, 0 → R$ 120,00). */
    MONEY,
}

/**
 * Minus / value / plus, for durations and small amounts. Tapping the value turns it into a field
 * with the number keyboard; Done or leaving it keeps the nearest value between [min] and [max]
 * (AC-014-29). A caller with its own exact entry passes [onPressValue] instead.
 */
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
    input: StepperInput = StepperInput.NUMBER,
    /** What one typed unit is worth in [value]: 100 when the value is basis points typed as percent. */
    typedScale: Long = 1,
) {
    val c = TutoraTheme.colors
    val shape = RoundedCornerShape(Radius.xl)
    var editing by remember { mutableStateOf(false) }
    // One compact height for every stepper; [size] stays for callers (Victor, 29 Oct 2026).
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(Size.controlMd)
                .clip(shape)
                .background(c.surface)
                .border(if (editing) Size.borderStrong else Size.border, if (editing) c.focus else c.line, shape)
                .padding(horizontal = Spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(IconName.MINUS, decrementLabel, { onChange((value - step).coerceIn(min, max)) }, enabled = value > min)
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(
                    if (!editing) {
                        Modifier
                            .clickable(role = Role.Button, onClick = onPressValue ?: { editing = true })
                            .semantics { contentDescription = listOfNotNull(valueLabel, format(value)).joinToString(", ") }
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (editing) {
                StepperEntry(value, input, typedScale, valueLabel ?: format(value)) { typed ->
                    editing = false
                    typed?.let { onChange(it.coerceIn(min, max)) }
                }
            } else {
                Text(format(value), style = TutoraTheme.type.bodyStrong.copy(fontFeatureSettings = "tnum"), color = c.ink, maxLines = 1)
            }
        }
        StepButton(IconName.PLUS, incrementLabel, { onChange((value + step).coerceIn(min, max)) }, enabled = value < max)
    }
}

/** The field a tapped [Stepper] value becomes: focused at once, the number keyboard, Done to keep it. */
@Composable
private fun StepperEntry(
    value: Long,
    input: StepperInput,
    typedScale: Long,
    label: String,
    onDone: (Long?) -> Unit,
) {
    val c = TutoraTheme.colors
    val language = currentLanguage()
    val focus = remember { FocusRequester() }
    var typed by remember { mutableLongStateOf(if (input == StepperInput.MONEY) value else value / typedScale.coerceAtLeast(1)) }
    var focused by remember { mutableStateOf(false) }
    val text = if (input == StepperInput.MONEY) formatMoney(typed, language) else typed.toString()

    fun commit() = onDone(if (input == StepperInput.MONEY) typed else typed * typedScale.coerceAtLeast(1))
    BasicTextField(
        TextFieldValue(text, TextRange(text.length)),
        { next ->
            typed =
                if (input == StepperInput.MONEY) {
                    typedCents(next.text, typed)
                } else {
                    next.text
                        .filter { it.isDigit() }
                        .takeLast(MAX_TYPED_DIGITS)
                        .toLongOrNull() ?: 0L
                }
        },
        Modifier
            .fillMaxWidth()
            .focusRequester(focus)
            .testTag("stepper-entry")
            .semantics { contentDescription = label }
            .onFocusChanged {
                if (focused && !it.isFocused) commit()
                focused = it.isFocused
            },
        textStyle = TutoraTheme.type.bodyStrong.copy(fontFeatureSettings = "tnum", color = c.ink, textAlign = TextAlign.Center),
        singleLine = true,
        cursorBrush = SolidColor(c.ink),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { commit() }),
    )
    LaunchedEffect(Unit) { focus.requestFocus() }
}

/** Digits a typed stepper value keeps: enough for any count, minutes or percent. */
private const val MAX_TYPED_DIGITS = 6

/** − or + of a Stepper: a small circle, with the full 48 dp to press around it (AC-016-8). */
@Composable
private fun StepButton(
    icon: IconName,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    val c = TutoraTheme.colors
    Box(
        Modifier
            .size(Size.touch)
            .alpha(if (enabled) 1f else Opacity.disabled)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(Size.tile)
                .clip(CircleShape)
                .background(c.surfaceSunken),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, tint = c.ink, size = Size.iconSm) }
    }
}
