package com.auracode.hiposim.feature.simulation.data

import retrofit2.http.Body
import retrofit2.http.POST

/** Simulation endpoints of the HipoSim API. [FakeSimulationService] stands in while the API host does not exist. */
interface SimulationService {
    /**
     * Public endpoint: the buyer token is optional and the auth interceptor adds it when there is a session.
     * Answers 400 with `application/problem+json` and an `errors` map when the request is invalid.
     */
    @POST("api/simulations")
    suspend fun createSimulation(
        @Body request: SimulationRequestDto,
    ): SimulationResponseDto
}
