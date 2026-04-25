package com.testapp.renttracker.service

import com.testapp.renttracker.error.ErrorCodes
import com.testapp.renttracker.error.ValidationError
import com.testapp.renttracker.logic.BillingCalculator
import com.testapp.renttracker.model.*
import com.testapp.renttracker.repo.*
import java.math.BigDecimal
import java.math.RoundingMode

class BillingMonthService(
    private val tenantRepo: TenantRepository,
    private val monthRepo: BillingMonthRepository,
    private val usageRepo: FlatUsageRepository,
    private val chargeRepo: TenantMonthlyChargeRepository,
    private val balanceRepo: TenantBalanceRepository,
) {
    private val zero = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)

    fun createBillingMonth(monthId: String) {
        if (monthRepo.getMonth(monthId) != null) {
            throw ValidationError(ErrorCodes.MONTH_ALREADY_EXISTS, "Billing month already exists")
        }
        monthRepo.createMonth(
            BillingMonth(
                id = monthId,
                electricityRatePerUnit = zero,
                status = BillingMonthStatus.Draft,
            )
        )
    }

    fun setElectricityRate(monthId: String, ratePerUnit: BigDecimal) {
        requireNonNegative(ratePerUnit, "ratePerUnit")
        val month = requireMonth(monthId)
        monthRepo.updateMonth(month.copy(electricityRatePerUnit = ratePerUnit.setScale(2, RoundingMode.HALF_UP)))
    }

    fun upsertFlatUsage(monthId: String, flatLabel: String, units: BigDecimal) {
        requireNonNegative(units, "units")
        usageRepo.upsertUsage(
            FlatUsage(
                flatLabel = flatLabel,
                billingMonthId = monthId,
                unitsConsumed = units.setScale(2, RoundingMode.HALF_UP),
            )
        )
    }

    fun computeMonthCharges(monthId: String) {
        val month = requireMonth(monthId)
        val activeTenants = tenantRepo.getActiveTenants()
        val activeFlatLabels = activeTenants.map { it.flatLabel }.distinct()

        val usage = usageRepo.getUsageByMonth(monthId)
        val electricityByFlat = BillingCalculator.computeElectricityChargeByFlatLabel(
            flatLabels = activeFlatLabels,
            usages = usage,
            ratePerUnit = month.electricityRatePerUnit,
        )

        val charges = activeTenants.map { tenant ->
            if (tenant.flatLabel.isBlank()) {
                throw ValidationError(ErrorCodes.INVALID_FLAT_TENANT_LINK, "Active tenant must have a flat label")
            }

            val previousMonthId = previousMonth(monthId)
            val previousBalance = previousMonthId?.let { balanceRepo.getBalance(tenant.id, it)?.balanceAmount } ?: BigDecimal.ZERO
            val adjustmentAmount = previousBalance.setScale(2, RoundingMode.HALF_UP)

            val rent = tenant.monthlyRent.setScale(2, RoundingMode.HALF_UP)
            val electricityShare = (electricityByFlat[tenant.flatLabel] ?: BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
            val totalDue = rent.add(electricityShare).subtract(adjustmentAmount).setScale(2, RoundingMode.HALF_UP)

            TenantMonthlyCharge(
                tenantId = tenant.id,
                billingMonthId = monthId,
                rentAmount = rent,
                electricityShare = electricityShare,
                adjustmentAmount = adjustmentAmount,
                totalDue = totalDue,
            )
        }

        chargeRepo.replaceMonthCharges(monthId, charges)
    }

    fun finalizeMonth(monthId: String) {
        val month = requireMonth(monthId)
        if (month.status != BillingMonthStatus.Draft) {
            throw ValidationError(ErrorCodes.MONTH_NOT_DRAFT, "Only Draft month can be finalized")
        }
        val charges = chargeRepo.getChargesByMonth(monthId)
        if (charges.isEmpty()) {
            throw ValidationError(ErrorCodes.MISSING_MONTH_CHARGES, "Compute month charges before finalizing")
        }
        monthRepo.updateMonth(month.copy(status = BillingMonthStatus.Finalized))
    }

    private fun requireMonth(monthId: String): BillingMonth {
        return monthRepo.getMonth(monthId)
            ?: throw ValidationError(ErrorCodes.MONTH_NOT_FOUND, "Billing month does not exist")
    }

    private fun previousMonth(monthId: String): String? {
        val parts = monthId.split("-")
        if (parts.size != 2) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null

        return if (month == 1) {
            "${year - 1}-12"
        } else {
            "%04d-%02d".format(year, month - 1)
        }
    }

    private fun requireNonNegative(value: BigDecimal, field: String) {
        if (value < BigDecimal.ZERO) {
            throw ValidationError(ErrorCodes.INVALID_AMOUNT, "$field must be non-negative", field)
        }
    }
}
