package com.auracode.hiposim.feature.simulation.data

import java.util.concurrent.ConcurrentHashMap

/**
 * What the in-memory API "stored" for each simulation, like the `simulations` table of the backend: the request
 * and the response, keyed by `simulationId`. [FakeSimulationService] writes it and the other fakes read it (the
 * saved scenarios of US06 take their figures from here, as the backend does).
 */
object FakeSimulationStore {
    private val simulations = ConcurrentHashMap<String, Pair<SimulationRequestDto, SimulationResponseDto>>()

    fun put(
        request: SimulationRequestDto,
        response: SimulationResponseDto,
    ) {
        simulations[response.simulationId] = request to response
    }

    fun get(simulationId: String): Pair<SimulationRequestDto, SimulationResponseDto>? = simulations[simulationId]
}
