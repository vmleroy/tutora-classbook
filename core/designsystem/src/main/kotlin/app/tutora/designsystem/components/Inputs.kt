package app.tutora.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import app.tutora.designsystem.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.tokens.TutoraTokens.Radius
import app.tutora.designsystem.tokens.TutoraTokens.Size
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.Cents
import app.tutora.domain.MAX_DURATION_MIN
import app.tutora.domain.MAX_FIELD_CENTS
import app.tutora.domain.durationDigits
import app.tutora.domain.hhmm
import app.tutora.domain.typedCents
import app.tutora.domain.typedDuration
import kotlinx.datetime.LocalDate

/**
 * A field's label. A required one ends in a red asterisk, read out as "required"; optional
 * fields carry no mark, and each form says once what the asterisk means.
 */
@Composable
fun FieldLabel(
    label: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    onPrimary: Boolean = false,
    inline: Boolean = false,
) {
    val c = TutoraTheme.colors
    val requiredWord = stringResource(R.string.ds_required)
    Text(
        buildAnnotatedString {
            append(label)
            if (required) withStyle(SpanStyle(color = c.dangerInk)) { append(" *") }
        },
        modifier.semantics { if (required) contentDescription = "$label, $requiredWord" },
        // Inside a receipt-style row the label reads as the line's name, not a caption.
        style = if (inline) TutoraTheme.type.body else TutoraTheme.type.fieldLabel,
        color =
            if (inline) {
                c.ink
            } else if (onPrimary) {
                c.onPrimaryMuted
            } else {
                c.inkMuted
            },
    )
}

/** The look all fields share: the label above, never only a placeholder. */
@Composable
private fun FieldFrame(
    label: String,
    helper: String?,
    error: String?,
    onPrimary: Boolean,
    errorKey: Any? = null,
    required: Boolean = false,
    field: @Composable (Modifier) -> Unit,
) {
    val c = TutoraTheme.colors
    val red by errorBlink(error?.let { it to errorKey })
    val shape = RoundedCornerShape(Radius.lg)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1_5)) {
        FieldLabel(label, required = required, onPrimary = onPrimary)
        field(
            Modifier
                .fillMaxWidth()
                .height(Size.controlMd)
                .background(errorFill(if (onPrimary) c.onPrimary.copy(alpha = 0.1f) else c.surface, error != null, red), shape)
                .border(
                    if (error != null) Size.borderStrong else Size.border,
                    if (error != null) {
                        c.danger
                    } else if (onPrimary) {
                        c.onPrimary.copy(alpha = 0.3f)
                    } else {
                        c.line
                    },
                    shape,
                ).padding(horizontal = Spacing.s3_5)
                .semantics {
                    contentDescription = label
                    if (error != null) error(error)
                },
        )
        if (error != null) {
            Text(error, style = TutoraTheme.type.meta, color = c.dangerInk)
        } else if (helper != null) {
            Text(helper, style = TutoraTheme.type.meta, color = c.inkMuted)
        }
    }
}

@Composable
private fun Input(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier,
    placeholder: String?,
    keyboardType: KeyboardType,
    onPrimary: Boolean,
    alignEnd: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    textStyle: TextStyle = TutoraTheme.type.body,
) {
    val c = TutoraTheme.colors
    val ink = if (onPrimary) c.onPrimary else c.ink
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle.copy(color = ink, textAlign = if (alignEnd) TextAlign.End else TextAlign.Start),
        // Next moves to the following field and Done closes the keyboard (Compose's defaults).
        keyboardOptions = KeyboardOptions(capitalization = capitalization, keyboardType = keyboardType, imeAction = imeAction),
        cursorBrush = SolidColor(ink),
        modifier = modifier,
        decorationBox = { inner ->
            // Fill the field's height so the text sits in its vertical middle.
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                // The hint and the input overlap, so both sit in the field's middle.
                Box(Modifier.weight(1f), contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart) {
                    if (value.text.isEmpty() && placeholder != null) {
                        Text(placeholder, style = TutoraTheme.type.body, color = if (onPrimary) c.onPrimaryMuted else c.inkMuted)
                    }
                    inner()
                }
            }
        },
    )
}

/** Labelled single-line text input. */
@Composable
fun TextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    helper: String? = null,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    imeAction: ImeAction = ImeAction.Next,
    onPrimary: Boolean = false,
    errorKey: Any? = null,
    required: Boolean = false,
) {
    // The field keeps its own selection, so the cursor can be placed anywhere to fix a typo.
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    if (field.text != value) field = TextFieldValue(value, TextRange(value.length))
    Column(modifier) {
        FieldFrame(label, helper, error, onPrimary, errorKey, required) { m ->
            Input(
                field,
                {
                    field = it
                    if (it.text != value) onValueChange(it.text)
                },
                m,
                placeholder,
                keyboardType,
                onPrimary,
                imeAction = imeAction,
                capitalization = capitalization,
            )
        }
    }
}

