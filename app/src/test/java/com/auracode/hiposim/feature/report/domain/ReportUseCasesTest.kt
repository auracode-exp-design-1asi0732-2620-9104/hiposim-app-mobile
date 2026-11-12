package com.auracode.hiposim.feature.report.domain

import com.auracode.hiposim.core.analytics.UsageEvents
import com.auracode.hiposim.core.network.TokenStore
import com.auracode.hiposim.feature.auth.data.DefaultAuthRepository
import com.auracode.hiposim.feature.auth.data.FakeAuthService
import com.auracode.hiposim.feature.auth.domain.SessionState
import com.auracode.hiposim.feature.report.data.DefaultReportRepository
import com.auracode.hiposim.feature.simulation.data.DefaultSimulationRepository
import com.auracode.hiposim.testing.RecordingUsageTracker
import com.auracode.hiposim.testing.StubReportService
import com.auracode.hiposim.testing.StubSimulationService
import com.auracode.hiposim.testing.contractExampleParams
import com.auracode.hiposim.testing.httpException
import com.auracode.hiposim.testing.validRegistrationForm
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** US08: exporting the PDF report of the simulation on screen. */
class ReportUseCasesTest {
    private val tokenStore = TokenStore()
    private val authRepository = DefaultAuthRepository(FakeAuthService(), tokenStore)
    private val simulationService = StubSimulationService()
    private val simulationRepository = DefaultSimulationRepository(simulationService, tokenStore)
    private val reportService = StubReportService()
    private val tracker = RecordingUsageTracker()
    private val export =
        ExportSimulationReportUseCase(
            reportRepository = DefaultReportRepository(reportService),
            simulationRepository = simulationRepository,
            authRepository = authRepository,
            usageTracker = tracker,
        )

    private suspend fun signIn() = authRepository.register(validRegistrationForm)

    private suspend fun simulate() = simulationRepository.simulate(contractExampleParams)

    private fun currentId() = requireNotNull(simulationRepository.currentSimulation.value).id

    @Test
    fun guest_isAskedForASession_andNothingIsDownloaded() =
        runTest {
            simulate()

            assertEquals(ExportReportResult.Failure(ReportFailure.SessionExpired), export())
            assertTrue(reportService.requests.isEmpty())
            assertTrue(tracker.events.isEmpty())
        }

    @Test
    fun withoutASimulation_nothingIsDownloaded() =
        runTest {
            signIn()

            assertEquals(ExportReportResult.Failure(ReportFailure.SimulationMissing), export())
            assertTrue(reportService.requests.isEmpty())
        }

    @Test
    fun signedInBuyer_downloadsThePdfOfTheSimulationOnScreen_andItIsMeasured() =
        runTest {
            signIn()
            simulate()

            val result = export()

            val file = (result as ExportReportResult.Success).file
            assertEquals("hiposim-simulacion-${currentId()}.pdf", file.fileName)
            assertEquals("%PDF-", String(file.bytes.copyOf(5), Charsets.US_ASCII))
            assertEquals(listOf(currentId()), reportService.requests)
            assertEquals(listOf(UsageEvents.PDF_EXPORTED), tracker.names)
            // Only figures and codes are measured: never the id of the simulation or anything of the buyer.
            assertEquals(
                setOf("termYears", "graceType"),
                tracker.events
                    .single()
                    .second.keys,
            )
        }

    @Test
    fun simulationCalculatedAsGuest_isDownloadedAsItIs_withoutRecalculating() =
        runTest {
            simulate()
            val guestId = currentId()
            signIn()

            val result = export()

            assertTrue(result is ExportReportResult.Success)
            assertEquals(1, simulationService.requests.size)
            assertEquals(listOf(guestId), reportService.requests)
        }

    @Test
    fun simulationOfAnotherSession_isCalculatedAgainUnderTheActiveOne_andRequestedOnce() =
        runTest {
            signIn()
            simulate()
            val firstId = currentId()
            val fake = reportService.onGet
            reportService.onGet = { id -> if (id == firstId) throw httpException(403) else fake(id) }

            val result = export()

            assertTrue(result is ExportReportResult.Success)
            assertEquals(2, simulationService.requests.size)
            assertEquals(2, reportService.requests.size)
            assertNotEquals(firstId, reportService.requests.last())
            assertEquals(currentId(), reportService.requests.last())
            assertEquals(listOf(UsageEvents.PDF_EXPORTED), tracker.names)
        }

    @Test
    fun ifTheSecondRequestIsAlsoRefused_itStopsThere() =
        runTest {
            signIn()
            simulate()
            reportService.onGet = { throw httpException(403) }

            val result = export()

            assertEquals(ExportReportResult.Failure(ReportFailure.NotYours), result)
            assertEquals(2, reportService.requests.size)
            assertTrue(tracker.events.isEmpty())
        }

    @Test
    fun expiredToken_signsOut() =
        runTest {
            signIn()
            simulate()
            reportService.onGet = { throw httpException(401) }

            assertEquals(ExportReportResult.Failure(ReportFailure.SessionExpired), export())
            assertEquals(SessionState.Guest, authRepository.session.value)
            assertTrue(tracker.events.isEmpty())
        }

    @Test
    fun unknownSimulation_isReportedAsNotFound() =
        runTest {
            signIn()
            simulate()
            reportService.onGet = { throw httpException(404) }

            assertEquals(ExportReportResult.Failure(ReportFailure.NotFound), export())
        }
}
