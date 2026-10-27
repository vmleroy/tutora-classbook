package com.vmleroy.tutora.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.vmleroy.tutora.designsystem.R
import com.vmleroy.tutora.designsystem.TutoraTheme
import com.vmleroy.tutora.designsystem.tokens.TutoraTokens
import com.vmleroy.tutora.domain.Cents
import com.vmleroy.tutora.domain.Language
import com.vmleroy.tutora.domain.wholeReais
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** The icon set: Lucide line icons on a 24-unit grid (docs/design-system, Icon). */
enum class IconName(
    @param:DrawableRes val res: Int,
) {
    PLUS(R.drawable.ic_plus),
    MINUS(R.drawable.ic_minus),
    CHECK(R.drawable.ic_check),
    CLOSE(R.drawable.ic_close),
    CHEVRON_LEFT(R.drawable.ic_chevron_left),
    CHEVRON_RIGHT(R.drawable.ic_chevron_right),
    HOME(R.drawable.ic_home),
    CALENDAR(R.drawable.ic_calendar),
    USERS(R.drawable.ic_users),
    PIN(R.drawable.ic_pin),
    WALLET(R.drawable.ic_wallet),
    CLOCK(R.drawable.ic_clock),
    ALERT(R.drawable.ic_alert),
    FILE(R.drawable.ic_file),
    CLOUD(R.drawable.ic_cloud),
    GEAR(R.drawable.ic_gear),
    CALENDAR_CHECK(R.drawable.ic_calendar_check),
    NO_SHOW(R.drawable.ic_no_show),
    PLAY(R.drawable.ic_play),
    STAR(R.drawable.ic_star),
    SEARCH(R.drawable.ic_search),
    ARCHIVE(R.drawable.ic_archive),
}

/** A line icon tinted with a token color; decorative unless it has a [label]. */
@Composable
fun Icon(
    name: IconName,
    modifier: Modifier = Modifier,
    size: Dp = TutoraTokens.Size.icon,
    tint: Color = TutoraTheme.colors.ink,
    label: String? = null,
) {
    Icon(painterResource(name.res), contentDescription = label, modifier = modifier.size(size), tint = tint)
}

/** The seven color tags for students and groups. */
enum class Tag { AMBER, GREEN, BLUE, VIOLET, ORANGE, RED, TEAL }

data class TagColors(
    val fill: Color,
    val soft: Color,
    val ink: Color,
)

@Composable
@ReadOnlyComposable
fun Tag.colors(): TagColors {
    val c = TutoraTheme.colors
    return when (this) {
        Tag.AMBER -> TagColors(c.tagAmber, c.tagAmberSoft, c.tagAmberInk)
        Tag.GREEN -> TagColors(c.tagGreen, c.tagGreenSoft, c.tagGreenInk)
        Tag.BLUE -> TagColors(c.tagBlue, c.tagBlueSoft, c.tagBlueInk)
        Tag.VIOLET -> TagColors(c.tagViolet, c.tagVioletSoft, c.tagVioletInk)
        Tag.ORANGE -> TagColors(c.tagOrange, c.tagOrangeSoft, c.tagOrangeInk)
        Tag.RED -> TagColors(c.tagRed, c.tagRedSoft, c.tagRedInk)
        Tag.TEAL -> TagColors(c.tagTeal, c.tagTealSoft, c.tagTealInk)
    }
}

private fun localeOf(language: Language): Locale = if (language == Language.PT_BR) Locale.forLanguageTag("pt-BR") else Locale.US

/**
 * Reais from integer cents for the app language: "R$ 1.234,56" / "R$1,234.56". The language
 * changes how the amount is written, never the amount (constitution rule 4). [compact] shows
 * whole reais, rounded in the domain.
 */
fun formatMoney(
    cents: Cents,
    language: Language,
    compact: Boolean = false,
): String {
    val format =
        NumberFormat.getCurrencyInstance(localeOf(language)).apply {
            currency = Currency.getInstance("BRL")
            minimumFractionDigits = if (compact) 0 else 2
            maximumFractionDigits = if (compact) 0 else 2
        }
    // Integer cents divided by 100 have at most two decimals, so the format is exact.
    return if (compact) format.format(wholeReais(cents)) else format.format(java.math.BigDecimal.valueOf(cents, 2))
}

/** The app language from the current configuration. */
@Composable
@ReadOnlyComposable
fun currentLanguage(): Language {
    val locale =
        androidx.compose.ui.platform.LocalConfiguration.current.locales
            .get(0)
    return if (locale?.language == "pt") Language.PT_BR else Language.EN
}
