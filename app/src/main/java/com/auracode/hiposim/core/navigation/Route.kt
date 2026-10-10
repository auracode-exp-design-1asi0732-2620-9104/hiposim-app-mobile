package com.auracode.hiposim.core.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation routes. */
@Serializable
sealed interface Route {
    /** Start destination: the buyer enters the home price, down payment and term. */
    @Serializable
    data object SimulationForm : Route

    /** Shows the current simulation of the session, calculated from the form. */
    @Serializable
    data object Results : Route

    @Serializable
    data object Register : Route
}
