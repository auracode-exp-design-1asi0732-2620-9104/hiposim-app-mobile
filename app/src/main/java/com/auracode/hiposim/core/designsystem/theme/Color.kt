package com.auracode.hiposim.core.designsystem.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand palette (docs/design/mockups).
val BrandTeal = Color(0xFF0E5C63)
val BrandTurquoise = Color(0xFF3AAFA9)
val BrandAmber = Color(0xFFF5A524)
val BrandAmberDark = Color(0xFFE0921B)
val BrandGray = Color(0xFF5F6B76)
val BrandBackground = Color(0xFFF7F9FF)
val BrandText = Color(0xFF1F2933)

/**
 * Colors outside the Material 3 scheme.
 *
 * [accent] (amber) is only a fill: text on it uses [onAccent]. Amber text on white fails WCAG AA,
 * so amounts that the mockups paint amber use [amount] instead.
 */
data class HipoSimExtraColors(
    val accent: Color = BrandAmber,
    val accentPressed: Color = BrandAmberDark,
    val onAccent: Color = Color(0xFF27313C),
    val turquoise: Color = BrandTurquoise,
    val amount: Color = Color(0xFF744A00),
    val insuranceSegment: Color = Color(0xFFFFB957),
)

val HipoSimLightColorScheme =
    lightColorScheme(
        primary = BrandTeal,
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = BrandTeal,
        onPrimaryContainer = Color(0xFF90D2DA),
        inversePrimary = Color(0xFF90D1D9),
        secondary = Color(0xFF006A66),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFF84F2EB),
        onSecondaryContainer = Color(0xFF006F6A),
        tertiary = Color(0xFF553500),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFF744A00),
        onTertiaryContainer = Color(0xFFFFBA59),
        background = BrandBackground,
        onBackground = BrandText,
        surface = BrandBackground,
        onSurface = BrandText,
        surfaceVariant = Color(0xFFD9E3F1),
        onSurfaceVariant = BrandGray,
        surfaceTint = Color(0xFF20676F),
        inverseSurface = Color(0xFF27313C),
        inverseOnSurface = Color(0xFFE8F2FF),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF93000A),
        outline = Color(0xFF6F797A),
        outlineVariant = Color(0xFFBFC8C9),
        surfaceBright = BrandBackground,
        surfaceDim = Color(0xFFD1DBE8),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFEDF4FF),
        surfaceContainer = Color(0xFFE4EFFD),
        surfaceContainerHigh = Color(0xFFDFE9F7),
        surfaceContainerHighest = Color(0xFFD9E3F1),
        primaryFixed = Color(0xFFACEEF6),
        primaryFixedDim = Color(0xFF90D1D9),
        onPrimaryFixed = Color(0xFF002023),
        onPrimaryFixedVariant = Color(0xFF004F55),
        secondaryFixed = Color(0xFF87F4EE),
        secondaryFixedDim = Color(0xFF69D8D1),
        onSecondaryFixed = Color(0xFF00201E),
        onSecondaryFixedVariant = Color(0xFF00504D),
        tertiaryFixed = Color(0xFFFFDDB5),
        tertiaryFixedDim = Color(0xFFFFB957),
        onTertiaryFixed = Color(0xFF2A1800),
        onTertiaryFixedVariant = Color(0xFF643F00),
    )
