package com.auracode.hiposim.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalExtraColors = staticCompositionLocalOf { HipoSimExtraColors() }

/** Access to the colors that live outside the Material 3 scheme. */
object HipoSimTheme {
    val extraColors: HipoSimExtraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtraColors.current
}

/** The mockups only define a light scheme, so dark mode is not implemented yet. */
@Composable
fun HipoSimTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalExtraColors provides HipoSimExtraColors()) {
        MaterialTheme(
            colorScheme = HipoSimLightColorScheme,
            typography = HipoSimTypography,
            shapes = HipoSimShapes,
            content = content,
        )
    }
}
