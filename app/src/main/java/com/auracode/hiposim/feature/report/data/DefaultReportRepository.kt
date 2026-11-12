package com.auracode.hiposim.feature.report.data

import com.auracode.hiposim.core.network.ApiError
import com.auracode.hiposim.core.network.ApiResult
import com.auracode.hiposim.core.network.safeApiCall
import com.auracode.hiposim.feature.report.domain.ExportReportResult
import com.auracode.hiposim.feature.report.domain.ReportFailure
import com.auracode.hiposim.feature.report.domain.ReportFile
import com.auracode.hiposim.feature.report.domain.ReportRepository
import javax.inject.Inject
import javax.inject.Singleton

/** The only [ReportRepository]. Real or in-memory answers are decided by the service that Hilt provides. */
@Singleton
class DefaultReportRepository
    @Inject
    constructor(
        private val service: ReportService,
    ) : ReportRepository {
        override suspend fun getReport(simulationId: String): ExportReportResult =
            when (val response = safeApiCall { service.getReport(simulationId).use { it.bytes() } }) {
                is ApiResult.Success ->
                    if (response.data.isPdf()) {
                        ExportReportResult.Success(ReportFile(fileNameOf(simulationId), response.data))
                    } else {
                        // A 200 that is not a PDF (a proxy page, an empty body) must not be handed to a PDF viewer.
                        ExportReportResult.Failure(ReportFailure.Unexpected)
                    }

                is ApiResult.Failure -> ExportReportResult.Failure(response.error.toReportFailure())
            }

        private fun ApiError.toReportFailure(): ReportFailure =
            when (this) {
                ApiError.NoConnection -> ReportFailure.NoConnection
                is ApiError.Unauthorized -> ReportFailure.SessionExpired
                is ApiError.Client ->
                    when (code) {
                        HTTP_FORBIDDEN -> ReportFailure.NotYours
                        HTTP_NOT_FOUND -> ReportFailure.NotFound
                        else -> ReportFailure.Unexpected
                    }
                is ApiError.Server -> ReportFailure.ServiceUnavailable
                is ApiError.Validation, is ApiError.Conflict, is ApiError.Unexpected -> ReportFailure.Unexpected
            }

        private companion object {
            const val HTTP_FORBIDDEN = 403
            const val HTTP_NOT_FOUND = 404
            val PDF_MAGIC = "%PDF-".toByteArray(Charsets.US_ASCII)
        }

        private fun ByteArray.isPdf(): Boolean =
            size > PDF_MAGIC.size && PDF_MAGIC.indices.all { this[it] == PDF_MAGIC[it] }
    }

/** Same name as the API sends in `Content-Disposition`. */
internal fun fileNameOf(simulationId: String) = "hiposim-simulacion-$simulationId.pdf"
