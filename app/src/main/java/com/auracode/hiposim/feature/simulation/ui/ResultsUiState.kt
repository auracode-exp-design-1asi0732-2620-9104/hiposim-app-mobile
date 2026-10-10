package com.auracode.hiposim.feature.simulation.ui

import androidx.compose.runtime.Immutable
import com.auracode.hiposim.feature.auth.ui.AccountUi
import com.auracode.hiposim.feature.simulation.domain.BonusStatus

/** Everything the Results screen draws. Money and rates are already formatted (es-PE). */
@Immutable
data class ResultsUiState(
    /** True only until the current simulation of the session has been read. */
    val isLoading: Boolean = true,
    /** The simulation calculated from the form, or null while loading or when [isSimulationMissing]. */
    val summary: ResultsSummaryUi? = null,
    /** There is no simulation to show, for example after the system recreated the app. The buyer can run it again. */
    val isSimulationMissing: Boolean = false,
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
    val bonusStatus: BonusStatus,
    val goodPayerBonus: String,
    val netLoan: String,
    val termYears: Int,
    val termMonths: Int,
    val monthlyPayment: String,
    val monthlyInsurance: String,
    val totalPaid: String,
    val totalInterest: String,
    val tcea: String,
    val npv: String,
    /** Annual rate the NPV was discounted at, sent by the API. */
    val npvDiscountRate: String,
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
