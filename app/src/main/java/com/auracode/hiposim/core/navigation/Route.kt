package com.auracode.hiposim.core.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation routes. */
@Serializable
sealed interface Route {
    /** Home: the simulation form. It works without an account. */
    @Serializable
    data object Simulation : Route

    @Serializable
    data object Results : Route

    @Serializable
    data object Register : Route
}
