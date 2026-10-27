package com.vmleroy.tutora.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import com.vmleroy.tutora.designsystem.R
import com.vmleroy.tutora.designsystem.TutoraTheme
import com.vmleroy.tutora.designsystem.tokens.TutoraTokens.Radius
import com.vmleroy.tutora.designsystem.tokens.TutoraTokens.Size
import com.vmleroy.tutora.designsystem.tokens.TutoraTokens.Spacing
import com.vmleroy.tutora.domain.Cents
import com.vmleroy.tutora.domain.MAX_DURATION_MIN
import com.vmleroy.tutora.domain.MAX_FIELD_CENTS
import com.vmleroy.tutora.domain.durationDigits
import com.vmleroy.tutora.domain.hhmm
import com.vmleroy.tutora.domain.typedCents
import com.vmleroy.tutora.domain.typedDuration

/** The look all fields share: the label above, never only a placeholder. */
@Composable
private fun FieldFrame(
    label: String,
    helper: String?,
    error: String?,
    onPrimary: Boolean,
    field: @Composable (Modifier) -> Unit,
) {
    val c = TutoraTheme.colors
    val shape = RoundedCornerShape(Radius.lg)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1_5)) {
        Text(label, style = TutoraTheme.type.fieldLabel, color = if (onPrimary) c.onPrimaryMuted else c.inkMuted)
        field(
            Modifier
                .fillMaxWidth()
                .height(Size.controlMd)
                .background(if (onPrimary) c.onPrimary.copy(alpha = 0.1f) else c.surface, shape)
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
        val help = error ?: helper
        if (help != null) {
            Text(help, style = TutoraTheme.type.meta, color = if (error != null) c.dangerInk else c.inkMuted)
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
) {
    val c = TutoraTheme.colors
    val ink = if (onPrimary) c.onPrimary else c.ink
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TutoraTheme.type.body.copy(color = ink, textAlign = if (alignEnd) TextAlign.End else TextAlign.Start),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        cursorBrush = SolidColor(ink),
        modifier = modifier,
        decorationBox = { inner ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
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
    onPrimary: Boolean = false,
) {
    Column(modifier) {
        FieldFrame(label, helper, error, onPrimary) { m ->
            Input(TextFieldValue(value, TextRange(value.length)), { onValueChange(it.text) }, m, placeholder, keyboardType, onPrimary)
        }
    }
}

/**
 * A money field: shows [cents] in reais for the app language and fills from the right as
 * digits are typed (1, 2, 0, 0, 0 → R$ 120,00); deleting drops the last digit and digits past
 * [maxCents] are refused (AC-003-8 to 10). The caret stays at the end.
 */
@Composable
fun MoneyField(
    label: String,
    cents: Cents,
    onCentsChange: (Cents) -> Unit,
    modifier: Modifier = Modifier,
    maxCents: Cents = MAX_FIELD_CENTS,
    helper: String? = null,
    error: String? = null,
) {
    val shown = formatMoney(cents, currentLanguage())
    Column(modifier) {
        FieldFrame(label, helper, error, false) { m ->
            Input(
                TextFieldValue(shown, TextRange(shown.length)),
                { onCentsChange(typedCents(it.text, cents, maxCents)) },
                m,
                null,
                KeyboardType.Number,
                false,
                alignEnd = true,
            )
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
) {
    val c = TutoraTheme.colors
    val shape = RoundedCornerShape(Radius.lg)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s1_5)) {
        if (label != null) Text(label, style = TutoraTheme.type.fieldLabel, color = c.inkMuted)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = Size.controlMd)
                    .background(c.surface, shape)
                    .border(Size.border, c.line, shape)
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
