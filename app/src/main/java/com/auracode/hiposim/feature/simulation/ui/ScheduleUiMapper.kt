package com.auracode.hiposim.feature.simulation.ui

import com.auracode.hiposim.core.util.PeruFormat
import com.auracode.hiposim.feature.simulation.domain.Installment
import java.math.BigDecimal

private const val CENTS = 2

fun List<Installment>.toDetailedScheduleUi(
    initialBalance: BigDecimal,
): List<ScheduleInstallmentUi> {
    var currentBalance = initialBalance

    return map { installment ->
        ScheduleInstallmentUi(
            number = installment.number,
            initialBalance = PeruFormat.soles(currentBalance, CENTS),
            principal = PeruFormat.soles(installment.principal, CENTS),
            interest = PeruFormat.soles(installment.interest, CENTS),
            remainingBalance = PeruFormat.soles(installment.remainingBalance, CENTS),
        ).also {
            currentBalance = installment.remainingBalance
        }
    }
}
