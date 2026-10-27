package com.vmleroy.tutora.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.vmleroy.tutora.designsystem.tokens.TutoraColors
import com.vmleroy.tutora.designsystem.tokens.TutoraTokens

private val LocalTutoraColors = staticCompositionLocalOf<TutoraColors> { error("TutoraTheme is missing") }

/**
 * The Tutora theme: the palette of [themeId] in light or dark, following the phone unless
 * told otherwise. Spec 002 adds the other themes' pickers and custom colors.
 */
@Composable
fun TutoraTheme(
    themeId: String = "quadro",
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = TutoraTokens.colorSets.getValue("$themeId-${if (dark) "dark" else "light"}")
    // Material components read their ColorScheme; fill it from the same palette.
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme =
        base.copy(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.primarySoft,
            secondary = colors.secondary,
            secondaryContainer = colors.primarySoft,
            onSecondaryContainer = colors.primary,
            background = colors.background,
            onBackground = colors.ink,
            surface = colors.surface,
            onSurface = colors.ink,
            onSurfaceVariant = colors.inkMuted,
            surfaceContainer = colors.surface,
            surfaceVariant = colors.surfaceSunken,
            outline = colors.line,
            outlineVariant = colors.lineSoft,
            error = colors.danger,
            scrim = colors.scrim,
        )
    CompositionLocalProvider(LocalTutoraColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = TutoraTypography.material, content = content)
    }
}

object TutoraTheme {
    val colors: TutoraColors
        @Composable @ReadOnlyComposable
        get() = LocalTutoraColors.current

    val type: TutoraTypography get() = TutoraTypography
}
