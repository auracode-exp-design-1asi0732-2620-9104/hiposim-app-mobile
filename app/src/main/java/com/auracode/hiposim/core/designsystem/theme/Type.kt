package com.auracode.hiposim.core.designsystem.theme

import androidx.annotation.FontRes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.auracode.hiposim.R

@OptIn(ExperimentalTextApi::class)
private fun variableFont(
    @FontRes resId: Int,
    weight: FontWeight,
) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Titles and figures. Variable font, weights 500 to 700. */
val Outfit =
    FontFamily(
        variableFont(R.font.outfit_variable, FontWeight.Medium),
        variableFont(R.font.outfit_variable, FontWeight.SemiBold),
        variableFont(R.font.outfit_variable, FontWeight.Bold),
    )

/** Body text. Variable font, weights 400 to 600. */
val RobotoFlex =
    FontFamily(
        variableFont(R.font.roboto_flex_variable, FontWeight.Normal),
        variableFont(R.font.roboto_flex_variable, FontWeight.Medium),
        variableFont(R.font.roboto_flex_variable, FontWeight.SemiBold),
    )

private fun style(
    family: FontFamily,
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp,
)

/** Scale from the mockups (mobile variants for the large styles). */
val HipoSimTypography =
    Typography(
        displayLarge = style(Outfit, FontWeight.Bold, size = 32, lineHeight = 40),
        headlineLarge = style(Outfit, FontWeight.SemiBold, size = 26, lineHeight = 34),
        headlineMedium = style(Outfit, FontWeight.SemiBold, size = 24, lineHeight = 32),
        headlineSmall = style(Outfit, FontWeight.SemiBold, size = 20, lineHeight = 28),
        titleLarge = style(Outfit, FontWeight.SemiBold, size = 20, lineHeight = 28),
        titleMedium = style(RobotoFlex, FontWeight.Medium, size = 16, lineHeight = 24),
        titleSmall = style(RobotoFlex, FontWeight.Medium, size = 14, lineHeight = 20),
        bodyLarge = style(RobotoFlex, FontWeight.Normal, size = 18, lineHeight = 28),
        bodyMedium = style(RobotoFlex, FontWeight.Normal, size = 16, lineHeight = 24),
        bodySmall = style(RobotoFlex, FontWeight.Normal, size = 14, lineHeight = 20),
        labelLarge = style(RobotoFlex, FontWeight.Medium, size = 16, lineHeight = 24),
        labelMedium = style(RobotoFlex, FontWeight.Medium, size = 14, lineHeight = 20),
        labelSmall = style(RobotoFlex, FontWeight.Medium, size = 12, lineHeight = 16),
    )

/** Big figure style (`metric-display-mobile` in the mockups). */
val MetricDisplayStyle: TextStyle = style(Outfit, FontWeight.Bold, size = 28, lineHeight = 36)
