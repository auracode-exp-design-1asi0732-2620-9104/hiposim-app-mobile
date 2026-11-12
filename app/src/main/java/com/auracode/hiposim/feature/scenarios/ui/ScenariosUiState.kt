package com.auracode.hiposim.feature.scenarios.ui

import androidx.compose.runtime.Immutable
import com.auracode.hiposim.core.util.PeruFormat
import com.auracode.hiposim.feature.scenarios.domain.SavedScenario
import com.auracode.hiposim.feature.scenarios.domain.ScenarioFailure
import com.auracode.hiposim.feature.simulation.domain.GraceType
import com.auracode.hiposim.feature.simulation.domain.SimulationConfig
import java.time.Instant

/** The buyer's saved scenarios (US06). Money and rates are already formatted (es-PE). */
@Immutable
data class ScenariosUiState(
    val isLoading: Boolean = true,
    val scenarios: List<ScenarioUi> = emptyList(),
    /** Why the list could not be read; null when it was read (even if empty). */
    val failure: ScenarioFailure? = null,
) {
    val isEmpty: Boolean get() = !isLoading && failure == null && scenarios.isEmpty()
}

/** One saved scenario as a card. A figure is null when the API did not send it, and its line is not drawn. */
@Immutable
data class ScenarioUi(
    val id: String,
    val name: String,
    /** Formatted by the screen with the language of the device. */
    val savedAt: Instant,
    val monthlyPayment: String?,
    val tcea: String?,
    val totalPaid: String?,
    val propertyPrice: String?,
    val downPayment: String?,
    val termYears: Int?,
    val goodPayerBonus: String?,
    val graceType: GraceType,
    val graceMonths: Int,
)

fun SavedScenario.toUi(): ScenarioUi =
    ScenarioUi(
        id = id,
        name = name,
        savedAt = createdAt,
        monthlyPayment = monthlyPayment?.let(PeruFormat::soles),
        tcea = tcea?.let { PeruFormat.percent(it) },
        totalPaid = totalPaid?.let(PeruFormat::soles),
        propertyPrice = propertyPrice?.let(PeruFormat::soles),
        downPayment = downPayment?.let(PeruFormat::soles),
        termYears = termMonths?.let { it / SimulationConfig.MONTHS_PER_YEAR },
        // Only an applied bonus is worth a line; "not requested" and "not eligible" both show nothing.
        goodPayerBonus = goodPayerBonus?.takeIf { it.signum() > 0 }?.let(PeruFormat::soles),
        graceType = graceType,
        graceMonths = graceMonths,
    )
