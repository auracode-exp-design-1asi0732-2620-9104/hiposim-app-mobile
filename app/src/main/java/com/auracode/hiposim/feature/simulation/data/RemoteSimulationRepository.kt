package com.auracode.hiposim.feature.simulation.data

import com.auracode.hiposim.core.network.ApiResponse
import com.auracode.hiposim.core.network.ProblemDetailsDto
import com.auracode.hiposim.core.network.apiCall
import com.auracode.hiposim.feature.simulation.domain.LoanSimulation
import com.auracode.hiposim.feature.simulation.domain.SimulationFailure
import com.auracode.hiposim.feature.simulation.domain.SimulationField
import com.auracode.hiposim.feature.simulation.domain.SimulationRepository
import com.auracode.hiposim.feature.simulation.domain.SimulationRequest
import com.auracode.hiposim.feature.simulation.domain.SimulationResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simulations calculated by the HipoSim API. The last successful one is kept in memory so the Results and
 * Registration screens can show it.
 */
@Singleton
class RemoteSimulationRepository
    @Inject
    constructor(
        private val api: SimulationApi,
    ) : SimulationRepository {
        @Volatile
        private var current: LoanSimulation? = null

        override suspend fun simulate(request: SimulationRequest): SimulationResult =
            when (val response = apiCall { api.simulate(request.toDto()) }) {
                is ApiResponse.Success -> {
                    val simulation = response.body.toDomainOrNull(request)
                    if (simulation == null) {
                        SimulationResult.Failure(SimulationFailure.ServerError)
                    } else {
                        current = simulation
                        SimulationResult.Success(simulation)
                    }
                }

                is ApiResponse.HttpError ->
                    SimulationResult.Failure(
                        if (response.code == HTTP_BAD_REQUEST) {
                            SimulationFailure.Rejected(response.problem.rejectedFields())
                        } else {
                            SimulationFailure.ServerError
                        },
                    )

                is ApiResponse.NetworkError -> SimulationResult.Failure(SimulationFailure.NoConnection)

                is ApiResponse.UnreadableResponse -> SimulationResult.Failure(SimulationFailure.ServerError)
            }

        override suspend fun getCurrentSimulation(): LoanSimulation? = current

        private fun ProblemDetailsDto?.rejectedFields(): Set<SimulationField> =
            this
                ?.errors
                ?.keys
                ?.mapNotNull { FIELD_BY_PROPERTY[it] }
                ?.toSet()
                .orEmpty()

        private companion object {
            const val HTTP_BAD_REQUEST = 400

            /** Request properties of the contract mapped to the form fields that edit them. */
            val FIELD_BY_PROPERTY =
                mapOf(
                    "propertyPrice" to SimulationField.PropertyPrice,
                    "downPayment" to SimulationField.DownPayment,
                    "termInMonths" to SimulationField.Term,
                )
        }
    }
