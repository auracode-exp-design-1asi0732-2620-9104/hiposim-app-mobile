package com.auracode.hiposim.feature.simulation.data

import com.auracode.hiposim.core.network.AuthInterceptor
import com.auracode.hiposim.core.network.TokenStore
import com.auracode.hiposim.di.NetworkModule
import com.auracode.hiposim.feature.simulation.domain.BonusStatus
import com.auracode.hiposim.feature.simulation.domain.SimulationFailure
import com.auracode.hiposim.feature.simulation.domain.SimulationField
import com.auracode.hiposim.feature.simulation.domain.SimulationParams
import com.auracode.hiposim.feature.simulation.domain.SimulationResult
import com.auracode.hiposim.testing.contractExampleParams
import com.auracode.hiposim.testing.contractResource
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.math.BigDecimal
import java.time.LocalDate

/** The repository against a local server that answers with the examples of the contract (openapi.v0.yaml). */
class DefaultSimulationRepositoryTest {
    private val server = MockWebServer()
    private val tokenStore = TokenStore()
    private lateinit var repository: DefaultSimulationRepository

    @Before
    fun setUp() {
        server.start()
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokenStore)).build()
        val service =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .client(client)
                .addConverterFactory(NetworkModule.provideJson().asConverterFactory("application/json".toMediaType()))
                .build()
                .create(SimulationService::class.java)
        repository = DefaultSimulationRepository(service)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun enqueue(
        code: Int,
        body: String = "",
        contentType: String = "application/json",
    ) = server.enqueue(
        MockResponse
            .Builder()
            .code(code)
            .addHeader("Content-Type", contentType)
            .body(body)
            .build(),
    )

    private fun sentBody(): JsonObject = Json.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject

    @Test
    fun simulate_postsTheRequestOfTheContract() =
        runTest {
            enqueue(200, WITH_BONUS_RESPONSE)

            repository.simulate(contractExampleParams)

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/simulations", request.url.encodedPath)
            assertNull(request.headers[AuthInterceptor.AUTHORIZATION])
            val body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
            assertEquals(
                setOf("propertyPrice", "downPayment", "annualEffectiveRate", "termInMonths", "applyGoodPayerBonus"),
                body.keys,
            )
            assertEquals(280_000.0, body.getValue("propertyPrice").jsonPrimitive.double, 0.0)
            assertEquals(42_000.0, body.getValue("downPayment").jsonPrimitive.double, 0.0)
            assertEquals(240, body.getValue("termInMonths").jsonPrimitive.int)
            assertTrue(body.getValue("applyGoodPayerBonus").jsonPrimitive.boolean)
        }

    @Test
    fun simulate_sendsTheRateAsAFractionNeverAsAPercentage() =
        runTest {
            enqueue(200, WITH_BONUS_RESPONSE)

            repository.simulate(contractExampleParams)

            assertEquals(0.085, sentBody().getValue("annualEffectiveRate").jsonPrimitive.double, 0.0)
        }

    @Test
    fun simulate_withASession_sendsTheBearerToken() =
        runTest {
            enqueue(200, WITH_BONUS_RESPONSE)
            tokenStore.token = "abc.def.ghi"

            repository.simulate(contractExampleParams)

            assertEquals("Bearer abc.def.ghi", server.takeRequest().headers[AuthInterceptor.AUTHORIZATION])
        }

    @Test
    fun simulate_mapsTheResponseAndKeepsItAsTheCurrentSimulation() =
        runTest {
            enqueue(200, WITH_BONUS_RESPONSE)

            val result = repository.simulate(contractExampleParams)

            val simulation = (result as SimulationResult.Success).simulation
            assertSame(simulation, repository.currentSimulation.value)
            assertEquals("3f9b8c2e-6a1d-4c58-9e0f-7d2b5a1c4e90", simulation.id)
            assertEquals(BigDecimal("280000"), simulation.propertyPrice)
            assertEquals(BigDecimal("42000"), simulation.downPayment)
            assertEquals(240, simulation.termMonths)
            assertEquals(20, simulation.termYears)
            assertEquals(BigDecimal("219800.00"), simulation.netLoan)
            assertEquals(BigDecimal("1863.99"), simulation.monthlyPayment)
            assertEquals(BigDecimal("447357.60"), simulation.totalPaid)
            assertEquals(BigDecimal("227557.97"), simulation.totalInterest)
            assertEquals(BigDecimal("0.085000"), simulation.tcea)
            assertEquals(BigDecimal("0.085000"), simulation.irr)
            assertEquals(BigDecimal("-20791.75"), simulation.npv)
            assertEquals(BigDecimal("18200.00"), simulation.goodPayerBonus)
            assertEquals(BonusStatus.Applied, simulation.bonusStatus)
            assertEquals(BigDecimal("0.100000"), simulation.assumptions.annualDiscountRate)
            assertEquals(BigDecimal("0.00"), simulation.monthlyInsurance)

            val first = simulation.schedule.first()
            assertEquals(1, first.number)
            assertEquals(LocalDate.parse("2026-11-01"), first.paymentDate)
            assertEquals(BigDecimal("219800.00"), first.openingBalance)
            assertEquals(BigDecimal("364.63"), first.principal)
            assertEquals(BigDecimal("1499.36"), first.interest)
            assertEquals(BigDecimal("1863.99"), first.payment)
            assertEquals(BigDecimal("219435.37"), first.remainingBalance)
        }

    @Test
    fun bonusRequestedButNotEligible_isASuccessNotAnError() =
        runTest {
            enqueue(200, OUT_OF_RANGE_RESPONSE)
            val params =
                SimulationParams(
                    propertyPrice = BigDecimal("450000"),
                    downPayment = BigDecimal("90000"),
                    termYears = 20,
                    applyGoodPayerBonus = true,
                )

            val simulation = (repository.simulate(params) as SimulationResult.Success).simulation

            assertEquals(BonusStatus.NotEligible, simulation.bonusStatus)
            assertEquals(BigDecimal("0.00"), simulation.goodPayerBonus)
            assertEquals(BigDecimal("360000.00"), simulation.netLoan)
        }

    @Test
    fun validationProblem_returnsTheFieldErrorsAndKeepsThePreviousSimulation() =
        runTest {
            enqueue(200, WITH_BONUS_RESPONSE)
            enqueue(
                400,
                contractResource("problem-down-payment-too-low.json"),
                contentType = "application/problem+json",
            )
            val previous = (repository.simulate(contractExampleParams) as SimulationResult.Success).simulation

            val result = repository.simulate(contractExampleParams.copy(downPayment = BigDecimal("1000")))

            assertEquals(
                SimulationResult.Failure(
                    SimulationFailure.Rejected(
                        mapOf(
                            SimulationField.DownPayment to
                                "The down payment must be at least 10% of the property price.",
                        ),
                    ),
                ),
                result,
            )
            assertSame(previous, repository.currentSimulation.value)
        }

    @Test
    fun serverErrorAndNoConnection_areFailures() =
        runTest {
            enqueue(503)
            assertEquals(
                SimulationResult.Failure(SimulationFailure.ServiceUnavailable),
                repository.simulate(contractExampleParams),
            )

            server.close()
            assertEquals(
                SimulationResult.Failure(SimulationFailure.NoConnection),
                repository.simulate(contractExampleParams),
            )
            assertNull(repository.currentSimulation.value)
        }

    @Test
    fun responseThatDoesNotMatchTheContract_isUnexpected() =
        runTest {
            enqueue(200, """{"simulationId":"x"}""")

            val result = repository.simulate(contractExampleParams)

            assertEquals(SimulationResult.Failure(SimulationFailure.Unexpected), result)
        }

    @Test
    fun responseWithoutScheduleOrWithABadDate_isUnexpectedAndNotStored() =
        runTest {
            val withoutSchedule =
                WITH_BONUS_RESPONSE.substringBefore("\"amortizationSchedule\"") + "\"amortizationSchedule\": []}"
            enqueue(200, withoutSchedule)
            enqueue(200, WITH_BONUS_RESPONSE.replace("2026-11-01", "next month"))

            assertEquals(
                SimulationResult.Failure(SimulationFailure.Unexpected),
                repository.simulate(contractExampleParams),
            )
            assertEquals(
                SimulationResult.Failure(SimulationFailure.Unexpected),
                repository.simulate(contractExampleParams),
            )
            assertNull(repository.currentSimulation.value)
        }

    @Test
    fun rejectionAboutAPropertyTheFormDoesNotShow_hasNoFieldMessages() =
        runTest {
            enqueue(
                400,
                """{"title":"Invalid.","status":400,"errors":{"annualEffectiveRate":["Must be a fraction."]}}""",
                contentType = "application/problem+json",
            )

            assertEquals(
                SimulationResult.Failure(SimulationFailure.Rejected(emptyMap())),
                repository.simulate(contractExampleParams),
            )
        }

    @Test
    fun otherClientErrors_areUnexpected() =
        runTest {
            enqueue(401, """{"title":"Unauthorized.","status":401}""", contentType = "application/problem+json")

            assertEquals(
                SimulationResult.Failure(SimulationFailure.Unexpected),
                repository.simulate(contractExampleParams),
            )
        }

    private companion object {
        // Examples of POST /api/simulations in openapi.v0.yaml (test/resources/contract).
        val WITH_BONUS_RESPONSE = contractResource("simulation-response-with-bonus.json")
        val OUT_OF_RANGE_RESPONSE = contractResource("simulation-response-out-of-range.json")
    }
}
