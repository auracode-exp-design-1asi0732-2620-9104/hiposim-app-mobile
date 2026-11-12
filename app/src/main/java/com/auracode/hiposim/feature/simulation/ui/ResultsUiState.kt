package com.auracode.hiposim.feature.simulation.ui

import androidx.compose.runtime.Immutable
import com.auracode.hiposim.feature.auth.ui.AccountUi
import com.auracode.hiposim.feature.report.domain.ReportFailure
import com.auracode.hiposim.feature.report.domain.ReportFile
import com.auracode.hiposim.feature.scenarios.domain.ScenarioFailure
import com.auracode.hiposim.feature.simulation.domain.BonusStatus
import com.auracode.hiposim.feature.simulation.domain.GraceType

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
    /** The sheet to choose an agency and authorize sending the simulation (signed in buyers only). */
    val isSendSheetVisible: Boolean = false,
    /** The PDF report is being downloaded (US08): the button is disabled and says so. */
    val isExportingPdf: Boolean = false,
    /** One-off: the PDF arrived and must be opened, cleared by [ResultsViewModel.onPdfReportHandled]. */
    val pdfReport: ReportFile? = null,
    /** One-off: why the PDF could not be downloaded, cleared by [ResultsViewModel.onPdfFailureShown]. */
    val pdfFailure: ReportFailure? = null,
    /** The dialog to save the simulation as a named scenario (US06), or null when it is closed. */
    val saveScenario: SaveScenarioUi? = null,
    /** One-off confirmation after saving, cleared by [ResultsViewModel.onScenarioSavedShown]. */
    val scenarioSaved: Boolean = false,
    /** One-off request to open "My scenarios", cleared by [ResultsViewModel.onScenariosNavigated]. */
    val navigateToScenarios: Boolean = false,
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
    /** Grace period of the simulation (US03); [graceMonths] is 0 without grace. */
    val graceType: GraceType = GraceType.None,
    val graceMonths: Int = 0,
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

/** The save scenario dialog (US06). [failure] is shown inside the dialog. */
@Immutable
data class SaveScenarioUi(
    val name: String,
    val isSaving: Boolean = false,
    val failure: ScenarioFailure? = null,
)
