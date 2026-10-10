package com.auracode.hiposim.feature.simulation.data

import com.auracode.hiposim.core.network.ApiResult
import com.auracode.hiposim.core.network.map
import com.auracode.hiposim.core.network.safeApiCall
import com.auracode.hiposim.feature.simulation.domain.LoanSimulation
import com.auracode.hiposim.feature.simulation.domain.SimulationParams
import com.auracode.hiposim.feature.simulation.domain.SimulationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only [SimulationRepository]. Whether it talks to the real API or to [FakeSimulationService] is decided
 * by the [SimulationService] that Hilt provides (`BuildConfig.USE_FAKE_API`), so both paths share the DTOs,
 * the mapping and the error handling.
 */
@Singleton
class DefaultSimulationRepository
    @Inject
    constructor(
        private val service: SimulationService,
    ) : SimulationRepository {
        private val _currentSimulation = MutableStateFlow<LoanSimulation?>(null)
        override val currentSimulation: StateFlow<LoanSimulation?> = _currentSimulation.asStateFlow()

        override suspend fun simulate(params: SimulationParams): ApiResult<LoanSimulation> {
            val result =
                safeApiCall { service.createSimulation(params.toRequestDto()) }
                    .map { it.toDomain(params) }
            if (result is ApiResult.Success) _currentSimulation.value = result.data
            return result
        }
    }
