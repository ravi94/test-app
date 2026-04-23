package com.testapp.renttracker.service

import com.testapp.renttracker.error.ErrorCodes
import com.testapp.renttracker.error.ValidationError
import com.testapp.renttracker.model.*
import com.testapp.renttracker.repo.IdGenerator
import com.testapp.renttracker.repo.PaymentRecordRepository
import com.testapp.renttracker.repo.TenantBalanceRepository
import com.testapp.renttracker.repo.TenantMonthlyChargeRepository
import java.math.BigDecimal
import java.math.RoundingMode

class PaymentService(
    private val paymentRepo: PaymentRecordRepository,
    private val chargeRepo: TenantMonthlyChargeRepository,
    private val balanceRepo: TenantBalanceRepository,
    private val idGenerator: IdGenerator,
) {
    fun recordPayment(input: RecordPaymentInput): PaymentRecord {
        if (input.paidOn == null) {
            throw ValidationError(
                code = ErrorCodes.PAYMENT_DATE_REQUIRED,
                message = "Payment date is required",
                field = "paidOn",
            )
        }
        if (input.amountPaid <= BigDecimal.ZERO) {
            throw ValidationError(
                code = ErrorCodes.INVALID_AMOUNT,
                message = "Paid amount must be greater than zero",
                field = "amountPaid",
            )
        }

        val payment = PaymentRecord(
            id = idGenerator.newId(),
            tenantId = input.tenantId,
            billingMonthId = input.billingMonthId,
            component = input.component,
            amountPaid = input.amountPaid.setScale(2, RoundingMode.HALF_UP),
            paidOn = input.paidOn,
            note = input.note,
        )
        paymentRepo.insertPayment(payment)
        return payment
    }

    fun recomputeTenantBalance(monthId: String): List<TenantBalance> {
        val charges = chargeRepo.getChargesByMonth(monthId)
        if (charges.isEmpty()) {
            throw ValidationError(ErrorCodes.MISSING_MONTH_CHARGES, "No monthly charges found for month")
        }

        return charges.map { charge ->
            val totalPaid = paymentRepo
                .getPaymentsByTenantAndMonth(charge.tenantId, monthId)
                .fold(BigDecimal.ZERO) { acc, payment -> acc.add(payment.amountPaid) }
                .setScale(2, RoundingMode.HALF_UP)

            val balanceAmount = totalPaid.subtract(charge.totalDue).setScale(2, RoundingMode.HALF_UP)
            TenantBalance(
                tenantId = charge.tenantId,
                asOfMonthId = monthId,
                balanceAmount = balanceAmount,
            ).also(balanceRepo::upsertBalance)
        }
    }

    fun paymentStateForMonth(monthId: String): List<TenantPaymentState> {
        val charges = chargeRepo.getChargesByMonth(monthId)
        return charges.map { charge ->
            val paid = paymentRepo
                .getPaymentsByTenantAndMonth(charge.tenantId, monthId)
                .fold(BigDecimal.ZERO) { acc, p -> acc.add(p.amountPaid) }
                .setScale(2, RoundingMode.HALF_UP)
            val pending = charge.totalDue.subtract(paid).setScale(2, RoundingMode.HALF_UP)
            val status = when {
                pending <= BigDecimal.ZERO -> PaymentStatus.Paid
                paid == BigDecimal.ZERO -> PaymentStatus.Unpaid
                else -> PaymentStatus.Partial
            }
            TenantPaymentState(charge.tenantId, charge.totalDue, paid, pending, status)
        }
    }
}
