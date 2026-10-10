package com.auracode.hiposim.feature.simulation.ui

import com.auracode.hiposim.core.util.PeruFormat
import com.auracode.hiposim.feature.simulation.domain.Installment
import com.auracode.hiposim.feature.simulation.domain.LoanSimulation

private const val CENTS = 2

/** Every installment of the simulation, not only the preview shown in Results. */
fun LoanSimulation.toScheduleUiState(): ScheduleUiState =
    ScheduleUiState(
        isLoading = false,
        rows = schedule.map { it.toScheduleRowUi() },
        totalPaid = PeruFormat.soles(totalPaid),
        totalInterest = PeruFormat.soles(totalInterest),
    )

private fun Installment.toScheduleRowUi() =
    ScheduleRowUi(
        number = number,
        paymentDate = paymentDate,
        openingBalance = PeruFormat.soles(openingBalance, CENTS),
        payment = PeruFormat.soles(payment, CENTS),
        interest = PeruFormat.soles(interest, CENTS),
        principal = PeruFormat.soles(principal, CENTS),
        insurance = PeruFormat.soles(insurance, CENTS),
        hasInsurance = insurance.signum() > 0,
        closingBalance = PeruFormat.soles(remainingBalance, CENTS),
        isGracePeriod = isGracePeriod,
    )
