package com.testapp.renttracker

import com.testapp.renttracker.error.ErrorCodes
import com.testapp.renttracker.error.ValidationError
import com.testapp.renttracker.model.Flat
import com.testapp.renttracker.model.PaymentComponent
import com.testapp.renttracker.model.RecordPaymentInput
import com.testapp.renttracker.model.Tenant
import com.testapp.renttracker.service.BillingMonthService
import com.testapp.renttracker.service.DashboardQueryService
import com.testapp.renttracker.service.PaymentService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.math.BigDecimal
import java.time.LocalDate

class ServicesFlowTest {
    @Test
    fun `record payment fails when paidOn is missing`() {
        val service = PaymentService(
            paymentRepo = InMemoryPaymentRepo(),
            chargeRepo = InMemoryChargeRepo(),
            balanceRepo = InMemoryBalanceRepo(),
            idGenerator = SequenceIdGenerator(),
        )

        val error = assertFailsWith<ValidationError> {
            service.recordPayment(
                RecordPaymentInput(
                    tenantId = "T1",
                    billingMonthId = "2026-02",
                    component = PaymentComponent.Combined,
                    amountPaid = BigDecimal("500.00"),
                    paidOn = null,
                )
            )
        }

        assertEquals(ErrorCodes.PAYMENT_DATE_REQUIRED, error.code)
    }

    @Test
    fun `full cycle computes charges and balance carry behavior`() {
        val flatRepo = InMemoryFlatRepo(
            mutableListOf(
                Flat("F1", "A-101", BigDecimal("5000.00")),
                Flat("F2", "A-102", BigDecimal("6000.00")),
            )
        )
        val tenantRepo = InMemoryTenantRepo(
            mutableListOf(
                Tenant("T1", "Ravi", "F1"),
                Tenant("T2", "Aman", "F2"),
            )
        )
        val monthRepo = InMemoryBillingMonthRepo()
        val usageRepo = InMemoryFlatUsageRepo()
        val chargeRepo = InMemoryChargeRepo()
        val paymentRepo = InMemoryPaymentRepo()
        val balanceRepo = InMemoryBalanceRepo()

        val billingService = BillingMonthService(flatRepo, tenantRepo, monthRepo, usageRepo, chargeRepo, balanceRepo)
        val paymentService = PaymentService(paymentRepo, chargeRepo, balanceRepo, SequenceIdGenerator())
        val dashboard = DashboardQueryService(chargeRepo, paymentRepo, tenantRepo)

        billingService.createBillingMonth("2026-02")
        billingService.setElectricityBill("2026-02", BigDecimal("1200.00"))
        billingService.upsertFlatUsage("2026-02", "F1", BigDecimal("40"))
        billingService.upsertFlatUsage("2026-02", "F2", BigDecimal("60"))
        billingService.computeMonthCharges("2026-02")

        paymentService.recordPayment(
            RecordPaymentInput(
                tenantId = "T1",
                billingMonthId = "2026-02",
                component = PaymentComponent.Combined,
                amountPaid = BigDecimal("6200.00"),
                paidOn = LocalDate.parse("2026-02-10"),
            )
        )
        paymentService.recordPayment(
            RecordPaymentInput(
                tenantId = "T2",
                billingMonthId = "2026-02",
                component = PaymentComponent.Combined,
                amountPaid = BigDecimal("6400.00"),
                paidOn = LocalDate.parse("2026-02-11"),
            )
        )

        val balances = paymentService.recomputeTenantBalance("2026-02").associateBy { it.tenantId }

        // T1 due = 5000 + 480 = 5480, paid 6200 => +720 credit
        assertEquals(BigDecimal("720.00"), balances.getValue("T1").balanceAmount)
        // T2 due = 6000 + 720 = 6720, paid 6400 => -320 debit
        assertEquals(BigDecimal("-320.00"), balances.getValue("T2").balanceAmount)

        val summary = dashboard.getMonthSummary("2026-02")
        assertEquals(BigDecimal("12200.00"), summary.totalExpected)
        assertEquals(BigDecimal("12600.00"), summary.totalPaid)
        assertEquals(BigDecimal("-400.00"), summary.totalPending)

        val tenantDetails = dashboard.getTenantDetails("2026-02").associateBy { it.tenantId }
        assertEquals(BigDecimal("5000.00"), tenantDetails.getValue("T1").rentAmount)
        assertEquals(BigDecimal("480.00"), tenantDetails.getValue("T1").electricityShare)
        assertEquals(BigDecimal("6200.00"), tenantDetails.getValue("T1").totalPaid)
        assertEquals(BigDecimal("-720.00"), tenantDetails.getValue("T1").dueAmount)
    }
}
