package app.tutora.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.tokens.TutoraTokens.Radius
import app.tutora.designsystem.tokens.TutoraTokens.Size
import app.tutora.designsystem.tokens.TutoraTokens.Spacing
import app.tutora.domain.Cents

// Spec 016: each screen's main numbers sit in one card painted with the Topo color (the
// "header" part of Suas cores), now that no screen has a colored band at the top.

/**
 * The Topo-colored card that holds a screen's main summary: today's earnings, the period on
 * Ganhos, a balance. Text on it uses the on-header colors; money uses earnings-on-header.
 */
@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    label: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = TutoraTheme.colors
    val shape = RoundedCornerShape(Radius.x3xl)
    Column(
        Modifier
            .clip(shape)
            .then(modifier)
            .fillMaxWidth()
            .testTag("summary-card")
            .background(c.header, shape)
            .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier)
            .padding(Spacing.s4),
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        content = content,
    )
}

/**
 * The fill of a tile or control on a [SummaryCard]: the Topo color moved [ON_HEADER_FILL] away
 * from its text (darker under light text, lighter under dark), so the text only gains contrast.
 */
@Composable
@ReadOnlyComposable
fun onHeaderFill(): Color {
    val c = TutoraTheme.colors
    // Toward the theme's darkest color under light text, its lightest under dark text.
    val (dark, light) = listOf(c.ink, c.background).sortedBy { it.luminance() }
    val away = if (c.onHeader.luminance() > 0.5f) dark else light
    val t = ON_HEADER_FILL
    return Color(
        red = c.header.red + (away.red - c.header.red) * t,
        green = c.header.green + (away.green - c.header.green) * t,
        blue = c.header.blue + (away.blue - c.header.blue) * t,
    )
}

/** PLAIN is a figure on the card itself; OWED and PAID keep their red and green in every theme. */
enum class SummaryTone { PLAIN, OWED, PAID }

/** A small figure on a [SummaryCard]: "Semana R$ 1.180", "Deve R$ 240". */
@Composable
fun RowScope.SummaryTile(
    label: String,
    cents: Cents,
    tone: SummaryTone = SummaryTone.PLAIN,
    onClick: (() -> Unit)? = null,
    compact: Boolean = true,
) {
    val c = TutoraTheme.colors
    val (bg, fg, sub) =
        when (tone) {
            SummaryTone.PLAIN -> Triple(onHeaderFill(), c.onHeader, c.onHeader)
            SummaryTone.OWED -> Triple(c.dangerSoft, c.dangerInk, c.dangerInk)
            SummaryTone.PAID -> Triple(c.successSoft, c.successInk, c.successInk)
        }
    val text = formatMoney(cents, currentLanguage(), compact)
    Column(
        Modifier
            .weight(1f)
            .testTag("summary-tile-${tone.name.lowercase()}")
            .clip(RoundedCornerShape(Radius.xl))
            .background(bg)
            .then(
                if (onClick != null) {
                    Modifier.sizeIn(minHeight = Size.touch).clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            ).semantics(mergeDescendants = true) {}
            .padding(horizontal = Spacing.s3, vertical = Spacing.s2_5),
    ) {
        Text(label, style = TutoraTheme.type.meta, color = sub, maxLines = 1)
        Text(text, style = TutoraTheme.type.moneyMd, color = fg, maxLines = 1)
    }
}

/**
 * Two actions that belong together, joined: [first] on a soft fill, [second] the main one in
 * the Botões principais color. On a [SummaryCard], [onSummary] tints the first with on-header.
 */
@Composable
fun SplitButtons(
    first: String,
    onFirst: () -> Unit,
    second: String,
    onSecond: () -> Unit,
    modifier: Modifier = Modifier,
    firstIcon: IconName? = null,
    secondIcon: IconName? = null,
    onSummary: Boolean = false,
) {
    val c = TutoraTheme.colors
    val outer = Radius.x3xl
    val inner = Radius.xs
    val (firstBg, firstFg) = if (onSummary) onHeaderFill() to c.onHeader else c.primarySoft to c.ink

    @Composable
    fun RowScope.Half(
        text: String,
        icon: IconName?,
        bg: Color,
        fg: Color,
        shape: RoundedCornerShape,
        tag: String,
        onClick: () -> Unit,
    ) {
        Row(
            Modifier
                .weight(1f)
                .sizeIn(minHeight = Size.controlLg)
                .testTag(tag)
                .clip(shape)
                .background(bg)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = Spacing.s3, vertical = Spacing.s2),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s1_5, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Icon(icon, tint = fg, size = Size.iconSm)
            Text(text, style = TutoraTheme.type.button, color = fg)
        }
    }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s1)) {
        Half(
            first,
            firstIcon,
            firstBg,
            firstFg,
            RoundedCornerShape(topStart = outer, bottomStart = outer, topEnd = inner, bottomEnd = inner),
            "split-first",
            onFirst,
        )
        Half(
            second,
            secondIcon,
            c.btnPrimary,
            c.onBtnPrimary,
            RoundedCornerShape(topStart = inner, bottomStart = inner, topEnd = outer, bottomEnd = outer),
            "split-second",
            onSecond,
        )
    }
}
