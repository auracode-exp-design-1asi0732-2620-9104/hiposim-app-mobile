package com.auracode.hiposim.feature.scenarios.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** Saved scenarios of the signed in buyer (US06). [FakeScenariosService] stands in with `BuildConfig.USE_FAKE_API`. */
interface ScenariosService {
    /**
     * Only for a buyer session: the auth interceptor attaches the token. Answers 201 with the scenario, 400 when the
     * name or the simulation is missing, 404 when the simulation does not exist or is not the buyer's, and 401 or
     * 403 without a valid buyer session.
     */
    @POST("api/scenarios")
    suspend fun createScenario(
        @Body request: CreateScenarioRequestDto,
    ): ScenarioDto

    /** The buyer's scenarios, newest first. */
    @GET("api/scenarios")
    suspend fun getScenarios(): List<ScenarioDto>
}
