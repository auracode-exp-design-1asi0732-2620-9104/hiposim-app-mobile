package com.auracode.hiposim.feature.leads.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** Lead endpoints of the HipoSim API. [FakeLeadsService] stands in when `BuildConfig.USE_FAKE_API` is on. */
interface LeadsService {
    /**
     * Only for a buyer session: the auth interceptor attaches the token. Answers 201 with the lead, 400 when the
     * consent or an id is missing, 404 when the agency or the simulation does not exist (or the simulation is not
     * the buyer's), and 401 or 403 without a valid buyer session.
     */
    @POST("api/leads")
    suspend fun createLead(
        @Body request: CreateLeadRequestDto,
    ): LeadResponseDto
}

/**
 * Agencies a buyer can send a simulation to. PROPOSED endpoint, not in the backend yet: while it does not
 * exist the app uses [FakeAgenciesService] (`BuildConfig.USE_FAKE_AGENCIES`), even against the real API.
 */
interface AgenciesService {
    @GET("api/agencies")
    suspend fun getAgencies(): List<AgencyDto>
}
