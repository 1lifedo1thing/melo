package `in`.melo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import `in`.melo.R

// Bundled fonts: no network request or Google Play Services needed for typography.
@OptIn(ExperimentalTextApi::class)
val MeloSans = FontFamily(
    *listOf(FontWeight.Light, FontWeight.Normal, FontWeight.Medium,
        FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold).map { weight ->
        Font(R.font.gflex_variable, weight = weight, variationSettings = FontVariation.Settings(
            FontVariation.weight(weight.weight), FontVariation.Setting("ROND", 0f)
        ))
    }.toTypedArray()
)

@OptIn(ExperimentalTextApi::class)
val DotMatrixFamily = FontFamily(Font(
    R.font.doto_variable, weight = FontWeight.Bold,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(700), FontVariation.Setting("ROND", 100f)
    )
))

// Compatibility aliases keep existing screens on the same type system.
val GoogleSansRounded = MeloSans
val MontserratFamily = MeloSans

private fun sans(size: Int, height: Int, weight: FontWeight = FontWeight.Normal, spacing: Float = 0f) = TextStyle(
    fontFamily = MeloSans, fontSize = size.sp, lineHeight = height.sp,
    fontWeight = weight, letterSpacing = spacing.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

val Typography = Typography(
    displayLarge = sans(56, 60, FontWeight.Light, -2f),
    displayMedium = sans(44, 50, FontWeight.Light, -1.5f),
    displaySmall = sans(36, 42, FontWeight.Normal, -1f),
    headlineLarge = sans(32, 38, FontWeight.Normal, -0.8f),
    headlineMedium = sans(28, 34, FontWeight.Normal, -0.5f),
    headlineSmall = sans(24, 30, FontWeight.Medium, -0.4f),
    titleLarge = sans(22, 28, FontWeight.Medium, -0.4f),
    titleMedium = sans(17, 24, FontWeight.Medium, -0.2f),
    titleSmall = sans(14, 20, FontWeight.Medium),
    bodyLarge = sans(16, 24),
    bodyMedium = sans(14, 21),
    bodySmall = sans(12, 18),
    labelLarge = sans(14, 20, FontWeight.Medium, 0.2f),
    labelMedium = sans(12, 18, FontWeight.Medium, 0.5f),
    labelSmall = sans(11, 16, FontWeight.Medium, 1f),
)

val ExpTitleTypography = Typography.copy(
    displayLarge = Typography.displayLarge,
    displayMedium = Typography.displayMedium,
    titleMedium = Typography.headlineLarge,
)
