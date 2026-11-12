package com.auracode.hiposim.feature.scenarios.domain

import com.auracode.hiposim.feature.simulation.domain.GraceType
import java.math.BigDecimal
import java.time.Instant

/**
 * A simulation the buyer saved under a name (US06), with the figures the API stored for it. A figure is null when
 * the API did not send it. Amounts in soles (PEN), rates as fractions.
 */
data class SavedScenario(
    val id: String,
    val name: String,
    val simulationId: String,
    val createdAt: Instant,
    val propertyPrice: BigDecimal?,
    val downPayment: BigDecimal?,
    val termMonths: Int?,
    val graceType: GraceType,
    val graceMonths: Int,
    val bonusRequested: Boolean?,
    val goodPayerBonus: BigDecimal?,
    val netLoan: BigDecimal?,
    val monthlyPayment: BigDecimal?,
    val totalPaid: BigDecimal?,
    val tcea: BigDecimal?,
)

enum class ScenarioFailure {
    /** The name is empty or too long. Checked before calling the API, and also returned by it (400). */
    NameInvalid,

    /** Fewer than two or more than three scenarios were picked to compare (US07). */
    SelectionInvalid,

    /** There is no simulation on screen to save. */
    SimulationMissing,

    /** The API does not know the simulation, or it belongs to another buyer (404). */
    NotFound,

    /** There is no valid buyer session (401 or 403). The session is closed when this happens. */
    SessionExpired,
    NoConnection,
    ServiceUnavailable,
    Unexpected,
}

sealed interface SaveScenarioResult {
    data class Success(
        val scenario: SavedScenario,
    ) : SaveScenarioResult

    data class Failure(
        val reason: ScenarioFailure,
    ) : SaveScenarioResult
}

sealed interface ScenariosResult {
    data class Success(
        val scenarios: List<SavedScenario>,
    ) : ScenariosResult

    data class Failure(
        val reason: ScenarioFailure,
    ) : ScenariosResult
}

/** The buyer's saved scenarios. */
interface ScenariosRepository {
    suspend fun save(
        name: String,
        simulationId: String,
    ): SaveScenarioResult

    suspend fun getScenarios(): ScenariosResult
}
