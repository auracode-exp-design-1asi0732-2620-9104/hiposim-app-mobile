package com.auracode.hiposim.feature.scenarios.domain

import com.auracode.hiposim.core.analytics.UsageEvents
import com.auracode.hiposim.core.analytics.UsageTracker
import javax.inject.Inject

sealed interface CompareScenariosResult {
    data class Success(
        val comparison: ScenarioComparison,
    ) : CompareScenariosResult

    data class Failure(
        val reason: ScenarioFailure,
    ) : CompareScenariosResult
}

/**
 * Compares two or three of the buyer's saved scenarios side by side (US07).
 *
 * The scenarios are read again from the API, so the comparison always shows what is saved now. The columns follow
 * the order of [ids], which is the order in which the buyer picked them.
 */
class CompareScenariosUseCase
    @Inject
    constructor(
        private val getScenarios: GetScenariosUseCase,
        private val usageTracker: UsageTracker = UsageTracker.None,
    ) {
        suspend operator fun invoke(ids: List<String>): CompareScenariosResult {
            val distinct = ids.distinct()
            if (distinct.size !in MIN_COMPARED_SCENARIOS..MAX_COMPARED_SCENARIOS) {
                return CompareScenariosResult.Failure(ScenarioFailure.SelectionInvalid)
            }
            return when (val result = getScenarios()) {
                is ScenariosResult.Failure -> CompareScenariosResult.Failure(result.reason)
                is ScenariosResult.Success -> {
                    val byId = result.scenarios.associateBy { it.id }
                    val chosen = distinct.mapNotNull { byId[it] }
                    if (chosen.size != distinct.size) {
                        // One was deleted or never belonged to this buyer: comparing the rest would hide that.
                        CompareScenariosResult.Failure(ScenarioFailure.NotFound)
                    } else {
                        // Only how many were compared is measured: never their names or figures.
                        usageTracker.track(UsageEvents.SCENARIOS_COMPARED, mapOf("count" to chosen.size))
                        CompareScenariosResult.Success(compareScenarios(chosen))
                    }
                }
            }
        }
    }
