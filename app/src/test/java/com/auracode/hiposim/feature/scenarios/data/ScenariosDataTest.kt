package com.auracode.hiposim.feature.scenarios.data

import com.auracode.hiposim.core.network.ApiError
import com.auracode.hiposim.core.network.ApiResult
import com.auracode.hiposim.core.network.safeApiCall
import com.auracode.hiposim.feature.scenarios.domain.SaveScenarioResult
import com.auracode.hiposim.feature.scenarios.domain.ScenarioFailure
import com.auracode.hiposim.feature.scenarios.domain.ScenariosResult
import com.auracode.hiposim.feature.simulation.data.FakeSimulationService
import com.auracode.hiposim.feature.simulation.data.SimulationRequestDto
import com.auracode.hiposim.feature.simulation.domain.GraceType
import com.auracode.hiposim.testing.StubScenariosService
import com.auracode.hiposim.testing.httpException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** US06: the in-memory API and the repository of saved scenarios. */
class ScenariosDataTest {
    private val now = OffsetDateTime.of(2026, 11, 12, 15, 0, 0, 0, ZoneOffset.UTC)

    private suspend fun simulationId(
        graceType: String? = null,
        graceMonths: Int? = null,
    ): String =
        FakeSimulationService()
            .createSimulation(
                SimulationRequestDto(
                    propertyPrice = 280_000.0,
                    downPayment = 42_000.0,
                    annualEffectiveRate = 0.0885,
                    termInMonths = 240,
                    applyGoodPayerBonus = true,
                    gracePeriodMonths = graceMonths,
                    graceType = graceType,
                ),
            ).simulationId

    @Test
    fun fake_savesTheScenarioWithTheFiguresOfItsSimulation() =
        runTest {
            val service = FakeScenariosService(now = { now })
            val simulationId = simulationId(graceType = "partial", graceMonths = 3)

            val scenario = service.createScenario(CreateScenarioRequestDto("  Plazo 20 años  ", simulationId))

            assertEquals("Plazo 20 años", scenario.name)
            assertEquals(simulationId, scenario.simulationId)
            assertEquals(280_000.0, scenario.propertyPrice)
            assertEquals(240, scenario.termInMonths)
            assertEquals("partial", scenario.graceType)
            assertEquals(3, scenario.gracePeriodMonths)
            assertEquals(18_200.0, scenario.goodPayerBonus)
            assertEquals(1918.11, scenario.monthlyInstallment)
            assertEquals(listOf(scenario), service.getScenarios())
        }

    @Test
    fun fake_listsTheNewestFirst() =
        runTest {
            val service = FakeScenariosService(now = { now })
            service.createScenario(CreateScenarioRequestDto("Primero", simulationId()))
            service.createScenario(CreateScenarioRequestDto("Segundo", simulationId()))

            assertEquals(listOf("Segundo", "Primero"), service.getScenarios().map { it.name })
        }

    @Test
    fun fake_failsLikeTheServer() =
        runTest {
            val service = FakeScenariosService()

            val blank = safeApiCall { service.createScenario(CreateScenarioRequestDto("  ", simulationId())) }
            val unknown = safeApiCall { service.createScenario(CreateScenarioRequestDto("Mío", "no-such-id")) }

            assertTrue((blank as ApiResult.Failure).error is ApiError.Validation)
            assertEquals(404, ((unknown as ApiResult.Failure).error as ApiError.Client).code)
        }

    @Test
    fun repository_mapsTheContractNamesToTheModel() =
        runTest {
            val repository = DefaultScenariosRepository(FakeScenariosService(now = { now }))

            val result = repository.save("Con gracia", simulationId(graceType = "total", graceMonths = 3))

            val scenario = (result as SaveScenarioResult.Success).scenario
            assertEquals(Instant.parse("2026-11-12T15:00:00Z"), scenario.createdAt)
            assertEquals(BigDecimal("219800.00"), scenario.netLoan)
            assertEquals(BigDecimal("1959.21"), scenario.monthlyPayment)
            assertEquals(GraceType.Total, scenario.graceType)
            assertEquals(3, scenario.graceMonths)
            assertEquals(BigDecimal("0.088500"), scenario.tcea)
        }

    @Test
    fun repository_keepsMissingFiguresAsNull() =
        runTest {
            val service = StubScenariosService()
            service.onGet = { listOf(ScenarioDto("s1", "Sin cifras", "sim", "2026-11-12T15:00:00Z")) }

            val scenario =
                (
                    DefaultScenariosRepository(
                        service,
                    ).getScenarios() as ScenariosResult.Success
                ).scenarios.single()

            assertNull(scenario.monthlyPayment)
            assertNull(scenario.tcea)
            assertEquals(GraceType.None, scenario.graceType)
        }

    @Test
    fun repository_translatesTheErrorsOfTheApi() =
        runTest {
            val service = StubScenariosService()
            val repository = DefaultScenariosRepository(service)
            val cases =
                mapOf(
                    httpException(401) to ScenarioFailure.SessionExpired,
                    httpException(403) to ScenarioFailure.SessionExpired,
                    httpException(404) to ScenarioFailure.NotFound,
                    httpException(500) to ScenarioFailure.ServiceUnavailable,
                    IOException("offline") to ScenarioFailure.NoConnection,
                )

            cases.forEach { (error, expected) ->
                service.onCreate = { throw error }
                service.onGet = { throw error }

                assertEquals(SaveScenarioResult.Failure(expected), repository.save("Mío", "sim"))
                assertEquals(ScenariosResult.Failure(expected), repository.getScenarios())
            }
        }
}
