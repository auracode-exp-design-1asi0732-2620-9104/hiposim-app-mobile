package com.auracode.hiposim.feature.leads.domain

import com.auracode.hiposim.feature.auth.domain.AuthRepository
import com.auracode.hiposim.feature.auth.domain.SessionState
import com.auracode.hiposim.feature.simulation.domain.SimulationFailure
import com.auracode.hiposim.feature.simulation.domain.SimulationRepository
import com.auracode.hiposim.feature.simulation.domain.SimulationResult
import javax.inject.Inject

class GetAgenciesUseCase
    @Inject
    constructor(
        private val repository: LeadsRepository,
    ) {
        suspend operator fun invoke(): AgenciesResult = repository.getAgencies()
    }

/**
 * Sends the simulation on screen to an agency (US21).
 *
 * Nothing leaves the device without a session and without the explicit consent. The API only accepts a
 * simulation that belongs to the signed in buyer, and the one on screen was often calculated before signing in,
 * so in that case it is calculated again under the session and that new one is sent.
 */
class SendSimulationToAgencyUseCase
    @Inject
    constructor(
        private val leadsRepository: LeadsRepository,
        private val simulationRepository: SimulationRepository,
        private val authRepository: AuthRepository,
    ) {
        suspend operator fun invoke(
            agencyId: String,
            consentAccepted: Boolean,
        ): SendLeadResult {
            if (!consentAccepted) return failure(LeadFailure.ConsentRequired)
            if (authRepository.session.value !is SessionState.Authenticated) return failure(LeadFailure.SessionExpired)
            if (simulationRepository.currentSimulation.value == null) return failure(LeadFailure.SimulationMissing)

            if (!simulationRepository.currentBelongsToSession) {
                val recalculated = simulationRepository.recalculateCurrent()
                if (recalculated is SimulationResult.Failure) return failure(recalculated.reason.toLeadFailure())
            }
            val simulation =
                simulationRepository.currentSimulation.value ?: return failure(LeadFailure.SimulationMissing)

            val result = leadsRepository.sendSimulation(agencyId, simulation.id, acceptedContact = true)
            if (result is SendLeadResult.Failure && result.reason == LeadFailure.SessionExpired) {
                // The token is no longer good: keeping the session would only repeat the failure.
                authRepository.signOut()
            }
            return result
        }

        private fun failure(reason: LeadFailure) = SendLeadResult.Failure(reason)

        private fun SimulationFailure.toLeadFailure(): LeadFailure =
            when (this) {
                SimulationFailure.NoConnection -> LeadFailure.NoConnection
                SimulationFailure.ServiceUnavailable -> LeadFailure.ServiceUnavailable
                is SimulationFailure.Rejected, SimulationFailure.Unexpected -> LeadFailure.Unexpected
            }
    }
