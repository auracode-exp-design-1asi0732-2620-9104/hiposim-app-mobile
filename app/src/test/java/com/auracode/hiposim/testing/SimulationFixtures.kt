package com.auracode.hiposim.testing

import com.auracode.hiposim.feature.simulation.data.DefaultSimulationRepository
import com.auracode.hiposim.feature.simulation.data.FakeSimulationService
import com.auracode.hiposim.feature.simulation.data.SimulationRequestDto
import com.auracode.hiposim.feature.simulation.data.SimulationResponseDto
import com.auracode.hiposim.feature.simulation.data.SimulationService
import com.auracode.hiposim.feature.simulation.domain.SimulationParams
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.math.BigDecimal

/** Request of the `withBonus` example of the contract: S/ 280,000, 15% down payment, 20 years, bonus applied. */
val contractExampleParams =
    SimulationParams(
        propertyPrice = BigDecimal("280000"),
        downPayment = BigDecimal("42000"),
        termYears = 20,
        applyGoodPayerBonus = true,
    )

/** A repository backed by the in-memory service, without latency. */
fun fakeBackedSimulationRepository() = DefaultSimulationRepository(FakeSimulationService())

/** A repository that already holds the contract example as its current simulation. */
suspend fun simulationRepositoryWithContractExample(): DefaultSimulationRepository =
    fakeBackedSimulationRepository().also { it.simulate(contractExampleParams) }

/** A service whose answer each test decides, to reach the failure paths. */
class StubSimulationService(
    var answer: suspend (
        SimulationRequestDto,
    ) -> SimulationResponseDto = { FakeSimulationService().createSimulation(it) },
) : SimulationService {
    val requests = mutableListOf<SimulationRequestDto>()

    override suspend fun createSimulation(request: SimulationRequestDto): SimulationResponseDto {
        requests += request
        return answer(request)
    }
}

fun httpException(
    code: Int,
    body: String = "",
) = HttpException(Response.error<Unit>(code, body.toResponseBody("application/problem+json".toMediaType())))
