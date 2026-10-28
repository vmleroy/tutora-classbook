package app.tutora.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.tutora.designsystem.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.tokens.TutoraTokens.Opacity
import app.tutora.designsystem.tokens.TutoraTokens.Radius
import app.tutora.designsystem.tokens.TutoraTokens.Size
import app.tutora.designsystem.tokens.TutoraTokens.Spacing

enum class ButtonVariant { PRIMARY, ACCENT, OUTLINE, GHOST, DANGER }

enum class ControlSize { SM, MD, LG }

/** Labelled action. One primary per screen; destructive actions are DANGER. */
@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    size: ControlSize = ControlSize.MD,
    icon: IconName? = null,
    fullWidth: Boolean = false,
    loading: Boolean = false,
    enabled: Boolean = true,
) {
    val c = TutoraTheme.colors
    val (bg, fg, line) =
        when (variant) {
            ButtonVariant.PRIMARY -> Triple(c.btnPrimary, c.onBtnPrimary, Color.Transparent)
            ButtonVariant.ACCENT -> Triple(c.accent, c.onAccent, Color.Transparent)
            ButtonVariant.OUTLINE -> Triple(c.surface, c.ink, c.line)
            ButtonVariant.GHOST -> Triple(Color.Transparent, c.btnPrimary, Color.Transparent)
            ButtonVariant.DANGER -> Triple(c.danger, c.surface, Color.Transparent)
        }
    val height =
        when (size) {
            ControlSize.SM -> Size.controlSm
            ControlSize.MD -> Size.controlMd
            ControlSize.LG -> Size.controlLg
        }
    val shape = RoundedCornerShape(Radius.lg)
    Row(
        modifier =
            modifier
                .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
                .sizeIn(minWidth = Size.touch, minHeight = Size.touch)
                .height(height)
                .clip(shape)
                .background(bg, shape)
                .then(if (line == Color.Transparent) Modifier else Modifier.border(Size.border, line, shape))
                .alpha(if (enabled) 1f else Opacity.disabled)
                .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick)
                .padding(horizontal = Spacing.s4),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(color = fg, strokeWidth = Spacing.s0_5, modifier = Modifier.size(Size.icon))
        } else if (icon != null) {
            Icon(icon, tint = fg, size = Size.iconSm)
        }
        Text(text, style = TutoraTheme.type.button, color = fg)
    }
}

enum class IconButtonVariant { SUNKEN, PLAIN, ON_PRIMARY, ACCENT }

/** Icon-only action; the label is read aloud since nothing else names it. */
@Composable
fun IconButton(
    icon: IconName,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: IconButtonVariant = IconButtonVariant.SUNKEN,
    enabled: Boolean = true,
) {
    val c = TutoraTheme.colors
    val (bg, fg) =
        when (variant) {
            IconButtonVariant.SUNKEN -> c.surfaceSunken to c.ink
            IconButtonVariant.PLAIN -> Color.Transparent to c.ink
            IconButtonVariant.ON_PRIMARY -> c.onPrimary.copy(alpha = 0.15f) to c.onPrimary
            IconButtonVariant.ACCENT -> c.accent to c.onAccent
        }
    Box(
        modifier =
            modifier
                .size(Size.touch)
                .clip(CircleShape)
                .background(bg, CircleShape)
                .alpha(if (enabled) 1f else Opacity.disabled)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, tint = fg)
    }
}

/** The floating + on Agenda: the only thing in the app that floats. */
@Composable
fun Fab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: IconName = IconName.PLUS,
    label: String = stringResource(R.string.ds_new_lesson),
) {
    val shape = RoundedCornerShape(Radius.x2xl)
    Box(
        modifier =
            modifier
                .size(Size.fab)
                .shadow(Spacing.s2, shape)
                .clip(shape)
                .background(TutoraTheme.colors.accent, shape)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, tint = TutoraTheme.colors.onAccent, size = Size.iconLg)
    }
}
