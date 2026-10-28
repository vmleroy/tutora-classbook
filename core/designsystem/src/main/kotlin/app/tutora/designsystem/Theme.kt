package app.tutora.designsystem

import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import app.tutora.designsystem.tokens.TutoraColors
import app.tutora.domain.ThemeMode
import app.tutora.domain.theme.CustomColors
import app.tutora.domain.theme.Scheme
import app.tutora.domain.theme.resolvePalette
import app.tutora.domain.theme.resolveScheme

private val LocalTutoraColors = staticCompositionLocalOf<TutoraColors> { error("TutoraTheme is missing") }
private val LocalTutoraType = staticCompositionLocalOf { TutoraTypography(1f) }
private val LocalReduceMotion = staticCompositionLocalOf { false }
private val LocalScheme = staticCompositionLocalOf { Scheme.LIGHT }

/** Large text scales every type style by 1.15; spacing never changes (spec 002). */
const val LARGE_TEXT_SCALE = 1.15f

/** Whether the phone asks for reduced motion (animator duration scale set to off). */
@Composable
fun systemReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * The Tutora theme: the palette of [themeId] in the chosen [mode], with the teacher's
 * [custom] colors, resolved by the domain (docs/design-system/Theming.md). Changing any
 * argument recomposes with the new palette at once (AC-002-5). [dark] forces a scheme, for
 * previews and tests.
 */
@Composable
fun TutoraTheme(
    themeId: String = "quadro",
    mode: ThemeMode = ThemeMode.SYSTEM,
    custom: CustomColors = emptyMap(),
    largeText: Boolean = false,
    reduceMotion: Boolean = systemReduceMotion(),
    dark: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val scheme = if (dark != null) (if (dark) Scheme.DARK else Scheme.LIGHT) else resolveScheme(mode, isSystemInDarkTheme())
    val colors = remember(themeId, scheme, custom) { TutoraColors.fromPalette(resolvePalette(themeId, scheme, custom)) }
    val type = remember(largeText) { TutoraTypography(if (largeText) LARGE_TEXT_SCALE else 1f) }
    // Material components read their ColorScheme; fill it from the same palette.
    val base = if (scheme == Scheme.DARK) darkColorScheme() else lightColorScheme()
    val material =
        base.copy(
            primary = colors.btnPrimary,
            onPrimary = colors.onBtnPrimary,
            primaryContainer = colors.primarySoft,
            secondary = colors.secondary,
            secondaryContainer = colors.tabActiveSoft,
            onSecondaryContainer = colors.tabActive,
            background = colors.background,
            onBackground = colors.ink,
            surface = colors.surface,
            onSurface = colors.ink,
            onSurfaceVariant = colors.inkMuted,
            surfaceContainer = colors.surface,
            surfaceContainerLow = colors.surface,
            surfaceContainerHigh = colors.surface,
            surfaceVariant = colors.surfaceSunken,
            outline = colors.line,
            outlineVariant = colors.lineSoft,
            error = colors.danger,
            scrim = colors.scrim,
        )
    CompositionLocalProvider(
        LocalTutoraColors provides colors,
        LocalTutoraType provides type,
        LocalReduceMotion provides reduceMotion,
        LocalScheme provides scheme,
    ) {
        MaterialTheme(colorScheme = material, typography = type.material, content = content)
    }
}

object TutoraTheme {
    val colors: TutoraColors
        @Composable @ReadOnlyComposable
        get() = LocalTutoraColors.current

    val type: TutoraTypography
        @Composable @ReadOnlyComposable
        get() = LocalTutoraType.current

    val scheme: Scheme
        @Composable @ReadOnlyComposable
        get() = LocalScheme.current

    val reduceMotion: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalReduceMotion.current
}

/** The motion every component uses: a short tween, or none when the phone asks (AC-002-17). */
fun <T> motionSpec(reduceMotion: Boolean): AnimationSpec<T> = if (reduceMotion) snap() else tween(durationMillis = 200)
