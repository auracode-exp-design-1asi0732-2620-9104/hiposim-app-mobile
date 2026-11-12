package com.auracode.hiposim.feature.report.data

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Report endpoint of the HipoSim API. Answers `application/pdf`, or 403 (the simulation is of another buyer) and 404
 * (unknown simulation) as `application/problem+json`. [FakeReportService] stands in for the demo build.
 */
interface ReportService {
    @GET("api/simulations/{id}/report")
    suspend fun getReport(
        @Path("id") simulationId: String,
    ): ResponseBody
}
