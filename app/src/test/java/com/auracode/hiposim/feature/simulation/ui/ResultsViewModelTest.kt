package com.auracode.hiposim.feature.simulation.ui

import com.auracode.hiposim.core.navigation.MainDestination
import com.auracode.hiposim.feature.auth.data.FakeAuthRepository
import com.auracode.hiposim.feature.auth.domain.ObserveSessionUseCase
import com.auracode.hiposim.feature.auth.domain.RegistrationForm
import com.auracode.hiposim.feature.auth.domain.SignOutUseCase
import com.auracode.hiposim.feature.simulation.data.FakeSimulationRepository
import com.auracode.hiposim.feature.simulation.domain.GetLoanSimulationUseCase
import com.auracode.hiposim.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ResultsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: ResultsViewModel

    @Before
    fun setUp() {
        authRepository = FakeAuthRepository()
        viewModel =
            ResultsViewModel(
                getLoanSimulation = GetLoanSimulationUseCase(FakeSimulationRepository()),
                observeSession = ObserveSessionUseCase(authRepository),
                signOut = SignOutUseCase(authRepository),
            )
    }

    private suspend fun signIn() {
        authRepository.register(
            RegistrationForm(
                fullName = "Carlos Mendoza",
                email = "carlos@ejemplo.com",
                phone = "987654321",
                password = "secret123",
                consentAccepted = true,
            ),
        )
    }

    @Test
    fun init_loadsSimulationWithLandingFigures() {
        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertFalse(state.isUnlockSheetVisible)
        assertFalse(state.isAuthenticated)
        val summary = requireNotNull(state.summary)
        assertEquals("S/ 280,000", summary.propertyPrice)
        assertEquals("S/ 42,000", summary.downPayment)
        assertEquals("15%", summary.downPaymentPercent)
        assertEquals("S/ 18,200", summary.goodPayerBonus)
        assertEquals("S/ 219,800", summary.netLoan)
        assertEquals(20, summary.termYears)
        assertEquals(240, summary.termMonths)
        assertEquals("S/ 1,909", summary.monthlyPayment)
        assertEquals("8.85%", summary.tcea)
    }

    @Test
    fun init_formatsScheduleRows() {
        val schedule = requireNotNull(viewModel.uiState.value.summary).schedule

        assertEquals(4, schedule.size)
        val first = schedule.first()
        assertEquals(1, first.number)
        assertEquals("S/ 1,909.00", first.payment)
        assertEquals("S/ 385.49", first.principal)
        assertEquals("S/ 1,417.71", first.interest)
        assertEquals("S/ 105.80", first.insurance)
        assertEquals("S/ 219,414.51", first.remainingBalance)
    }

    @Test
    fun init_compositionAddsUpToOneHundred() {
        val composition = requireNotNull(viewModel.uiState.value.summary).composition

        assertEquals(
            100,
            composition.interestPercent + composition.principalPercent + composition.insurancePercent,
        )
        assertEquals(74, composition.interestPercent)
        assertEquals(20, composition.principalPercent)
        assertEquals(6, composition.insurancePercent)
    }

    @Test
    fun guest_sendQuote_opensUnlockSheet() {
        viewModel.onSendQuoteClick()

        assertTrue(viewModel.uiState.value.isUnlockSheetVisible)
        assertFalse(viewModel.uiState.value.showComingSoon)
    }

    @Test
    fun guest_lockedDestinations_openUnlockSheet() {
        listOf(MainDestination.Realtors, MainDestination.History, MainDestination.Profile).forEach { destination ->
            viewModel.onUnlockSheetDismiss()

            viewModel.onDestinationClick(destination)

            assertTrue(destination.name, viewModel.uiState.value.isUnlockSheetVisible)
        }
    }

    @Test
    fun simulateDestination_doesNothing() {
        viewModel.onDestinationClick(MainDestination.Simulate)

        assertFalse(viewModel.uiState.value.isUnlockSheetVisible)
        assertFalse(viewModel.uiState.value.showComingSoon)
    }

    @Test
    fun dismiss_hidesUnlockSheet() {
        viewModel.onSendQuoteClick()
        viewModel.onUnlockSheetDismiss()

        assertFalse(viewModel.uiState.value.isUnlockSheetVisible)
    }

    @Test
    fun downloadPdf_showsComingSoonUntilConsumed() {
        viewModel.onDownloadPdfClick()
        assertTrue(viewModel.uiState.value.showComingSoon)

        viewModel.onComingSoonShown()
        assertFalse(viewModel.uiState.value.showComingSoon)
    }

    @Test
    fun signingIn_exposesTheAccountAndClosesTheUnlockSheet() =
        runTest {
            viewModel.onSendQuoteClick()

            signIn()

            val state = viewModel.uiState.value
            assertTrue(state.isAuthenticated)
            assertEquals("Carlos Mendoza", state.account?.name)
            assertEquals("carlos@ejemplo.com", state.account?.email)
            assertFalse(state.isUnlockSheetVisible)
        }

    @Test
    fun signedIn_sendQuoteAndAgenciesShowComingSoonInsteadOfTheSheet() =
        runTest {
            signIn()

            viewModel.onSendQuoteClick()
            assertTrue(viewModel.uiState.value.showComingSoon)
            assertFalse(viewModel.uiState.value.isUnlockSheetVisible)

            viewModel.onComingSoonShown()
            viewModel.onDestinationClick(MainDestination.Realtors)
            assertTrue(viewModel.uiState.value.showComingSoon)
            assertFalse(viewModel.uiState.value.isUnlockSheetVisible)
        }

    @Test
    fun signedIn_profileOpensAccountSheet_andSignOutReturnsToGuest() =
        runTest {
            signIn()

            viewModel.onDestinationClick(MainDestination.Profile)
            assertTrue(viewModel.uiState.value.isAccountSheetVisible)

            viewModel.onSignOutClick()

            val state = viewModel.uiState.value
            assertFalse(state.isAccountSheetVisible)
            assertFalse(state.isAuthenticated)
            assertNull(state.account)
        }
}
