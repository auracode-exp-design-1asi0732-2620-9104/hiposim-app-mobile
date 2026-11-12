package com.auracode.hiposim.testing

import com.auracode.hiposim.feature.scenarios.data.CreateScenarioRequestDto
import com.auracode.hiposim.feature.scenarios.data.FakeScenariosService
import com.auracode.hiposim.feature.scenarios.data.ScenarioDto
import com.auracode.hiposim.feature.scenarios.data.ScenariosService

/** A scenarios service whose answers each test decides. It behaves like the fake by default. */
class StubScenariosService : ScenariosService {
    private val fake = FakeScenariosService()
    var onCreate: suspend (CreateScenarioRequestDto) -> ScenarioDto = { fake.createScenario(it) }
    var onGet: suspend () -> List<ScenarioDto> = { fake.getScenarios() }
    val requests = mutableListOf<CreateScenarioRequestDto>()
    var getCalls = 0

    override suspend fun createScenario(request: CreateScenarioRequestDto): ScenarioDto {
        requests += request
        return onCreate(request)
    }

    override suspend fun getScenarios(): List<ScenarioDto> {
        getCalls++
        return onGet()
    }
}
