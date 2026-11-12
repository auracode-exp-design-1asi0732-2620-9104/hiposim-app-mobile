package com.auracode.hiposim.feature.report.domain

import com.auracode.hiposim.core.analytics.UsageEvents
import com.auracode.hiposim.core.analytics.UsageTracker
import com.auracode.hiposim.feature.auth.domain.AuthRepository
import com.auracode.hiposim.feature.auth.domain.SessionState
import com.auracode.hiposim.feature.simulation.domain.SimulationFailure
import com.auracode.hiposim.feature.simulation.domain.SimulationRepository
import com.auracode.hiposim.feature.simulation.domain.SimulationResult
import javax.inject.Inject

/**
 * Downloads the PDF report of the simulation on screen (US08).
 *
 * Only a signed in buyer can export it. The API gives the report of a simulation calculated without a session to
 * whoever knows its id, and of a simulation of a buyer only to that buyer. So when the one on screen belongs to
 * another session (the buyer signed out and another signed in), it is calculated again under the active session and
 * the report is asked for once more.
 */
class ExportSimulationReportUseCase
    @Inject
    constructor(
        private val reportRepository: ReportRepository,
        private val simulationRepository: SimulationRepository,
        private val authRepository: AuthRepository,
        private val usageTracker: UsageTracker = UsageTracker.None,
    ) {
        suspend operator fun invoke(): ExportReportResult {
            if (authRepository.session.value !is SessionState.Authenticated) {
                return failure(
                    ReportFailure.SessionExpired,
                )
            }
            val simulation =
                simulationRepository.currentSimulation.value ?: return failure(ReportFailure.SimulationMissing)

            var result = reportRepository.getReport(simulation.id)
            if (result is ExportReportResult.Failure && result.reason == ReportFailure.NotYours) {
                val recalculated = simulationRepository.recalculateCurrent()
                if (recalculated is SimulationResult.Failure) return failure(recalculated.reason.toReportFailure())
                val again =
                    simulationRepository.currentSimulation.value ?: return failure(ReportFailure.SimulationMissing)
                result = reportRepository.getReport(again.id)
            }

            when (result) {
                is ExportReportResult.Success ->
                    usageTracker.track(
                        UsageEvents.PDF_EXPORTED,
                        mapOf("termYears" to simulation.termYears, "graceType" to simulation.graceType.apiValue),
                    )

                is ExportReportResult.Failure ->
                    if (result.reason == ReportFailure.SessionExpired) {
                        // The token is no longer good: keeping the session would only repeat the failure.
                        authRepository.signOut()
                    }
            }
            return result
        }

        private fun failure(reason: ReportFailure) = ExportReportResult.Failure(reason)

        private fun SimulationFailure.toReportFailure(): ReportFailure =
            when (this) {
                SimulationFailure.NoConnection -> ReportFailure.NoConnection
                SimulationFailure.ServiceUnavailable -> ReportFailure.ServiceUnavailable
                is SimulationFailure.Rejected, SimulationFailure.Unexpected -> ReportFailure.Unexpected
            }
    }
