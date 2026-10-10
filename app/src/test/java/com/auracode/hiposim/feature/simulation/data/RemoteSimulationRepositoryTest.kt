package com.auracode.hiposim.feature.simulation.data

import com.auracode.hiposim.core.network.NetworkJson
import com.auracode.hiposim.feature.simulation.domain.SimulationFailure
import com.auracode.hiposim.feature.simulation.domain.SimulationField
import com.auracode.hiposim.feature.simulation.domain.SimulationRequest
import com.auracode.hiposim.feature.simulation.domain.SimulationResult
import com.auracode.hiposim.testing.readTestResource
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

/** Runs the real Retrofit client against a local server that answers with the contract examples. */
class RemoteSimulationRepositoryTest {
    private lateinit var server: MockWebServer

    private val request =
        SimulationRequest(
            propertyPrice = BigDecimal("280000"),
            downPayment = BigDecimal("42000"),
            termYears = 20,
        )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun repository(baseUrl: HttpUrl = server.url("/")): RemoteSimulationRepository {
        val client =
            OkHttpClient
                .Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build()
        val retrofit =
            Retrofit
                .Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(NetworkJson.asConverterFactory("application/json; charset=UTF-8".toMediaType()))
                .build()
        return RemoteSimulationRepository(retrofit.create(SimulationApi::class.java))
    }

    private fun respond(
        code: Int,
        body: String,
        contentType: String = "application/json",
    ) {
        server.enqueue(
            MockResponse()
                .setResponseCode(code)
                .addHeader("Content-Type", contentType)
                .setBody(body),
        )
    }

    private fun respondProblem(
        code: Int,
        body: String,
    ) = respond(code, body, "application/problem+json")

    private fun withBonusBody() = readTestResource("contract/simulation-response-with-bonus.json")

    private fun rejected(vararg fields: SimulationField) =
        SimulationResult.Failure(SimulationFailure.Rejected(fields.toSet()))

    @Test
    fun simulate_sendsThePostWithTheContractBody() =
        runTest {
            respond(200, withBonusBody())

            repository().simulate(request)

            val recorded = server.takeRequest()
            assertEquals("POST", recorded.method)
            assertEquals("/api/simulations", recorded.path)
            assertTrue(recorded.getHeader("Content-Type").orEmpty().startsWith("application/json"))
            assertNull("A guest sends no token", recorded.getHeader("Authorization"))
            val body = Json.parseToJsonElement(recorded.body.readUtf8()).jsonObject
            assertEquals(280000.0, body.getValue("propertyPrice").jsonPrimitive.double, 0.0)
            assertEquals(42000.0, body.getValue("downPayment").jsonPrimitive.double, 0.0)
            assertEquals(0.0885, body.getValue("annualEffectiveRate").jsonPrimitive.double, 0.0)
            assertEquals(240, body.getValue("termInMonths").jsonPrimitive.int)
            assertEquals(false, body.getValue("applyGoodPayerBonus").jsonPrimitive.boolean)
        }

    @Test
    fun simulate_whenTheApiAnswers200_returnsTheSimulationAndKeepsItAsCurrent() =
        runTest {
            respond(200, withBonusBody())
            val repository = repository()
            assertNull(repository.getCurrentSimulation())

            val result = repository.simulate(request)

            val simulation = (result as SimulationResult.Success).simulation
            assertEquals(0, BigDecimal("219800").compareTo(simulation.netLoan))
            assertEquals(0, BigDecimal("1863.99").compareTo(simulation.monthlyPayment))
            assertEquals(simulation, repository.getCurrentSimulation())
        }

    @Test
    fun simulate_whenTheApiRejectsTheDownPayment_namesTheField() =
        runTest {
            respondProblem(400, readTestResource("contract/problem-down-payment-too-low.json"))
            val repository = repository()

            val result = repository.simulate(request)

            assertEquals(rejected(SimulationField.DownPayment), result)
            assertNull("A failed simulation must not replace the current one", repository.getCurrentSimulation())
        }

    @Test
    fun simulate_whenTheApiRejectsSeveralFields_namesTheOnesTheFormHas() =
        runTest {
            val errors = """{"propertyPrice":["a"],"termInMonths":["b"],"annualEffectiveRate":["c"]}"""
            respondProblem(400, """{"title":"x","status":400,"errors":$errors}""")

            val result = repository().simulate(request)

            assertEquals(rejected(SimulationField.PropertyPrice, SimulationField.Term), result)
        }

    @Test
    fun simulate_when400HasNoFieldErrors_rejectsWithoutFields() =
        runTest {
            respond(400, "not json at all", "text/plain")

            val result = repository().simulate(request)

            assertEquals(rejected(), result)
        }

    @Test
    fun simulate_whenTheServerFails_returnsServerError() =
        runTest {
            respondProblem(500, """{"title":"An error occurred","status":500}""")

            val result = repository().simulate(request)

            assertEquals(SimulationResult.Failure(SimulationFailure.ServerError), result)
        }

    @Test
    fun simulate_whenTheBodyIsNotTheContract_returnsServerError() =
        runTest {
            respond(200, """{"unexpected":true}""")

            val result = repository().simulate(request)

            assertEquals(SimulationResult.Failure(SimulationFailure.ServerError), result)
        }

    @Test
    fun simulate_whenTheScheduleIsEmpty_returnsServerError() =
        runTest {
            val schedule = Regex("\"amortizationSchedule\"\\s*:\\s*\\[.*]", RegexOption.DOT_MATCHES_ALL)
            respond(200, withBonusBody().replace(schedule, "\"amortizationSchedule\": []"))

            val result = repository().simulate(request)

            assertEquals(SimulationResult.Failure(SimulationFailure.ServerError), result)
        }

    @Test
    fun simulate_whenTheServerIsDown_returnsNoConnection() =
        runTest {
            val deadUrl = server.url("/")
            server.shutdown()

            val result = repository(deadUrl).simulate(request)

            assertEquals(SimulationResult.Failure(SimulationFailure.NoConnection), result)
        }
}
