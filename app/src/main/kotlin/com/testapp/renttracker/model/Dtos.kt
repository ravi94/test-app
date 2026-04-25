package com.testapp.renttracker.model

import java.math.BigDecimal
import java.time.LocalDate

data class RecordPaymentInput(
    val tenantId: String,
    val billingMonthId: String,
    val component: PaymentComponent,
    val amountPaid: BigDecimal,
    val paidOn: LocalDate?,
    val note: String? = null,
)

data class CreateTenantOnboardingInput(
    val name: String,
    val flatLabel: String,
    val monthlyRent: BigDecimal,
    val phone: String? = null,
    val isActive: Boolean = true,
    val notes: String? = null,
    val billingStartMonth: String,
    val initialDue: BigDecimal,
)

data class OverallDashboardSummary(
    val totalBilled: BigDecimal,
    val totalPaid: BigDecimal,
    val totalBalance: BigDecimal,
    val paidTenantCount: Int,
    val totalTenantCount: Int,
    val tenantRows: List<OverallTenantDashboardRow>,
)

data class OverallTenantDashboardRow(
    val tenantId: String,
    val tenantName: String,
    val totalBilled: BigDecimal,
    val totalPaid: BigDecimal,
    val balance: BigDecimal,
    val status: PaymentStatus,
)

data class TenantPaymentHistoryRow(
    val tenantId: String,
    val billingMonthId: String,
    val paidOn: LocalDate,
    val amountPaid: BigDecimal,
    val component: PaymentComponent,
    val note: String? = null,
)

data class TenantMonthlyAmountRow(
    val tenantId: String,
    val billingMonthId: String,
    val amount: BigDecimal,
)

data class TenantHistoryScreenData(
    val tenantId: String,
    val tenantName: String,
    val payments: List<TenantPaymentHistoryRow>,
    val electricityCharges: List<TenantMonthlyAmountRow>,
    val rentCharges: List<TenantMonthlyAmountRow>,
    val totalPayments: BigDecimal,
    val totalDue: BigDecimal,
)

data class TenantPaymentState(
    val tenantId: String,
    val due: BigDecimal,
    val paid: BigDecimal,
    val pending: BigDecimal,
    val status: PaymentStatus,
)

data class TenantListItem(
    val tenantId: String,
    val tenantName: String,
    val flatLabel: String,
    val monthlyRent: BigDecimal,
    val isActive: Boolean,
)
