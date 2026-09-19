package com.auracode.hiposim.feature.simulation.ui

import androidx.compose.runtime.Immutable
import com.auracode.hiposim.feature.auth.ui.AccountUi

/** Everything the Results screen draws. Money and rates are already formatted (es-PE). */
@Immutable
data class ResultsUiState(
    val isLoading: Boolean = true,
    val summary: ResultsSummaryUi? = null,
    /** The signed in user, or null for a guest. */
    val account: AccountUi? = null,
    val isUnlockSheetVisible: Boolean = false,
    val isAccountSheetVisible: Boolean = false,
    /** One-off message request, cleared by [ResultsViewModel.onComingSoonShown]. */
    val showComingSoon: Boolean = false,
) {
    val isAuthenticated: Boolean get() = account != null
}

@Immutable
data class ResultsSummaryUi(
    val propertyPrice: String,
    val downPayment: String,
    val downPaymentPercent: String,
    val goodPayerBonus: String,
    val netLoan: String,
    val termYears: Int,
    val termMonths: Int,
    val monthlyPayment: String,
    val monthlyInsurance: String,
    val tcea: String,
    val npv: String,
    val irr: String,
    val composition: PaymentCompositionUi,
    val schedule: List<InstallmentUi>,
)

/** Share of the first installment going to interest, principal and insurance. The three add up to 100. */
@Immutable
data class PaymentCompositionUi(
    val interestPercent: Int,
    val principalPercent: Int,
    val insurancePercent: Int,
)

@Immutable
data class InstallmentUi(
    val number: Int,
    val payment: String,
    val principal: String,
    val interest: String,
    val insurance: String,
    val remainingBalance: String,
)
