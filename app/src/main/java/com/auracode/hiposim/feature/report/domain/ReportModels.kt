package com.auracode.hiposim.feature.report.domain

/** The PDF report of a simulation, exactly as the API sent it (US08). */
class ReportFile(
    val fileName: String,
    val bytes: ByteArray,
)

enum class ReportFailure {
    /** There is no simulation on screen to export. */
    SimulationMissing,

    /** There is no valid buyer session (401). The session is closed when this happens. */
    SessionExpired,

    /** The API does not know the simulation (404). */
    NotFound,

    /** The simulation belongs to another session (403). The use case calculates it again under the active one. */
    NotYours,
    NoConnection,
    ServiceUnavailable,
    Unexpected,
}

sealed interface ExportReportResult {
    data class Success(
        val file: ReportFile,
    ) : ExportReportResult

    data class Failure(
        val reason: ReportFailure,
    ) : ExportReportResult
}

/** The PDF report of a simulation of the API (`GET /api/simulations/{id}/report`). */
interface ReportRepository {
    suspend fun getReport(simulationId: String): ExportReportResult
}
