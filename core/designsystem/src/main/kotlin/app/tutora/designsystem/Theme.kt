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
private val LocalLandscape = staticCompositionLocalOf { false }

/** Below this height a wide window is a phone turned sideways; taller ones are tablets (out of spec 017). */
private const val PHONE_MAX_HEIGHT_DP = 600

/** A phone turned sideways: wider than tall and shorter than 600 dp (spec 017). */
@Composable
fun isLandscapePhone(): Boolean {
    val size = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
    val height = with(androidx.compose.ui.platform.LocalDensity.current) { size.height.toDp() }
    return size.width > size.height && height.value < PHONE_MAX_HEIGHT_DP
}

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
    /** Tamanho do texto's factor; overrides [largeText] when given (AC-013-10). */
    textScale: Float? = null,
    /** Tamanho da interface: everything drawn grows or shrinks with it (AC-013-21). */
    uiScale: Float = 1f,
    reduceMotion: Boolean = systemReduceMotion(),
    dark: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val scheme = if (dark != null) (if (dark) Scheme.DARK else Scheme.LIGHT) else resolveScheme(mode, isSystemInDarkTheme())
    val colors = remember(themeId, scheme, custom) { TutoraColors.fromPalette(resolvePalette(themeId, scheme, custom)) }
    val factor = textScale ?: if (largeText) LARGE_TEXT_SCALE else 1f
    val type = remember(factor) { TutoraTypography(factor) }
    val landscape = isLandscapePhone()
    val phone = androidx.compose.ui.platform.LocalDensity.current
    val density =
        remember(phone, uiScale) {
            androidx.compose.ui.unit
                .Density(phone.density * uiScale, phone.fontScale)
        }
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
        androidx.compose.ui.platform.LocalDensity provides density,
        LocalTutoraColors provides colors,
        LocalTutoraType provides type,
        LocalReduceMotion provides reduceMotion,
        LocalScheme provides scheme,
        LocalLandscape provides landscape,
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

    /** The phone is sideways: screens branch on this and nothing else measures the screen (spec 017). */
    val landscape: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalLandscape.current
}

/** The motion every component uses: a short tween, or none when the phone asks (AC-002-17). */
fun <T> motionSpec(reduceMotion: Boolean): AnimationSpec<T> = if (reduceMotion) snap() else tween(durationMillis = 200)
