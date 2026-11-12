package com.auracode.hiposim.feature.scenarios.data

import com.auracode.hiposim.core.network.ApiError
import com.auracode.hiposim.core.network.ApiResult
import com.auracode.hiposim.core.network.safeApiCall
import com.auracode.hiposim.feature.scenarios.domain.SaveScenarioResult
import com.auracode.hiposim.feature.scenarios.domain.SavedScenario
import com.auracode.hiposim.feature.scenarios.domain.ScenarioFailure
import com.auracode.hiposim.feature.scenarios.domain.ScenariosRepository
import com.auracode.hiposim.feature.scenarios.domain.ScenariosResult
import com.auracode.hiposim.feature.simulation.domain.GraceType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** The only [ScenariosRepository]. Real or in-memory answers are decided by the service that Hilt provides. */
@Singleton
class DefaultScenariosRepository
    @Inject
    constructor(
        private val service: ScenariosService,
    ) : ScenariosRepository {
        override suspend fun save(
            name: String,
            simulationId: String,
        ): SaveScenarioResult {
            val request = CreateScenarioRequestDto(name, simulationId)
            // The mapping runs inside the call so an unreadable date is a failure, not a crash.
            return when (val response = safeApiCall { service.createScenario(request).toDomain() }) {
                is ApiResult.Success -> SaveScenarioResult.Success(response.data)
                is ApiResult.Failure -> SaveScenarioResult.Failure(response.error.toScenarioFailure())
            }
        }

        override suspend fun getScenarios(): ScenariosResult =
            when (val response = safeApiCall { service.getScenarios().map { it.toDomain() } }) {
                is ApiResult.Success -> ScenariosResult.Success(response.data)
                is ApiResult.Failure -> ScenariosResult.Failure(response.error.toScenarioFailure())
            }

        private fun ApiError.toScenarioFailure(): ScenarioFailure =
            when (this) {
                ApiError.NoConnection -> ScenarioFailure.NoConnection
                is ApiError.Validation -> ScenarioFailure.NameInvalid
                is ApiError.Unauthorized -> ScenarioFailure.SessionExpired
                is ApiError.Client ->
                    when (code) {
                        HTTP_FORBIDDEN -> ScenarioFailure.SessionExpired
                        HTTP_NOT_FOUND -> ScenarioFailure.NotFound
                        else -> ScenarioFailure.Unexpected
                    }
                is ApiError.Server -> ScenarioFailure.ServiceUnavailable
                is ApiError.Conflict, is ApiError.Unexpected -> ScenarioFailure.Unexpected
            }

        private companion object {
            const val HTTP_FORBIDDEN = 403
            const val HTTP_NOT_FOUND = 404
        }
    }

private const val MONEY_SCALE = 2
private const val RATE_SCALE = 6

/**
 * Contract names to model names: `financedAmount` is [SavedScenario.netLoan] and `monthlyInstallment` is
 * [SavedScenario.monthlyPayment], as in the simulation mapping.
 */
fun ScenarioDto.toDomain(): SavedScenario =
    SavedScenario(
        id = id,
        name = name,
        simulationId = simulationId,
        createdAt = OffsetDateTime.parse(createdAt).toInstant(),
        propertyPrice = propertyPrice?.toMoney(),
        downPayment = downPayment?.toMoney(),
        termMonths = termInMonths,
        graceType = GraceType.fromApiValue(graceType),
        graceMonths = gracePeriodMonths ?: 0,
        bonusRequested = applyGoodPayerBonus,
        goodPayerBonus = goodPayerBonus?.toMoney(),
        netLoan = financedAmount?.toMoney(),
        monthlyPayment = monthlyInstallment?.toMoney(),
        totalPaid = totalPaid?.toMoney(),
        tcea = tcea?.let { BigDecimal.valueOf(it).setScale(RATE_SCALE, RoundingMode.HALF_UP) },
    )

private fun Double.toMoney(): BigDecimal = BigDecimal.valueOf(this).setScale(MONEY_SCALE, RoundingMode.HALF_UP)
