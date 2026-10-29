package app.tutora.designsystem.components

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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import app.tutora.designsystem.R
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.tokens.TutoraTokens.Radius
import app.tutora.designsystem.tokens.TutoraTokens.Size
import app.tutora.designsystem.tokens.TutoraTokens.Spacing

/**
 * A section title with an optional aside ("4 aulas", "Ver todas"), tappable with [onAside], and
 * an optional small icon action at the end ("+" to add to the section).
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    aside: String? = null,
    asidePrimary: Boolean = false,
    required: Boolean = false,
    onAside: (() -> Unit)? = null,
    action: IconName? = null,
    actionLabel: String = "",
    onAction: () -> Unit = {},
) {
    val c = TutoraTheme.colors
    // The title wraps; the aside keeps its own width on one line, never squeezed into a column.
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalAlignment = if (action != null || onAside != null) Alignment.CenterVertically else Alignment.Bottom,
    ) {
        Text(
            buildAnnotatedString {
                append(title)
                if (required) withStyle(SpanStyle(color = c.dangerInk)) { append(" *") }
            },
            style = TutoraTheme.type.section,
            color = c.ink,
            modifier = Modifier.weight(1f),
        )
        if (aside != null) {
            Text(
                aside,
                style = TutoraTheme.type.caption,
                color = if (asidePrimary || onAside != null) c.btnPrimary else c.inkMuted,
                maxLines = 1,
                softWrap = false,
                modifier =
                    if (onAside != null) {
                        Modifier
                            .sizeIn(
                                minWidth = Size.touch,
                                minHeight = Size.touch,
                            ).clickable(role = Role.Button, onClick = onAside)
                            .wrapContentHeight()
                    } else {
                        Modifier
                    },
            )
        }
        if (action != null) IconButton(action, actionLabel, onAction, variant = IconButtonVariant.PLAIN)
    }
}

/** One lesson in a day list: time, tag, who and where, and its state or value; lists put it in a [ListGroup]. */
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
    statusIcon: IconName? = null,
) {
    val c = TutoraTheme.colors
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .sizeIn(minHeight = Size.controlLg)
                .background(c.surface)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(horizontal = Spacing.s4, vertical = Spacing.s3),
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
            status != null -> Badge(status.second, tone = status.first, icon = statusIcon)
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

/** A titled card of rows separated by hairlines: every list in the app (spec 016). */
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
            Text(title, style = TutoraTheme.type.section, color = c.ink, modifier = Modifier.padding(horizontal = Spacing.s1))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .testTag("list-group")
                .clip(RoundedCornerShape(Radius.x3xl))
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

/** REMOVE is a × that takes the row's item out of a list of connections. */
enum class RowTrailing { CHEVRON, SWITCH, NONE, REMOVE }

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
    onRemove: () -> Unit = {},
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
                    .clip(RoundedCornerShape(Radius.lg))
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
            RowTrailing.REMOVE ->
                IconButton(IconName.CLOSE, stringResource(R.string.ds_remove, title), onRemove, variant = IconButtonVariant.PLAIN)
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
