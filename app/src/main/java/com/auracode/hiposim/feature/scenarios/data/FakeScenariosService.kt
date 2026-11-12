package com.auracode.hiposim.feature.scenarios.data

import com.auracode.hiposim.core.network.problemException
import com.auracode.hiposim.core.network.validationProblemException
import com.auracode.hiposim.feature.simulation.data.FakeSimulationStore
import kotlinx.coroutines.delay
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Stand-in for /api/scenarios (`BuildConfig.USE_FAKE_API`). It keeps the scenarios in memory for the life of the
 * app and takes the figures of each one from the simulations the fake API calculated ([FakeSimulationStore]),
 * like the backend does with its stored simulations. It fails like the server for an empty name (400) and an
 * unknown simulation (404).
 */
class FakeScenariosService(
    private val latencyMillis: Long = 0,
    private val now: () -> OffsetDateTime = { OffsetDateTime.now(ZoneOffset.UTC) },
) : ScenariosService {
    private val scenarios = CopyOnWriteArrayList<ScenarioDto>()

    override suspend fun createScenario(request: CreateScenarioRequestDto): ScenarioDto {
        if (latencyMillis > 0) delay(latencyMillis)
        val name = request.name.trim()
        if (name.isEmpty() || name.length > MAX_NAME_LENGTH) {
            throw validationProblemException(
                mapOf("name" to listOf("The name is required and cannot exceed $MAX_NAME_LENGTH characters.")),
            )
        }
        val (input, result) =
            FakeSimulationStore.get(request.simulationId)
                ?: throw problemException(NOT_FOUND, "Simulation not found.")
        val scenario =
            ScenarioDto(
                id = UUID.randomUUID().toString(),
                name = name,
                simulationId = request.simulationId,
                createdAt = now().toString(),
                propertyPrice = input.propertyPrice,
                downPayment = input.downPayment,
                annualEffectiveRate = input.annualEffectiveRate,
                termInMonths = input.termInMonths,
                gracePeriodMonths = input.gracePeriodMonths ?: 0,
                graceType = input.graceType ?: "none",
                applyGoodPayerBonus = input.applyGoodPayerBonus,
                goodPayerBonus = result.benefit.appliedAmount,
                financedAmount = result.financedAmount,
                monthlyInstallment = result.monthlyInstallment,
                totalPaid = result.totalPaid,
                totalInterest = result.totalInterest,
                tcea = result.tcea,
                npv = result.npv,
                irr = result.irr,
            )
        scenarios.add(0, scenario)
        return scenario
    }

    override suspend fun getScenarios(): List<ScenarioDto> {
        if (latencyMillis > 0) delay(latencyMillis)
        return scenarios.toList()
    }

    companion object {
        /** Simulated network delay of the app, so the loading state can be seen. */
        const val DEFAULT_LATENCY_MILLIS = 400L
        const val MAX_NAME_LENGTH = 60
        private const val NOT_FOUND = 404
    }
}
