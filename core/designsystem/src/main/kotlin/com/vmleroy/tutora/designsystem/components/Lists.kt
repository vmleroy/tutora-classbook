package com.vmleroy.tutora.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.vmleroy.tutora.designsystem.TutoraTheme
import com.vmleroy.tutora.designsystem.tokens.TutoraTokens.Radius
import com.vmleroy.tutora.designsystem.tokens.TutoraTokens.Size
import com.vmleroy.tutora.designsystem.tokens.TutoraTokens.Spacing

/** A section title with an optional aside ("4 aulas", "Ver todas"). */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    aside: String? = null,
    asidePrimary: Boolean = false,
) {
    val c = TutoraTheme.colors
    // The title wraps; the aside keeps its own width on one line, never squeezed into a column.
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalAlignment = Alignment.Bottom) {
        Text(title, style = TutoraTheme.type.section, color = c.ink, modifier = Modifier.weight(1f))
        if (aside != null) {
            Text(
                aside,
                style = TutoraTheme.type.caption,
                color = if (asidePrimary) c.btnPrimary else c.inkMuted,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/** One lesson in a day list: time, tag, who and where, and its state or value. */
@Composable
fun LessonRow(
    time: String,
    tag: Tag,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    status: Pair<BadgeTone, String>? = null,
    trailing: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = TutoraTheme.colors
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .sizeIn(minHeight = Size.controlLg)
                .clip(RoundedCornerShape(Radius.xl))
                .background(c.surface)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(horizontal = Spacing.s3_5, vertical = Spacing.s3),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Grows for "10:30 AM" and Large text; never wraps.
        Text(time, style = TutoraTheme.type.rowTitle.copy(fontFeatureSettings = "tnum"), color = c.inkMuted, maxLines = 1, softWrap = false)
        TagDot(tag)
        Column(Modifier.weight(1f)) {
            Text(title, style = TutoraTheme.type.rowTitle, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle !=
                null
            ) {
                Text(subtitle, style = TutoraTheme.type.meta, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        when {
            status != null -> Badge(status.second, tone = status.first)
            trailing != null ->
                Text(
                    trailing,
                    style = TutoraTheme.type.caption.copy(fontFeatureSettings = "tnum"),
                    color = c.inkMuted,
                    maxLines = 1,
                )
        }
    }
}

/** A titled white block of rows separated by hairlines, as in Ajustes. */
@Composable
fun ListGroup(
    rows: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val c = TutoraTheme.colors
    Column(
        modifier.then(
            if (title !=
                null
            ) {
                Modifier.semantics { contentDescription = title }
            } else {
                Modifier
            },
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.s1_5),
    ) {
        if (title !=
            null
        ) {
            Text(title, style = TutoraTheme.type.badge, color = c.inkMuted, modifier = Modifier.padding(horizontal = Spacing.s1))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.xl))
                .background(c.surface),
        ) {
            rows.forEachIndexed { i, row ->
                if (i > 0) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(Size.border)
                            .background(c.lineSoft),
                    )
                }
                row()
            }
        }
    }
}

enum class TileTone { SECONDARY, DANGER, SUCCESS, ACCENT, PRIMARY }

enum class RowTrailing { CHEVRON, SWITCH, NONE }

/** A settings or navigation row whose subtitle states the current value. */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: IconName? = null,
    iconTone: TileTone = TileTone.SECONDARY,
    avatar: Pair<String, Tag>? = null,
    trailing: RowTrailing = RowTrailing.CHEVRON,
    switchValue: Boolean = false,
    onSwitchChange: (Boolean) -> Unit = {},
    badge: Pair<BadgeTone, String>? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = TutoraTheme.colors
    val (tile, tileInk) =
        when (iconTone) {
            TileTone.SECONDARY -> c.secondarySoft to c.secondaryInk
            TileTone.DANGER -> c.dangerSoft to c.dangerInk
            TileTone.SUCCESS -> c.successSoft to c.successInk
            TileTone.ACCENT -> c.accentSoft to c.accentInk
            TileTone.PRIMARY -> c.primarySoft to c.primary
        }
    val pressable = trailing != RowTrailing.SWITCH && onClick != null
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .sizeIn(minHeight = Size.controlLg)
                .then(
                    if (pressable) {
                        Modifier
                            .clickable(role = Role.Button, onClick = onClick)
                            .semantics { contentDescription = listOfNotNull(title, subtitle).joinToString(", ") }
                    } else {
                        Modifier
                    },
                ).padding(horizontal = Spacing.s4, vertical = Spacing.s2_5),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (avatar != null) {
            Avatar(avatar.first, avatar.second, size = AvatarSize.SM)
        } else if (icon != null) {
            Box(
                Modifier
                    .size(Size.tile)
                    .clip(RoundedCornerShape(Radius.md))
                    .background(tile),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, tint = tileInk, size = Size.iconSm) }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = TutoraTheme.type.bodyStrong, color = c.ink)
            if (subtitle != null) Text(subtitle, style = TutoraTheme.type.meta, color = c.inkMuted)
        }
        if (badge != null) Badge(badge.second, tone = badge.first)
        when (trailing) {
            RowTrailing.CHEVRON -> Icon(IconName.CHEVRON_RIGHT, tint = c.inkMuted, size = Size.iconSm)
            RowTrailing.SWITCH -> Switch(switchValue, onSwitchChange, title)
            RowTrailing.NONE -> Unit
        }
    }
}

enum class AlertTone { DANGER, ACCENT, SUCCESS, SECONDARY }

/** One line that needs attention ("3 alunos com pagamento pendente"), with an optional value. */
@Composable
fun AlertBanner(
    message: String,
    modifier: Modifier = Modifier,
    tone: AlertTone = AlertTone.DANGER,
    icon: IconName = IconName.ALERT,
    value: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = TutoraTheme.colors
    val (bg, fg) =
        when (tone) {
            AlertTone.DANGER -> c.dangerSoft to c.dangerInk
            AlertTone.ACCENT -> c.accentSoft to c.accentInk
            AlertTone.SUCCESS -> c.successSoft to c.successInk
            AlertTone.SECONDARY -> c.secondarySoft to c.secondaryInk
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .sizeIn(minHeight = Size.touch)
                .clip(RoundedCornerShape(Radius.lg))
                .background(bg)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(horizontal = Spacing.s3_5, vertical = Spacing.s2_5),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2_5),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, tint = fg, size = Size.iconSm)
        Text(message, style = TutoraTheme.type.caption, color = fg, modifier = Modifier.weight(1f))
        if (value != null) Text(value, style = TutoraTheme.type.bodyStrong.copy(fontFeatureSettings = "tnum"), color = fg)
        if (onClick != null) Icon(IconName.CHEVRON_RIGHT, tint = fg, size = Size.iconSm)
    }
}
