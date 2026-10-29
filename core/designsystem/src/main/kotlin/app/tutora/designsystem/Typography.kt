package app.tutora.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.tutora.designsystem.tokens.TutoraTokens
import app.tutora.designsystem.tokens.TypeToken

private fun nunito(weight: Int) =
    Font(
        R.font.nunito,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
    )

/** Nunito 400–900 from the variable font (AC-001-11); headings and money are 900 (spec 016). */
val Nunito = FontFamily(nunito(400), nunito(600), nunito(700), nunito(800), nunito(900))

/** The type styles of the tokens, multiplied by the text scale (Large text, AC-002-16). */
class TutoraTypography(
    val scale: Float,
) {
    private fun style(
        t: TypeToken,
        tabular: Boolean = false,
    ) = TextStyle(
        fontFamily = Nunito,
        fontWeight = FontWeight(t.weight),
        fontSize = (t.size * scale).sp,
        lineHeight = (t.lineHeight * scale).sp,
        letterSpacing = t.letterSpacing.sp,
        // Money uses tabular figures so digits do not jitter (AC-001-11).
        fontFeatureSettings = if (tabular) "tnum" else null,
    )

    val moneyDisplay = style(TutoraTokens.Type.moneyDisplay, tabular = true)
    val moneyXl = style(TutoraTokens.Type.moneyXl, tabular = true)
    val moneyLg = style(TutoraTokens.Type.moneyLg, tabular = true)
    val moneyMd = style(TutoraTokens.Type.moneyMd, tabular = true)
    val titleXl = style(TutoraTokens.Type.titleXl)
    val titleLg = style(TutoraTokens.Type.titleLg)
    val titleMd = style(TutoraTokens.Type.titleMd)
    val section = style(TutoraTokens.Type.section)
    val bodyStrong = style(TutoraTokens.Type.bodyStrong)
    val body = style(TutoraTokens.Type.body)
    val rowTitle = style(TutoraTokens.Type.rowTitle)
    val caption = style(TutoraTokens.Type.caption)
    val meta = style(TutoraTokens.Type.meta)
    val button = style(TutoraTokens.Type.button)
    val fieldLabel = style(TutoraTokens.Type.fieldLabel)
    val badge = style(TutoraTokens.Type.badge)
    val overline = style(TutoraTokens.Type.overline)
    val tab = style(TutoraTokens.Type.tab)

    /** Material components get the same faces. */
    val material =
        Typography(
            headlineSmall = titleXl,
            titleLarge = titleLg,
            titleMedium = titleMd,
            bodyLarge = body,
            bodyMedium = body,
            bodySmall = caption,
            labelLarge = button,
            labelMedium = tab,
            labelSmall = meta,
        )
}