/**
 * A money field, as a receipt line: the label inside on the left, the amount on the right,
 * the whole row one tap target. It shows [cents] in reais for the app language and fills from
 * the right as digits are typed (1, 2, 0, 0, 0 → R$ 120,00); deleting drops the last digit and
 * digits past [maxCents] are refused (AC-003-8 to 10). With no amount (null or zero) it shows
 * [placeholder] in muted text, "R$ 0,00" unless the caller names the empty state ("Sem
 * preço"); erasing every digit reports null.
 */
@Composable
fun MoneyField(
    label: String,
    cents: Cents?,
    onCentsChange: (Cents?) -> Unit,
    modifier: Modifier = Modifier,
    maxCents: Cents = MAX_FIELD_CENTS,
    placeholder: String? = null,
    helper: String? = null,
    error: String? = null,
    errorKey: Any? = null,
    required: Boolean = false,
) {
    val c = TutoraTheme.colors
    val shape = RoundedCornerShape(Radius.lg)
    val language = currentLanguage()
    val shown = if (cents == null || cents == 0L) "" else formatMoney(cents, language)
    val red by errorBlink(error?.let { it to errorKey })
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s1_5)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = Size.controlMd)
                .background(errorFill(c.surface, error != null, red), shape)
                .border(
                    if (error != null || focused) Size.borderStrong else Size.border,
                    when {
                        error != null -> c.danger
                        focused -> c.primary
                        else -> c.line
                    },
                    shape,
                ).clickable(interactionSource = null, indication = null) { runCatching { focus.requestFocus() } }
                .padding(horizontal = Spacing.s3_5),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FieldLabel(label, Modifier.weight(1f), required = required, inline = true)
            Input(
                TextFieldValue(shown, TextRange(shown.length)),
                {
                    val next = typedCents(it.text, cents ?: 0, maxCents)
                    onCentsChange(if (next == 0L) null else next)
                },
                Modifier
                    .weight(1f)
                    .height(Size.controlMd)
                    .focusRequester(focus)
                    .onFocusChanged { focused = it.isFocused }
                    .semantics {
                        contentDescription = label
                        if (error != null) error(error)
                    },
                placeholder ?: formatMoney(0, language),
                KeyboardType.Number,
                false,
                alignEnd = true,
                textStyle = TutoraTheme.type.moneyMd,
            )
        }
        if (error != null) {
            Text(error, style = TutoraTheme.type.meta, color = c.dangerInk)
        } else if (helper != null) {
            Text(helper, style = TutoraTheme.type.meta, color = c.inkMuted)
        }
    }
}

/**
 * A duration or time typed as hh:mm, filling from the right (1, 3, 0 → 01:30). Minutes over
 * 59 or past [maxMinutes] show an error and report null (AC-004-27).
 */
@Composable
fun DurationField(
    label: String,
    minutes: Int,
    onMinutesChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    maxMinutes: Int = MAX_DURATION_MIN,
) {
    var digits by rememberSaveable { mutableStateOf(durationDigits(minutes)) }
    val valid = typedDuration(digits, digits, maxMinutes).minutes != null
    val shown = hhmm(digits)
    Column(modifier) {
        FieldFrame(
            label,
            null,
            if (valid) null else stringResource(R.string.ds_duration_invalid, hhmm(durationDigits(maxMinutes))),
            false,
        ) { m ->
            Input(
                TextFieldValue(shown, TextRange(shown.length)),
                {
                    val next = typedDuration(it.text, digits, maxMinutes)
                    digits = next.digits
                    onMinutesChange(next.minutes)
                },
                m,
                null,
                KeyboardType.Number,
                false,
                alignEnd = true,
            )
        }
    }
}

/** A field that opens a picker: shows the current choice, never typed into. */
@Composable
fun SelectField(
    value: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    label: String? = null,
    sub: String? = null,
    avatar: Pair<String, Tag>? = null,
    icon: IconName? = null,
    actionLabel: String? = null,
    isError: Boolean = false,
    errorKey: Any? = null,
    required: Boolean = false,
) {
    val c = TutoraTheme.colors
    val shape = RoundedCornerShape(Radius.lg)
    val red by errorBlink(if (isError) errorKey ?: Unit else null)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s1_5)) {
        if (label != null) FieldLabel(label, required = required)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = Size.controlMd)
                    .background(errorFill(c.surface, isError, red), shape)
                    .border(if (isError) Size.borderStrong else Size.border, if (isError) c.danger else c.line, shape)
                    .then(
                        if (onClick != null) {
                            Modifier
                                .clickable(role = Role.Button, onClick = onClick)
                                .semantics { contentDescription = listOfNotNull(label, value).joinToString(": ") }
                        } else {
                            Modifier
                        },
                    ).padding(horizontal = Spacing.s3_5, vertical = Spacing.s2),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (avatar != null) Avatar(avatar.first, avatar.second, size = AvatarSize.SM)
            if (icon != null) Icon(icon, tint = c.inkMuted, size = Size.iconSm)
            Column(Modifier.weight(1f)) {
                Text(value, style = TutoraTheme.type.bodyStrong, color = c.ink, maxLines = 1)
                if (sub != null) Text(sub, style = TutoraTheme.type.meta, color = c.inkMuted, maxLines = 1)
            }
            if (actionLabel != null) Text(actionLabel, style = TutoraTheme.type.caption, color = c.btnPrimary)
        }
    }
}

