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

    private fun idsOf(viewModel: ScenariosViewModel) =
        viewModel.uiState.value.scenarios
            .map { it.id }

    @Test
    fun withTwoOrMoreScenarios_pickingIsAvailable_butComparingNeedsTwoPicked() {
        signInAndSave("Uno", "Dos", "Tres")
        val viewModel = viewModel()
        val ids = idsOf(viewModel)

        assertTrue(viewModel.uiState.value.canPick)
        assertFalse(viewModel.uiState.value.canCompare)

        viewModel.onToggleSelection(ids[0])
        assertFalse(viewModel.uiState.value.canCompare)

        viewModel.onToggleSelection(ids[1])
        assertTrue(viewModel.uiState.value.canCompare)
        assertEquals(listOf(ids[0], ids[1]), viewModel.uiState.value.selectedIds)
    }

    @Test
    fun aSingleScenario_cannotBePickedToCompare() {
        signInAndSave("Uno")

        assertFalse(viewModel().uiState.value.canPick)
    }

    @Test
    fun atMostThreeCanBePicked_andAPickedOneCanBeDropped() {
        signInAndSave("Uno", "Dos", "Tres", "Cuatro")
        val viewModel = viewModel()
        val ids = idsOf(viewModel)

        ids.forEach(viewModel::onToggleSelection)

        assertEquals(ids.take(3), viewModel.uiState.value.selectedIds)
        assertTrue(viewModel.uiState.value.isSelectionFull)

        viewModel.onToggleSelection(ids[0])
        assertEquals(ids.subList(1, 3), viewModel.uiState.value.selectedIds)
        assertFalse(viewModel.uiState.value.isSelectionFull)
    }

    @Test
    fun anUnknownIdIsIgnored() {
        signInAndSave("Uno", "Dos")
        val viewModel = viewModel()

        viewModel.onToggleSelection("no-such-id")

        assertTrue(
            viewModel.uiState.value.selectedIds
                .isEmpty(),
        )
    }

    @Test
    fun theSelection_survivesReadingTheListAgain_exceptForScenariosThatAreGone() {
        signInAndSave("Uno", "Dos")
        val viewModel = viewModel()
        val ids = idsOf(viewModel)
        ids.forEach(viewModel::onToggleSelection)
        val remaining = scenariosService.onGet

        viewModel.onRetry()
        assertEquals(ids, viewModel.uiState.value.selectedIds)

        scenariosService.onGet = { remaining().filter { it.id == ids[0] } }
        viewModel.onRetry()
        assertEquals(listOf(ids[0]), viewModel.uiState.value.selectedIds)
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
