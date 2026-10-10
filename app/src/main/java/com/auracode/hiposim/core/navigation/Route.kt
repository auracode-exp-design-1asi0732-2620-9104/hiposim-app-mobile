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

    /** Every installment of the current simulation (US02). */
    @Serializable
    data object Schedule : Route

    /** Creates a buyer account. Reached from Results, where the buyer already has a simulation. */
    @Serializable
    data object Register : Route

    /** Signs in with an existing buyer account. */
    @Serializable
    data object Login : Route
}
