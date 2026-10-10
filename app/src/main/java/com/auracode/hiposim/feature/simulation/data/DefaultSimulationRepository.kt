package com.auracode.hiposim.feature.simulation.data

import com.auracode.hiposim.core.network.ApiError
import com.auracode.hiposim.core.network.ApiResult
import com.auracode.hiposim.core.network.TokenStore
import com.auracode.hiposim.core.network.safeApiCall
import com.auracode.hiposim.feature.simulation.domain.LoanSimulation
import com.auracode.hiposim.feature.simulation.domain.SimulationFailure
import com.auracode.hiposim.feature.simulation.domain.SimulationField
import com.auracode.hiposim.feature.simulation.domain.SimulationParams
import com.auracode.hiposim.feature.simulation.domain.SimulationRepository
import com.auracode.hiposim.feature.simulation.domain.SimulationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only [SimulationRepository]. Whether it talks to the real API or to [FakeSimulationService] is decided
 * by the [SimulationService] that Hilt provides (`BuildConfig.USE_FAKE_API`), so both paths share the DTOs,
 * the mapping and the error handling.
 *
 * Network outcomes ([ApiResult]) stop here: the domain and the UI only see [SimulationResult].
 */
@Singleton
class DefaultSimulationRepository
    @Inject
    constructor(
        private val service: SimulationService,
        private val tokenStore: TokenStore = TokenStore(),
    ) : SimulationRepository {
        private val _currentSimulation = MutableStateFlow<LoanSimulation?>(null)
        override val currentSimulation: StateFlow<LoanSimulation?> = _currentSimulation.asStateFlow()

        /** What the current simulation was calculated from, and the token its request carried (null for a guest). */
        @Volatile
        private var currentParams: SimulationParams? = null

        @Volatile
        private var currentToken: String? = null

        override val currentBelongsToSession: Boolean
            get() {
                val activeToken = tokenStore.token
                return _currentSimulation.value != null && activeToken != null && activeToken == currentToken
            }

        override suspend fun recalculateCurrent(): SimulationResult {
            val params = currentParams ?: return SimulationResult.Failure(SimulationFailure.Unexpected)
            return simulate(params)
        }

        override suspend fun simulate(params: SimulationParams): SimulationResult {
            // Read before the call: this is the token the auth interceptor attaches to it.
            val tokenOfTheRequest = tokenStore.token
            // The mapping runs inside the call so a body that cannot be mapped (a bad date) is a failure, not a crash.
            val response = safeApiCall { service.createSimulation(params.toRequestDto()).toDomainOrNull(params) }
            return when (response) {
                is ApiResult.Success -> {
                    val simulation = response.data
                    if (simulation == null) {
                        SimulationResult.Failure(SimulationFailure.Unexpected)
                    } else {
                        currentParams = params
                        currentToken = tokenOfTheRequest
                        _currentSimulation.value = simulation
                        SimulationResult.Success(simulation)
                    }
                }

                is ApiResult.Failure -> SimulationResult.Failure(response.error.toSimulationFailure())
            }
        }

        private fun ApiError.toSimulationFailure(): SimulationFailure =
            when (this) {
                is ApiError.Validation -> SimulationFailure.Rejected(toFieldMessages())
                ApiError.NoConnection -> SimulationFailure.NoConnection
                is ApiError.Server -> SimulationFailure.ServiceUnavailable
                is ApiError.Unauthorized,
                is ApiError.Conflict,
                is ApiError.Client,
                is ApiError.Unexpected,
                -> SimulationFailure.Unexpected
            }

        /** First message of each form field the API named. Properties the form does not show are dropped. */
        private fun ApiError.Validation.toFieldMessages(): Map<SimulationField, String> =
            buildMap {
                fieldErrors.forEach { (property, messages) ->
                    val field = SimulationField.fromRequestProperty(property)
                    val message = messages.firstOrNull()
                    if (field != null && message != null) put(field, message)
                }
            }
    }
