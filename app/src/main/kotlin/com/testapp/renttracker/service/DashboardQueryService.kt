package com.testapp.renttracker.service

import com.testapp.renttracker.model.MonthSummary
import com.testapp.renttracker.repo.PaymentRecordRepository
import com.testapp.renttracker.repo.TenantMonthlyChargeRepository
import java.math.BigDecimal
import java.math.RoundingMode

class DashboardQueryService(
    private val chargeRepo: TenantMonthlyChargeRepository,
    private val paymentRepo: PaymentRecordRepository,
) {
    fun getMonthSummary(monthId: String): MonthSummary {
        val charges = chargeRepo.getChargesByMonth(monthId)
        val payments = paymentRepo.getPaymentsByMonth(monthId)

        val totalExpected = charges.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.totalDue) }.setScale(2, RoundingMode.HALF_UP)
        val totalPaid = payments.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.amountPaid) }.setScale(2, RoundingMode.HALF_UP)
        val totalPending = totalExpected.subtract(totalPaid).setScale(2, RoundingMode.HALF_UP)

        val paidAndUnpaid = charges.associate { charge ->
            val paidForTenant = payments
                .filter { it.tenantId == charge.tenantId }
                .fold(BigDecimal.ZERO) { acc, p -> acc.add(p.amountPaid) }
                .setScale(2, RoundingMode.HALF_UP)
            charge.tenantId to charge.totalDue.subtract(paidForTenant)
        }

        val unpaidTenantIds = paidAndUnpaid.filterValues { it > BigDecimal.ZERO }.keys.toList()
        val paidTenantCount = paidAndUnpaid.size - unpaidTenantIds.size

        return MonthSummary(
            monthId = monthId,
            totalExpected = totalExpected,
            totalPaid = totalPaid,
            totalPending = totalPending,
            paidTenantCount = paidTenantCount,
            totalTenantCount = paidAndUnpaid.size,
            unpaidTenantIds = unpaidTenantIds,
        )
    }
}
