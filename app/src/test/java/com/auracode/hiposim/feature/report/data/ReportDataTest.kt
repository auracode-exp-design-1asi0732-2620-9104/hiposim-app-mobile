package com.auracode.hiposim.feature.report.data

import com.auracode.hiposim.feature.report.domain.ExportReportResult
import com.auracode.hiposim.feature.report.domain.ReportFailure
import com.auracode.hiposim.testing.StubReportService
import com.auracode.hiposim.testing.contractExampleParams
import com.auracode.hiposim.testing.fakeBackedSimulationRepository
import com.auracode.hiposim.testing.httpException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** US08: the report service, its in-memory stand-in and how the repository reads their answers. */
class ReportDataTest {
    private val service = StubReportService()
    private val repository = DefaultReportRepository(service)

    private suspend fun knownSimulationId(): String {
        val simulations = fakeBackedSimulationRepository()
        simulations.simulate(contractExampleParams)
        return requireNotNull(simulations.currentSimulation.value).id
    }

    private fun failureOf(result: ExportReportResult) = (result as ExportReportResult.Failure).reason

    @Test
    fun fake_answersAPdfForASimulationItCalculated() =
        runTest {
            val id = knownSimulationId()

            val file = (repository.getReport(id) as ExportReportResult.Success).file

            assertEquals("hiposim-simulacion-$id.pdf", file.fileName)
            assertEquals("%PDF-1.4", String(file.bytes.copyOf(8), Charsets.US_ASCII))
        }

    @Test
    fun fake_answers404ForAnUnknownSimulation() =
        runTest {
            val unknown = "00000000-0000-0000-0000-000000000000"

            assertEquals(ReportFailure.NotFound, failureOf(repository.getReport(unknown)))
        }

    @Test
    fun demoPdf_hasOffsetsAnyViewerCanFollow() {
        val text = String(FakeReportService.demoPdf("abc"), Charsets.US_ASCII)

        assertTrue(text.startsWith("%PDF-1.4\n"))
        assertTrue(text.trimEnd().endsWith("%%EOF"))
        // startxref points at the xref table, and each entry of it points at its object.
        val xrefAt = Regex("startxref\\n(\\d+)").find(text)!!.groupValues[1].toInt()
        assertEquals("xref", text.substring(xrefAt, xrefAt + 4))
        val entries = Regex("(\\d{10}) 00000 n").findAll(text).map { it.groupValues[1].toInt() }.toList()
        assertEquals(5, entries.size)
        entries.forEachIndexed { index, offset ->
            assertTrue(text.substring(offset).startsWith("${index + 1} 0 obj"))
        }
    }

    @Test
    fun httpErrors_areTranslatedToWhatTheBuyerCanDo() =
        runTest {
            mapOf(
                401 to ReportFailure.SessionExpired,
                403 to ReportFailure.NotYours,
                404 to ReportFailure.NotFound,
                400 to ReportFailure.Unexpected,
                500 to ReportFailure.ServiceUnavailable,
                503 to ReportFailure.ServiceUnavailable,
            ).forEach { (code, expected) ->
                service.onGet = { throw httpException(code) }

                assertEquals("HTTP $code", expected, failureOf(repository.getReport("id")))
            }
        }

    @Test
    fun noNetwork_isReportedAsNoConnection() =
        runTest {
            service.onGet = { throw IOException("offline") }

            assertEquals(ReportFailure.NoConnection, failureOf(repository.getReport("id")))
        }

    @Test
    fun aBodyThatIsNotAPdf_isNeverHandedToAViewer() =
        runTest {
            val pdf = "application/pdf".toMediaType()
            listOf("", "%PDF-", "<html>captive portal</html>").forEach { body ->
                service.onGet = { body.toResponseBody(pdf) }

                assertEquals("'$body'", ReportFailure.Unexpected, failureOf(repository.getReport("id")))
            }
        }
}
