package com.testapp.renttracker.model

import java.math.BigDecimal
import java.time.LocalDate

data class Flat(
    val id: String,
    val unitLabel: String,
    val fixedMonthlyRent: BigDecimal,
    val isActive: Boolean = true,
    val notes: String? = null,
)

data class Tenant(
    val id: String,
    val name: String,
    val flatId: String,
    val phone: String? = null,
    val isActive: Boolean = true,
    val notes: String? = null,
)

data class BillingMonth(
    val id: String,
    val electricityTotalAmount: BigDecimal,
    val status: BillingMonthStatus,
)

data class FlatUsage(
    val flatId: String,
    val billingMonthId: String,
    val unitsConsumed: BigDecimal,
)

data class TenantMonthlyCharge(
    val tenantId: String,
    val billingMonthId: String,
    val rentAmount: BigDecimal,
    val electricityShare: BigDecimal,
    val adjustmentAmount: BigDecimal,
    val totalDue: BigDecimal,
)

data class TenantBalance(
    val tenantId: String,
    val asOfMonthId: String,
    val balanceAmount: BigDecimal,
)

data class PaymentRecord(
    val id: String,
    val tenantId: String,
    val billingMonthId: String,
    val component: PaymentComponent,
    val amountPaid: BigDecimal,
    val paidOn: LocalDate,
    val note: String? = null,
)