/**
 * A time of day: shows the time and opens the clock picker (dial, or typed with the keyboard
 * toggle), 24-hour or 12-hour as the app language writes times.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(
    label: String,
    hour: Int,
    minute: Int,
    onChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
    is24Hour: Boolean = currentLanguage() == app.tutora.domain.Language.PT_BR,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val shown =
        if (is24Hour) {
            "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
        } else {
            val h12 = if (hour % 12 == 0) 12 else hour % 12
            "$h12:${minute.toString().padStart(2, '0')} ${if (hour < 12) "AM" else "PM"}"
        }
    SelectField(shown, { open = true }, modifier, label = label, icon = IconName.CLOCK)
    if (open) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = is24Hour)
        var typing by rememberSaveable { mutableStateOf(false) }
        val c = TutoraTheme.colors
        val colors =
            TimePickerDefaults.colors(
                clockDialColor = c.surfaceSunken,
                selectorColor = c.btnPrimary,
                timeSelectorSelectedContainerColor = c.primarySoft,
                timeSelectorSelectedContentColor = c.primary,
                timeSelectorUnselectedContainerColor = c.surfaceSunken,
                timeSelectorUnselectedContentColor = c.ink,
                periodSelectorSelectedContainerColor = c.primarySoft,
                periodSelectorSelectedContentColor = c.primary,
            )
        BottomSheet(
            label,
            { open = false },
            actions =
                listOf(
                    { m ->
                        Button(
                            stringResource(if (typing) R.string.ds_time_dial else R.string.ds_time_type),
                            { typing = !typing },
                            m,
                            variant = ButtonVariant.OUTLINE,
                            icon = if (typing) IconName.CLOCK else null,
                        )
                    },
                    { m ->
                        Button(stringResource(R.string.ds_ok), {
                            onChange(state.hour, state.minute)
                            open = false
                        }, m)
                    },
                ),
        ) {
            // Digits typed on a keyboard while the clock shows fill the time from the right
            // (1, 4, 3, 0 → 14:30); Enter applies it. "Digitar" opens the boxes for the on-screen keyboard.
            var digits by remember { mutableStateOf("") }
            TypingTarget({ key ->
                when (key) {
                    is Typed.Digit -> digits = (digits + key.digit).takeLast(4)
                    Typed.Erase -> digits = digits.dropLast(1)
                    Typed.Enter -> {
                        onChange(state.hour, state.minute)
                        open = false
                        return@TypingTarget
                    }
                }
                val d = digits.padStart(4, '0')
                val h = d.take(2).toInt()
                val m = d.takeLast(2).toInt()
                if (h < 24 && m < 60) {
                    state.hour = h
                    state.minute = m
                }
            }, enabled = !typing) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (typing) TimeInput(state, colors = colors) else TimePicker(state, colors = colors)
                }
            }
        }
    }
}

/**
 * A calendar date: shows [text] (the date as the screen writes it) and opens the month
 * calendar, which can also be typed; [title] heads the calendar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    text: String,
    date: LocalDate,
    onPick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    SelectField(text, { open = true }, modifier, label = label, icon = IconName.CALENDAR)
    if (open) {
        // The calendar counts in UTC milliseconds at midnight.
        val state = rememberDatePickerState(initialSelectedDateMillis = date.toEpochDays() * DAY_MILLIS)
        val c = TutoraTheme.colors
        val colors =
            DatePickerDefaults.colors(
                containerColor = c.surface,
                titleContentColor = c.inkMuted,
                headlineContentColor = c.ink,
                weekdayContentColor = c.inkMuted,
                dayContentColor = c.ink,
                selectedDayContainerColor = c.btnPrimary,
                selectedDayContentColor = c.onBtnPrimary,
                todayContentColor = c.primary,
                todayDateBorderColor = c.primary,
                selectedYearContainerColor = c.btnPrimary,
                selectedYearContentColor = c.onBtnPrimary,
            )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                Button(stringResource(R.string.ds_ok), {
                    state.selectedDateMillis?.let { onPick(LocalDate.fromEpochDays(it / DAY_MILLIS)) }
                    open = false
                }, variant = ButtonVariant.GHOST, enabled = state.selectedDateMillis != null)
            },
            dismissButton = { Button(stringResource(R.string.ds_cancel), { open = false }, variant = ButtonVariant.GHOST) },
            colors = colors,
        ) {
            // Digits typed on a keyboard while the calendar shows pick the day: day and month in
            // the language's order, then an optional four-digit year; Enter applies it.
            var digits by remember { mutableStateOf("") }
            val dayFirst = currentLanguage() == app.tutora.domain.Language.PT_BR
            TypingTarget({ key ->
                when (key) {
                    is Typed.Digit -> digits = (digits + key.digit).take(8)
                    Typed.Erase -> digits = digits.dropLast(1)
                    Typed.Enter -> {
                        state.selectedDateMillis?.let { onPick(LocalDate.fromEpochDays(it / DAY_MILLIS)) }
                        open = false
                        return@TypingTarget
                    }
                }
                typedDate(digits, dayFirst, date.year)?.let {
                    state.selectedDateMillis = it.toEpochDays() * DAY_MILLIS
                    state.displayedMonthMillis = LocalDate(it.year, it.month, 1).toEpochDays() * DAY_MILLIS
                }
            }, enabled = state.displayMode == DisplayMode.Picker) {
                DatePicker(
                    state,
                    title =
                        title?.let {
                            {
                                Text(
                                    it,
                                    Modifier.padding(start = Spacing.s6, top = Spacing.s4),
                                    style = TutoraTheme.type.caption,
                                )
                            }
                        },
                    colors = colors,
                )
            }
        }
    }
}

/** The day typed as digits: dd mm or mm dd, then an optional yyyy; null until it is a real date. */
internal fun typedDate(
    digits: String,
    dayFirst: Boolean,
    year: Int,
): LocalDate? {
    if (digits.length != 4 && digits.length != 8) return null
    val a = digits.substring(0, 2).toInt()
    val b = digits.substring(2, 4).toInt()
    val y = if (digits.length == 8) digits.substring(4).toInt() else year
    val (day, month) = if (dayFirst) a to b else b to a
    return runCatching { LocalDate(y, month, day) }.getOrNull()
}

