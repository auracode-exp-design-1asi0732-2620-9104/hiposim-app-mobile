package com.auracode.hiposim.feature.scenarios.ui

import com.auracode.hiposim.core.network.TokenStore
import com.auracode.hiposim.feature.auth.data.DefaultAuthRepository
import com.auracode.hiposim.feature.auth.data.FakeAuthService
import com.auracode.hiposim.feature.scenarios.data.DefaultScenariosRepository
import com.auracode.hiposim.feature.scenarios.domain.GetScenariosUseCase
import com.auracode.hiposim.feature.scenarios.domain.SaveScenarioUseCase
import com.auracode.hiposim.feature.scenarios.domain.ScenarioFailure
import com.auracode.hiposim.feature.simulation.data.DefaultSimulationRepository
import com.auracode.hiposim.feature.simulation.domain.GraceType
import com.auracode.hiposim.testing.MainDispatcherRule
import com.auracode.hiposim.testing.StubScenariosService
import com.auracode.hiposim.testing.StubSimulationService
import com.auracode.hiposim.testing.contractExampleParams
import com.auracode.hiposim.testing.validRegistrationForm
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal

/** US06: the "My scenarios" screen. */
class ScenariosViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tokenStore = TokenStore()
    private val authRepository = DefaultAuthRepository(FakeAuthService(), tokenStore)
    private val simulationRepository = DefaultSimulationRepository(StubSimulationService(), tokenStore)
    private val scenariosService = StubScenariosService()
    private val scenariosRepository = DefaultScenariosRepository(scenariosService)

    private fun viewModel() = ScenariosViewModel(GetScenariosUseCase(scenariosRepository, authRepository))

    private fun signInAndSave(vararg names: String) =
        runBlocking {
            authRepository.register(validRegistrationForm)
            simulationRepository.simulate(
                contractExampleParams.copy(
                    annualEffectiveRate = BigDecimal("0.0885"),
                    graceType = GraceType.Partial,
                    graceMonths = 3,
                ),
            )
            val save = SaveScenarioUseCase(scenariosRepository, simulationRepository, authRepository)
            names.forEach { save(it) }
        }

    @Test
    fun savedScenarios_areShownAsCards() {
        signInAndSave("Primero", "Segundo")

        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertNull(state.failure)
        assertEquals(listOf("Segundo", "Primero"), state.scenarios.map { it.name })
        val card = state.scenarios.first()
        assertEquals("S/ 1,918", card.monthlyPayment)
        assertEquals("8.85%", card.tcea)
        assertEquals(20, card.termYears)
        assertEquals("S/ 18,200", card.goodPayerBonus)
        assertEquals(GraceType.Partial, card.graceType)
        assertEquals(3, card.graceMonths)
    }

    @Test
    fun noScenarios_isTheEmptyState() {
        signInAndSave()

        val state = viewModel().uiState.value

        assertTrue(state.isEmpty)
    }

    @Test
    fun guest_seesTheSessionFailure() {
        val state = viewModel().uiState.value

        assertEquals(ScenarioFailure.SessionExpired, state.failure)
        assertFalse(state.isEmpty)
    }

    @Test
    fun failure_canBeRetried() {
        signInAndSave("Primero")
        scenariosService.onGet = { throw IOException("offline") }
        val viewModel = viewModel()
        assertEquals(ScenarioFailure.NoConnection, viewModel.uiState.value.failure)

        scenariosService.onGet = { emptyList() }
        viewModel.onRetry()

        assertNull(viewModel.uiState.value.failure)
        assertTrue(viewModel.uiState.value.isEmpty)
    }
}
