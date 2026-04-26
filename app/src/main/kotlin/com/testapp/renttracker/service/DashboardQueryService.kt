package com.testapp.renttracker.service

import com.testapp.renttracker.model.OverallDashboardSummary
import com.testapp.renttracker.model.OverallTenantDashboardRow
import com.testapp.renttracker.model.PaymentStatus
import com.testapp.renttracker.model.TenantHistoryScreenData
import com.testapp.renttracker.model.TenantMonthlyAmountRow
import com.testapp.renttracker.model.TenantPaymentHistoryRow
import com.testapp.renttracker.repo.PaymentRecordRepository
import com.testapp.renttracker.repo.TenantRepository
import com.testapp.renttracker.repo.TenantMonthlyChargeRepository
import java.math.BigDecimal
import java.math.RoundingMode

class DashboardQueryService(
    private val chargeRepo: TenantMonthlyChargeRepository,
    private val paymentRepo: PaymentRecordRepository,
    private val tenantRepo: TenantRepository,
) {
    fun getOverallSummary(): OverallDashboardSummary {
        val charges = chargeRepo.getAllCharges()
        val payments = paymentRepo.getAllPayments()
        val tenantsById = tenantRepo.getActiveTenants().associateBy { it.id }
        val initialAdjustmentByTenant = charges
            .groupBy { it.tenantId }
            .mapValues { (_, tenantCharges) ->
                tenantCharges.minByOrNull { it.billingMonthId }?.adjustmentAmount ?: BigDecimal.ZERO
            }

        val billedByTenant = charges.groupBy { it.tenantId }.mapValues { (_, tenantCharges) ->
            val baseCharges = tenantCharges.fold(BigDecimal.ZERO) { acc, charge ->
                acc.add(charge.rentAmount).add(charge.electricityShare)
            }
            baseCharges.subtract(initialAdjustmentByTenant[tenantCharges.first().tenantId] ?: BigDecimal.ZERO).scaled()
        }
        val paidByTenant = payments.groupBy { it.tenantId }.mapValues { (_, tenantPayments) ->
            tenantPayments.fold(BigDecimal.ZERO) { acc, payment -> acc.add(payment.amountPaid) }.scaled()
        }

        val tenantIds = (billedByTenant.keys + paidByTenant.keys).toSortedSet()
        val tenantRows = tenantIds.map { tenantId ->
            val totalBilled = billedByTenant[tenantId] ?: zero()
            val totalPaid = paidByTenant[tenantId] ?: zero()
            val balance = totalBilled.subtract(totalPaid).scaled()
            val status = when {
                balance <= BigDecimal.ZERO -> PaymentStatus.Paid
                totalPaid.compareTo(BigDecimal.ZERO) == 0 -> PaymentStatus.Unpaid
                else -> PaymentStatus.Partial
            }

            OverallTenantDashboardRow(
                tenantId = tenantId,
                tenantName = tenantsById[tenantId]?.name ?: tenantId,
                totalBilled = totalBilled,
                totalPaid = totalPaid,
                balance = balance,
                status = status,
            )
        }.sortedBy { it.tenantName }

        val totalBilled = tenantRows.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.totalBilled) }.scaled()
        val totalPaid = tenantRows.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.totalPaid) }.scaled()
        val totalBalance = tenantRows.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.balance) }.scaled()
        val paidTenantCount = tenantRows.count { it.status == PaymentStatus.Paid }

        return OverallDashboardSummary(
            totalBilled = totalBilled,
            totalPaid = totalPaid,
            totalBalance = totalBalance,
            paidTenantCount = paidTenantCount,
            totalTenantCount = tenantRows.size,
            tenantRows = tenantRows,
        )
    }

    fun getTenantHistory(tenantId: String): TenantHistoryScreenData {
        val tenant = tenantRepo.getActiveTenants().firstOrNull { it.id == tenantId }
        val charges = chargeRepo.getAllCharges()
            .filter { it.tenantId == tenantId }
            .sortedByDescending { it.billingMonthId }
        val payments = paymentRepo.getAllPayments()
            .filter { it.tenantId == tenantId }
            .sortedWith(
                compareByDescending<com.testapp.renttracker.model.PaymentRecord> { it.paidOn }
                    .thenByDescending { it.id }
            )

        val paymentRows = payments.map { payment ->
            TenantPaymentHistoryRow(
                tenantId = payment.tenantId,
                billingMonthId = payment.billingMonthId,
                paidOn = payment.paidOn,
                amountPaid = payment.amountPaid.scaled(),
                component = payment.component,
                note = payment.note,
            )
        }
        val electricityCharges = charges.map { charge ->
            TenantMonthlyAmountRow(
                tenantId = charge.tenantId,
                billingMonthId = charge.billingMonthId,
                amount = charge.electricityShare.scaled(),
            )
        }
        val rentCharges = charges.map { charge ->
            TenantMonthlyAmountRow(
                tenantId = charge.tenantId,
                billingMonthId = charge.billingMonthId,
                amount = charge.rentAmount.scaled(),
            )
        }
        val adjustmentCharges = charges
            .filter { it.adjustmentAmount.compareTo(BigDecimal.ZERO) != 0 }
            .map { charge ->
                TenantMonthlyAmountRow(
                    tenantId = charge.tenantId,
                    billingMonthId = charge.billingMonthId,
                    amount = charge.adjustmentAmount.scaled(),
                )
            }

        val totalPayments = paymentRows.fold(BigDecimal.ZERO) { acc, payment -> acc.add(payment.amountPaid) }.scaled()
        val totalRent = rentCharges.fold(BigDecimal.ZERO) { acc, charge -> acc.add(charge.amount) }.scaled()
        val totalElectricity = electricityCharges.fold(BigDecimal.ZERO) { acc, charge -> acc.add(charge.amount) }.scaled()
        val initialAdjustment = charges.minByOrNull { it.billingMonthId }?.adjustmentAmount ?: BigDecimal.ZERO

        return TenantHistoryScreenData(
            tenantId = tenantId,
            tenantName = tenant?.name ?: tenantId,
            payments = paymentRows,
            electricityCharges = electricityCharges,
            rentCharges = rentCharges,
            adjustments = adjustmentCharges,
            totalPayments = totalPayments,
            totalDue = totalRent.add(totalElectricity).subtract(totalPayments).subtract(initialAdjustment).scaled(),
        )
    }

    private fun BigDecimal.scaled(): BigDecimal = setScale(2, RoundingMode.HALF_UP)

    private fun zero(): BigDecimal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
}
