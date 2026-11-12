package com.auracode.hiposim.testing

import com.auracode.hiposim.feature.report.data.FakeReportService
import com.auracode.hiposim.feature.report.data.ReportService
import okhttp3.ResponseBody

/** A report service whose answers each test decides. It behaves like the fake by default. */
class StubReportService : ReportService {
    private val fake = FakeReportService()
    var onGet: suspend (String) -> ResponseBody = { fake.getReport(it) }
    val requests = mutableListOf<String>()

    override suspend fun getReport(simulationId: String): ResponseBody {
        requests += simulationId
        return onGet(simulationId)
    }
}
