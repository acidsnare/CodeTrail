package codetrail.app.ui

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import codetrail.app.res.Res
import codetrail.app.res.nunito
import org.jetbrains.compose.resources.Font

/**
 * Bundled UI font so text metrics are identical on macOS, Windows and Linux.
 * Nunito is a variable font: one file, weights selected through the wght axis.
 */
@Composable
fun appFontFamily(): FontFamily {
    val weights = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold, FontWeight.Black)
    return FontFamily(
        weights.map { w ->
            Font(Res.font.nunito, weight = w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))
        },
    )
}

@Composable
fun appTypography(): Typography {
    val family = appFontFamily()
    val base = Typography()
    fun TextStyle.withFont() = copy(fontFamily = family)
    return Typography(
        displayLarge = base.displayLarge.withFont(),
        displayMedium = base.displayMedium.withFont(),
        displaySmall = base.displaySmall.withFont(),
        headlineLarge = base.headlineLarge.withFont(),
        headlineMedium = base.headlineMedium.withFont(),
        headlineSmall = base.headlineSmall.withFont(),
        titleLarge = base.titleLarge.withFont(),
        titleMedium = base.titleMedium.withFont(),
        titleSmall = base.titleSmall.withFont(),
        bodyLarge = base.bodyLarge.withFont(),
        bodyMedium = base.bodyMedium.withFont(),
        bodySmall = base.bodySmall.withFont(),
        labelLarge = base.labelLarge.withFont(),
        labelMedium = base.labelMedium.withFont(),
        labelSmall = base.labelSmall.withFont(),
    )
}
