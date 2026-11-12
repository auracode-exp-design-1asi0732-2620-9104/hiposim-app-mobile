package com.auracode.hiposim.feature.scenarios.ui

import androidx.lifecycle.SavedStateHandle
import com.auracode.hiposim.core.navigation.Route
import com.auracode.hiposim.core.network.TokenStore
import com.auracode.hiposim.feature.auth.data.DefaultAuthRepository
import com.auracode.hiposim.feature.auth.data.FakeAuthService
import com.auracode.hiposim.feature.scenarios.data.DefaultScenariosRepository
import com.auracode.hiposim.feature.scenarios.domain.CompareScenariosUseCase
import com.auracode.hiposim.feature.scenarios.domain.GetScenariosUseCase
import com.auracode.hiposim.feature.scenarios.domain.SaveScenarioUseCase
import com.auracode.hiposim.feature.scenarios.domain.ScenarioFailure
import com.auracode.hiposim.feature.scenarios.domain.ScenariosResult
import com.auracode.hiposim.feature.simulation.data.DefaultSimulationRepository
import com.auracode.hiposim.testing.MainDispatcherRule
import com.auracode.hiposim.testing.StubScenariosService
import com.auracode.hiposim.testing.StubSimulationService
import com.auracode.hiposim.testing.contractExampleParams
import com.auracode.hiposim.testing.validRegistrationForm
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/** US07: the comparison screen reads the picked ids from its route. */
class CompareScenariosViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tokenStore = TokenStore()
    private val authRepository = DefaultAuthRepository(FakeAuthService(), tokenStore)
    private val simulationRepository = DefaultSimulationRepository(StubSimulationService(), tokenStore)
    private val scenariosService = StubScenariosService()
    private val scenariosRepository = DefaultScenariosRepository(scenariosService)
    private val getScenarios = GetScenariosUseCase(scenariosRepository, authRepository)

    private fun savedIds(vararg names: String): List<String> =
        runBlocking {
            authRepository.register(validRegistrationForm)
            simulationRepository.simulate(contractExampleParams)
            val save = SaveScenarioUseCase(scenariosRepository, simulationRepository, authRepository)
            names.forEach { save(it) }
            val byName = (getScenarios() as ScenariosResult.Success).scenarios.associateBy { it.name }
            names.map { byName.getValue(it).id }
        }

    private fun viewModel(ids: List<String>) =
        CompareScenariosViewModel(
            SavedStateHandle(mapOf(Route.CompareScenarios.ARG_IDS to Route.CompareScenarios.of(ids).ids)),
            CompareScenariosUseCase(getScenarios),
        )

    @Test
    fun pickedScenarios_becomeColumnsWithThreeHighlightedRows() {
        val ids = savedIds("Uno", "Dos")

        val state = viewModel(ids).uiState.value

        assertFalse(state.isLoading)
        assertNull(state.failure)
        val comparison = requireNotNull(state.comparison)
        assertEquals(ids.size, comparison.scenarios.size)
        assertEquals(3, comparison.metrics.size)
        assertEquals(
            ids.size,
            comparison.metrics
                .first()
                .cells.size,
        )
    }

    @Test
    fun routeWithOneId_isASelectionFailure() {
        val ids = savedIds("Uno", "Dos")

        val state = viewModel(ids.take(1)).uiState.value

        assertEquals(ScenarioFailure.SelectionInvalid, state.failure)
        assertNull(state.comparison)
    }

    @Test
    fun failure_canBeRetried() {
        val ids = savedIds("Uno", "Dos")
        val working = scenariosService.onGet
        scenariosService.onGet = { throw IOException("offline") }
        val viewModel = viewModel(ids)
        assertEquals(ScenarioFailure.NoConnection, viewModel.uiState.value.failure)

        scenariosService.onGet = working
        viewModel.onRetry()

        assertNull(viewModel.uiState.value.failure)
        assertNotNull(viewModel.uiState.value.comparison)
    }
}
