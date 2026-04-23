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

data class MonthSummary(
    val monthId: String,
    val totalExpected: BigDecimal,
    val totalPaid: BigDecimal,
    val totalPending: BigDecimal,
    val paidTenantCount: Int,
    val totalTenantCount: Int,
    val unpaidTenantIds: List<String>,
)

data class TenantPaymentState(
    val tenantId: String,
    val due: BigDecimal,
    val paid: BigDecimal,
    val pending: BigDecimal,
    val status: PaymentStatus,
)
