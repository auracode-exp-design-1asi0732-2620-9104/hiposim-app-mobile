package com.auracode.hiposim.core.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation routes. */
@Serializable
sealed interface Route {
    @Serializable
    data object Results : Route

    @Serializable
    data object Register : Route
}