private sealed interface Typed {
    data class Digit(
        val digit: Char,
    ) : Typed

    data object Erase : Typed

    data object Enter : Typed
}

/** Takes the keyboard's keys while a picker is open, so a time or date can be typed onto it. */
@Composable
private fun TypingTarget(
    onKey: (Typed) -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val focus = remember { FocusRequester() }
    Box(
        Modifier
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { e ->
                if (!enabled || e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val ch = e.utf16CodePoint.toChar()
                val typed =
                    when {
                        ch in '0'..'9' -> Typed.Digit(ch)
                        e.key == Key.Backspace -> Typed.Erase
                        e.key == Key.Enter || e.key == Key.NumPadEnter -> Typed.Enter
                        else -> null
                    }
                typed?.let(onKey)
                typed != null
            },
    ) { content() }
    LaunchedEffect(enabled) { if (enabled) runCatching { focus.requestFocus() } }
}

/**
 * How red an errored input is, 0 to 1: a new or repeated error blinks three times, then stays
 * red; with reduced motion it is red at once. [key] changes with each error (and each failed
 * attempt), null when there is none.
 */
@Composable
internal fun errorBlink(key: Any?): State<Float> {
    val red = remember { Animatable(1f) }
    val reduce = TutoraTheme.reduceMotion
    LaunchedEffect(key) {
        red.snapTo(1f)
        if (key != null && !reduce) {
            repeat(3) {
                red.animateTo(0f, tween(BLINK_MS))
                red.animateTo(1f, tween(BLINK_MS))
            }
        }
    }
    return red.asState()
}

/** An input's fill: its own color, or the soft danger red while it has an error. */
@Composable
private fun errorFill(
    base: Color,
    isError: Boolean,
    red: Float,
): Color = if (isError) lerp(base, TutoraTheme.colors.dangerSoft, red) else base

/** A form's own error, outside any field (a missing choice, a clash): red, with the alert icon, blinking as it appears. */
@Composable
fun FieldError(
    text: String,
    modifier: Modifier = Modifier,
    key: Any? = null,
) {
    val red by errorBlink(text to key)
    Row(
        modifier.alpha(0.3f + 0.7f * red).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(Spacing.s1_5),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(IconName.ALERT, size = Size.iconSm, tint = TutoraTheme.colors.dangerInk)
        Text(text, style = TutoraTheme.type.meta, color = TutoraTheme.colors.dangerInk)
    }
}

private const val BLINK_MS = 160

private const val DAY_MILLIS = 86_400_000L
