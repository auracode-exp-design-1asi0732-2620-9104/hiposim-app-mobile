package com.auracode.hiposim.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.auracode.hiposim.R

/** Top-level destinations of the bottom bar. Those that [requiresAccount] show a padlock for guests. */
enum class MainDestination(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val requiresAccount: Boolean,
) {
    Simulate(R.string.nav_simulate, Icons.Outlined.Calculate, requiresAccount = false),
    Realtors(R.string.nav_realtors, Icons.Outlined.Apartment, requiresAccount = true),
    History(R.string.nav_history, Icons.Outlined.History, requiresAccount = true),
    Profile(R.string.nav_profile, Icons.Outlined.Person, requiresAccount = true),
}
