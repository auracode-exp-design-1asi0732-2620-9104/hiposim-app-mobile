package com.auracode.hiposim.feature.simulation.ui

import androidx.compose.runtime.Immutable
import java.time.LocalDate

/** The full payment schedule of the current simulation. Money is already formatted (es-PE). */
@Immutable
data class ScheduleUiState(
    /** True only until the current simulation of the session has been read. */
    val isLoading: Boolean = true,
    /** One row per month, in order. Empty while loading or when [isSimulationMissing]. */
    val rows: List<ScheduleRowUi> = emptyList(),
    val totalPaid: String = "",
    val totalInterest: String = "",
    /** There is no simulation to show, for example after the system recreated the app. */
    val isSimulationMissing: Boolean = false,
)

/** One month of the schedule: what is owed at the start, how the payment splits and what is left. */
@Immutable
data class ScheduleRowUi(
    val number: Int,
    /** Formatted by the screen with the language of the device. */
    val paymentDate: LocalDate,
    val openingBalance: String,
    /** Total paid in the month, insurance included. */
    val payment: String,
    val interest: String,
    val principal: String,
    val insurance: String,
    /** False when the simulation has no insurance, so the row does not show a line of zeros. */
    val hasInsurance: Boolean,
    val closingBalance: String,
    val isGracePeriod: Boolean,
)
