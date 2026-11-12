package com.auracode.hiposim.feature.scenarios.domain

import com.auracode.hiposim.feature.auth.domain.AuthRepository
import com.auracode.hiposim.feature.auth.domain.SessionState
import com.auracode.hiposim.feature.simulation.domain.SimulationFailure
import com.auracode.hiposim.feature.simulation.domain.SimulationRepository
import com.auracode.hiposim.feature.simulation.domain.SimulationResult
import javax.inject.Inject

/** Longest scenario name the API accepts. */
const val MAX_SCENARIO_NAME_LENGTH = 60

/**
 * Saves the simulation on screen under a name (US06).
 *
 * The API only saves a simulation that belongs to the signed in buyer, and the one on screen was often calculated
 * before signing in, so in that case it is calculated again under the session and that one is saved (as US21 does).
 */
class SaveScenarioUseCase
    @Inject
    constructor(
        private val scenariosRepository: ScenariosRepository,
        private val simulationRepository: SimulationRepository,
        private val authRepository: AuthRepository,
    ) {
        suspend operator fun invoke(name: String): SaveScenarioResult {
            val trimmed = name.trim()
            if (trimmed.isEmpty() || trimmed.length > MAX_SCENARIO_NAME_LENGTH) {
                return failure(ScenarioFailure.NameInvalid)
            }
            if (authRepository.session.value !is SessionState.Authenticated) {
                return failure(ScenarioFailure.SessionExpired)
            }
            if (simulationRepository.currentSimulation.value == null) return failure(ScenarioFailure.SimulationMissing)
            if (!simulationRepository.currentBelongsToSession) {
                val recalculated = simulationRepository.recalculateCurrent()
                if (recalculated is SimulationResult.Failure) return failure(recalculated.reason.toScenarioFailure())
            }
            val simulation =
                simulationRepository.currentSimulation.value ?: return failure(ScenarioFailure.SimulationMissing)
            val result = scenariosRepository.save(trimmed, simulation.id)
            if (result is SaveScenarioResult.Failure && result.reason == ScenarioFailure.SessionExpired) {
                // The token is no longer good: keeping the session would only repeat the failure.
                authRepository.signOut()
            }
            return result
        }

        private fun failure(reason: ScenarioFailure) = SaveScenarioResult.Failure(reason)

        private fun SimulationFailure.toScenarioFailure(): ScenarioFailure =
            when (this) {
                SimulationFailure.NoConnection -> ScenarioFailure.NoConnection
                SimulationFailure.ServiceUnavailable -> ScenarioFailure.ServiceUnavailable
                is SimulationFailure.Rejected, SimulationFailure.Unexpected -> ScenarioFailure.Unexpected
            }
    }

/** The signed in buyer's scenarios, newest first. Without a session there is nothing to ask for. */
class GetScenariosUseCase
    @Inject
    constructor(
        private val scenariosRepository: ScenariosRepository,
        private val authRepository: AuthRepository,
    ) {
        suspend operator fun invoke(): ScenariosResult {
            if (authRepository.session.value !is SessionState.Authenticated) {
                return ScenariosResult.Failure(ScenarioFailure.SessionExpired)
            }
            val result = scenariosRepository.getScenarios()
            if (result is ScenariosResult.Failure && result.reason == ScenarioFailure.SessionExpired) {
                authRepository.signOut()
            }
            return result
        }
    }
