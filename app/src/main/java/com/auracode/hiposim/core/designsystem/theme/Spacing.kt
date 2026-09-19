package com.auracode.hiposim.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale. Everything is a multiple of 8 dp except [xs], a half step for icon and text gaps. */
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp

    /** Minimum touch target size. */
    val minTouchTarget: Dp = 48.dp
}
