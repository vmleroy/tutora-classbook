package app.tutora.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
    val (base, fgBase, line) =
        when (variant) {
            ButtonVariant.PRIMARY -> Triple(c.btnPrimary, c.onBtnPrimary, Color.Transparent)
            ButtonVariant.ACCENT -> Triple(c.accent, c.onAccent, Color.Transparent)
            ButtonVariant.OUTLINE -> Triple(c.surface, c.ink, c.line)
            ButtonVariant.GHOST -> Triple(Color.Transparent, c.btnPrimary, Color.Transparent)
            ButtonVariant.DANGER -> Triple(c.danger, c.surface, Color.Transparent)
        }
    // Disabled dims the colors themselves, not a layer: a layer switched back to full opacity
    // inside a bottom sheet was left undrawn on a device.
    val dim = if (enabled) 1f else Opacity.disabled
    val bg = base.copy(alpha = base.alpha * dim)
    val fg = fgBase.copy(alpha = fgBase.alpha * dim)
    val height =
        when (size) {
            ControlSize.SM -> Size.buttonSm
            ControlSize.MD -> Size.buttonMd
            ControlSize.LG -> Size.buttonLg
        }
    // Buttons are pills (spec 016).
    val shape = RoundedCornerShape(Radius.full)
    // Pressed on the full touch size, drawn at [height] inside it (AC-002-12; Victor, 4 Nov 2026):
    // the press area comes before the vertical padding, the pill and its ripple after it.
    val press =
        remember {
            androidx.compose.foundation.interaction
                .MutableInteractionSource()
        }
    Row(
        modifier =
            modifier
                .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
                .sizeIn(minWidth = Size.touch)
                .height(maxOf(height, Size.touch))
                .clickable(
                    interactionSource = press,
                    indication = null,
                    enabled = enabled && !loading,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(vertical = (Size.touch - height).coerceAtLeast(androidx.compose.ui.unit.Dp.Hairline) / 2)
                .clip(shape)
                .background(bg, shape)
                .then(if (line == Color.Transparent) Modifier else Modifier.border(Size.border, line.copy(alpha = line.alpha * dim), shape))
                .indication(press, androidx.compose.material3.ripple())
                .padding(horizontal = Spacing.s4),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(color = fg, strokeWidth = Spacing.s0_5, modifier = Modifier.size(Size.icon))
        } else if (icon != null) {
            Icon(icon, tint = fg, size = Size.iconSm)
        }
        Text(
            text,
            style =
                if (size ==
                    ControlSize.SM
                ) {
                    TutoraTheme.type.button.copy(fontSize = TutoraTheme.type.caption.fontSize)
                } else {
                    TutoraTheme.type.button
                },
            color = fg,
            // Centered, also when a long label wraps (Victor, 4 Nov 2026).
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

enum class IconButtonVariant { SUNKEN, PLAIN, ON_PRIMARY, ACCENT, ACCENT_SQUARE }

/**
 * Icon-only action; the label is read aloud since nothing else names it. Round, except
 * ACCENT_SQUARE: an add button in a tab header, a 56 dp rounded square in the highlight color
 * (spec 016).
 */
@Composable
fun IconButton(
    icon: IconName,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: IconButtonVariant = IconButtonVariant.SUNKEN,
    enabled: Boolean = true,
    /** A small red dot at the top end: something there needs a look (the bell, AC-014-32). */
    dot: Boolean = false,
    /** SM draws a 36 dp circle with a smaller icon, for headers; it still answers on 48 dp. */
    size: ControlSize = ControlSize.MD,
    /** A number at the top end, such as the bell's unseen notifications (AC-014-40); 0 shows none. */
    count: Int = 0,
) {
    val c = TutoraTheme.colors
    val (bg, fg) =
        when (variant) {
            IconButtonVariant.SUNKEN -> c.surfaceSunken to c.ink
            IconButtonVariant.PLAIN -> Color.Transparent to c.ink
            IconButtonVariant.ON_PRIMARY -> onHeaderFill() to c.onHeader
            IconButtonVariant.ACCENT, IconButtonVariant.ACCENT_SQUARE -> c.accent to c.onAccent
        }
    val square = variant == IconButtonVariant.ACCENT_SQUARE
    val small = size == ControlSize.SM && !square
    val shape = if (square) RoundedCornerShape(Radius.x2xl) else CircleShape
    // The press area is always at least the touch size; the drawn circle may be smaller.
    Box(
        modifier =
            modifier
                .size(if (square) Size.fab else Size.touch)
                .alpha(if (enabled) 1f else Opacity.disabled)
                // Only the drawn circle is clipped, so a count in the corner shows whole.
                .then(if (count > 0) Modifier else Modifier.clip(shape))
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = if (count > 0) "$label, $count" else label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(
                    if (square) {
                        Size.fab
                    } else if (small) {
                        Size.controlSm
                    } else {
                        Size.touch
                    },
                ).clip(shape)
                .background(bg, shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                tint = fg,
                size =
                    if (square) {
                        Size.iconLg
                    } else if (small) {
                        Size.iconSm
                    } else {
                        Size.icon
                    },
            )
        }
        if (count > 0) {
            Text(
                if (count > MAX_COUNT) "$MAX_COUNT+" else count.toString(),
                style = TutoraTheme.type.badge.copy(fontFeatureSettings = "tnum"),
                color = c.surface,
                maxLines = 1,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .testTag("count")
                        .clip(RoundedCornerShape(Radius.full))
                        .background(c.danger)
                        .padding(horizontal = Spacing.s1),
            )
        } else if (dot) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(if (small) Spacing.s1_5 else Spacing.s2_5)
                    .size(Size.dot)
                    .clip(CircleShape)
                    .background(c.danger)
                    .testTag("dot"),
            )
        }
    }
}

private const val MAX_COUNT = 99

/** How far a tile or control on the summary card moves from the Topo color, away from its text. */
const val ON_HEADER_FILL = 0.14f

/**
 * The floating + on the Agenda: the only thing in the app that floats. Small, in the bottom-right
 * corner, its fill a little see-through so the classes behind it show (Victor, 29 Oct 2026).
 */
@Composable
fun Fab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: IconName = IconName.PLUS,
    label: String = stringResource(R.string.ds_new_lesson),
) {
    val shape = RoundedCornerShape(Radius.xl)
    Box(
        modifier =
            modifier
                .size(Size.fab)
                .clip(shape)
                .background(TutoraTheme.colors.accent.copy(alpha = Opacity.floating), shape)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, tint = TutoraTheme.colors.onAccent, size = Size.icon)
    }
}
