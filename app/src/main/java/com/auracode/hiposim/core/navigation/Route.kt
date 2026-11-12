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

    /** The signed in buyer's saved scenarios (US06). */
    @Serializable
    data object Scenarios : Route

    /** Two or three saved scenarios side by side (US07). [ids] are the scenario ids joined by commas, in column order. */
    @Serializable
    data class CompareScenarios(
        val ids: String,
    ) : Route {
        companion object {
            /** Name of the argument in the route, which is also its key in the `SavedStateHandle` of the screen. */
            const val ARG_IDS = "ids"

            fun of(ids: List<String>) = CompareScenarios(ids.joinToString(SEPARATOR))

            fun splitIds(raw: String?): List<String> = raw.orEmpty().split(SEPARATOR).filter { it.isNotBlank() }

            private const val SEPARATOR = ","
        }
    }
}
