package com.testapp.renttracker.service

import com.testapp.renttracker.repo.TenantBalanceRepository
import com.testapp.renttracker.repo.TenantMonthlyChargeRepository

class CarryForwardService(
    private val balanceRepo: TenantBalanceRepository,
    private val chargeRepo: TenantMonthlyChargeRepository,
) {
    fun applyCarryForward(nextMonthId: String, previousMonthId: String) {
        val balances = balanceRepo.getAllBalancesForMonth(previousMonthId).associateBy { it.tenantId }
        val existingCharges = chargeRepo.getChargesByMonth(nextMonthId)

        val updated = existingCharges.map { charge ->
            val adjustment = balances[charge.tenantId]?.balanceAmount ?: charge.adjustmentAmount
            charge.copy(
                adjustmentAmount = adjustment,
                totalDue = charge.rentAmount.add(charge.electricityShare).subtract(adjustment),
            )
        }
        chargeRepo.replaceMonthCharges(nextMonthId, updated)
    }
}
