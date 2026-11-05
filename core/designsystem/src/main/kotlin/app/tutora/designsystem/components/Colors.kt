package app.tutora.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import app.tutora.designsystem.TutoraTheme
import app.tutora.designsystem.tokens.TutoraTokens.Radius
import app.tutora.designsystem.tokens.TutoraTokens.Size
import app.tutora.designsystem.tokens.TutoraTokens.Spacing

// Choosing colors (spec 013): a swatch, a theme's card and a slider whose track shows its range.

enum class SwatchKind { COLOR, THEME, ADD }

/**
 * One color to pick, read aloud by [label]. THEME is "Do tema" (the theme's own color, half
 * outlined); ADD is the + that opens Suas cores.
 */
@Composable
fun Swatch(
    color: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: SwatchKind = SwatchKind.COLOR,
) {
    val c = TutoraTheme.colors
    Box(
        modifier
            .size(Size.touch)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = label
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(Size.controlSm)
                .border(if (selected) Size.borderStrong else Size.border, if (selected) c.ink else c.line, CircleShape)
                .padding(Size.borderStrong)
                .clip(CircleShape)
                .background(
                    when (kind) {
                        SwatchKind.COLOR -> Brush.linearGradient(listOf(color, color))
                        SwatchKind.THEME -> Brush.linearGradient(0.5f to color, 0.5f to c.surface)
                        SwatchKind.ADD -> Brush.linearGradient(listOf(c.surfaceSunken, c.surfaceSunken))
                    },
                ),
            contentAlignment = Alignment.Center,
        ) { if (kind == SwatchKind.ADD) Icon(IconName.PLUS, tint = c.ink, size = Size.iconSm) }
    }
}

/** A theme in Aparência's list: its name and its main and highlight colors; outlined when chosen. */
@Composable
fun ThemeCard(
    name: String,
    main: Color,
    highlight: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = TutoraTheme.colors
    val shape = RoundedCornerShape(Radius.lg)
    Column(
        modifier
            .clip(shape)
            .background(c.surface, shape)
            .border(if (selected) Size.borderStrong else Size.border, if (selected) c.btnPrimary else c.line, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(Spacing.s3),
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            listOf(main, highlight).forEach { Box(Modifier.size(Size.dot * 2).clip(CircleShape).background(it)) }
        }
        Text(name, style = TutoraTheme.type.bodyStrong, color = c.ink, maxLines = 1)
    }
}

/** Matiz, Intensidade or Brilho: [track] is the range of colors along it, [value] in [range]. */
@Composable
fun ColorSlider(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    track: List<Color>,
    modifier: Modifier = Modifier,
    range: IntRange = 0..100,
) {
    val c = TutoraTheme.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = TutoraTheme.type.meta, color = c.inkMuted)
            Text(value.toString(), style = TutoraTheme.type.meta, color = c.ink)
        }
        Box(Modifier.fillMaxWidth().height(Size.touch), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(Size.progress)
                    .clip(RoundedCornerShape(Radius.full))
                    .background(Brush.horizontalGradient(track)),
            )
            Slider(
                value = value.toFloat(),
                onValueChange = { onChange(it.toInt()) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                colors =
                    SliderDefaults.colors(
                        thumbColor = c.surface,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent,
                    ),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            )
        }
    }
}
