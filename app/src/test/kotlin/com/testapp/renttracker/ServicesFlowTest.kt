package com.testapp.renttracker

import com.testapp.renttracker.error.ErrorCodes
import com.testapp.renttracker.error.ValidationError
import com.testapp.renttracker.model.PaymentStatus
import com.testapp.renttracker.model.PaymentComponent
import com.testapp.renttracker.model.RecordPaymentInput
import com.testapp.renttracker.model.Tenant
import com.testapp.renttracker.model.TenantMonthlyCharge
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
            monthRepo = InMemoryBillingMonthRepo(),
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
    fun `full cycle computes carry-forward and overall tenant summary`() {
        val tenantRepo = InMemoryTenantRepo(
            mutableListOf(
                Tenant("T1", "Ravi", "A-101", BigDecimal("5000.00")),
                Tenant("T2", "Aman", "A-102", BigDecimal("6000.00")),
            )
        )
        val monthRepo = InMemoryBillingMonthRepo()
        val usageRepo = InMemoryFlatUsageRepo()
        val chargeRepo = InMemoryChargeRepo()
        val paymentRepo = InMemoryPaymentRepo()
        val balanceRepo = InMemoryBalanceRepo()

        val billingService = BillingMonthService(tenantRepo, monthRepo, usageRepo, chargeRepo, balanceRepo)
        val paymentService = PaymentService(paymentRepo, chargeRepo, balanceRepo, monthRepo, SequenceIdGenerator())
        val dashboard = DashboardQueryService(chargeRepo, paymentRepo, tenantRepo)

        billingService.createBillingMonth("2026-02")
        billingService.setElectricityRate("2026-02", BigDecimal("8.00"))
        billingService.upsertFlatUsage("2026-02", "A-101", BigDecimal("10"))
        billingService.upsertFlatUsage("2026-02", "A-102", BigDecimal("20"))
        billingService.computeMonthCharges("2026-02")

        val febCharges = chargeRepo.getChargesByMonth("2026-02").associateBy { it.tenantId }
        assertEquals(BigDecimal("5080.00"), febCharges.getValue("T1").totalDue)
        assertEquals(BigDecimal("6160.00"), febCharges.getValue("T2").totalDue)

        paymentService.recordPayment(
            RecordPaymentInput(
                tenantId = "T1",
                billingMonthId = "2026-02",
                component = PaymentComponent.Combined,
                amountPaid = BigDecimal("4000.00"),
                paidOn = LocalDate.parse("2026-02-10"),
            )
        )

        val febBalances = balanceRepo.getAllBalancesForMonth("2026-02").associateBy { it.tenantId }
        assertEquals(BigDecimal("-1080.00"), febBalances.getValue("T1").balanceAmount)
        assertEquals(BigDecimal("-6160.00"), febBalances.getValue("T2").balanceAmount)

        billingService.createBillingMonth("2026-03")
        billingService.setElectricityRate("2026-03", BigDecimal("8.00"))
        billingService.upsertFlatUsage("2026-03", "A-101", BigDecimal("20"))
        billingService.upsertFlatUsage("2026-03", "A-102", BigDecimal("40"))
        billingService.computeMonthCharges("2026-03")

        val marCharges = chargeRepo.getChargesByMonth("2026-03").associateBy { it.tenantId }
        assertMonthCharge(
            marCharges.getValue("T1"),
            rent = "5000.00",
            electricity = "160.00",
            adjustment = "-1080.00",
            totalDue = "6240.00",
        )
        assertMonthCharge(
            marCharges.getValue("T2"),
            rent = "6000.00",
            electricity = "320.00",
            adjustment = "-6160.00",
            totalDue = "12480.00",
        )

        val summary = dashboard.getOverallSummary()
        assertEquals(BigDecimal("22720.00"), summary.totalBilled)
        assertEquals(BigDecimal("4000.00"), summary.totalPaid)
        assertEquals(BigDecimal("18720.00"), summary.totalBalance)

        val rows = summary.tenantRows.associateBy { it.tenantId }
        assertEquals(BigDecimal("10240.00"), rows.getValue("T1").totalBilled)
        assertEquals(BigDecimal("4000.00"), rows.getValue("T1").totalPaid)
        assertEquals(BigDecimal("6240.00"), rows.getValue("T1").balance)
        assertEquals(PaymentStatus.Partial, rows.getValue("T1").status)
        assertEquals(BigDecimal("12480.00"), rows.getValue("T2").totalBilled)
        assertEquals(BigDecimal("0.00"), rows.getValue("T2").totalPaid)
        assertEquals(BigDecimal("12480.00"), rows.getValue("T2").balance)
        assertEquals(PaymentStatus.Unpaid, rows.getValue("T2").status)

        val tenantHistory = dashboard.getTenantHistory("T1")
        assertEquals("Ravi", tenantHistory.tenantName)
        assertEquals(BigDecimal("4000.00"), tenantHistory.totalPayments)
        assertEquals(BigDecimal("6240.00"), tenantHistory.totalDue)
        assertEquals(listOf("2026-03", "2026-02"), tenantHistory.rentCharges.map { it.billingMonthId })
        assertEquals(listOf("2026-03", "2026-02"), tenantHistory.electricityCharges.map { it.billingMonthId })
        assertEquals(listOf(LocalDate.parse("2026-02-10")), tenantHistory.payments.map { it.paidOn })
    }

    @Test
    fun `overall summary marks overpaid tenant as paid`() {
        val chargeRepo = InMemoryChargeRepo()
        val paymentRepo = InMemoryPaymentRepo()
        val tenantRepo = InMemoryTenantRepo(mutableListOf(Tenant("T1", "Ravi", "A-101", BigDecimal("5000.00"))))
        val dashboard = DashboardQueryService(chargeRepo, paymentRepo, tenantRepo)

        chargeRepo.replaceMonthCharges(
            "2026-02",
            listOf(
                TenantMonthlyCharge(
                    tenantId = "T1",
                    billingMonthId = "2026-02",
                    rentAmount = BigDecimal("5000.00"),
                    electricityShare = BigDecimal("80.00"),
                    adjustmentAmount = BigDecimal("0.00"),
                    totalDue = BigDecimal("5080.00"),
                )
            )
        )
        paymentRepo.insertPayment(
            com.testapp.renttracker.model.PaymentRecord(
                id = "pay-1",
                tenantId = "T1",
                billingMonthId = "2026-02",
                component = PaymentComponent.Combined,
                amountPaid = BigDecimal("6000.00"),
                paidOn = LocalDate.parse("2026-02-12"),
            )
        )

        val row = dashboard.getOverallSummary().tenantRows.single()
        assertEquals(BigDecimal("-920.00"), row.balance)
        assertEquals(PaymentStatus.Paid, row.status)
    }

    @Test
    fun `record payment fails when billing month does not exist`() {
        val service = PaymentService(
            paymentRepo = InMemoryPaymentRepo(),
            chargeRepo = InMemoryChargeRepo(),
            balanceRepo = InMemoryBalanceRepo(),
            monthRepo = InMemoryBillingMonthRepo(),
            idGenerator = SequenceIdGenerator(),
        )

        val error = assertFailsWith<ValidationError> {
            service.recordPayment(
                RecordPaymentInput(
                    tenantId = "T1",
                    billingMonthId = "2026-04",
                    component = PaymentComponent.Combined,
                    amountPaid = BigDecimal("500.00"),
                    paidOn = LocalDate.parse("2026-04-01"),
                )
            )
        }

        assertEquals(ErrorCodes.MONTH_NOT_FOUND, error.code)
    }

    @Test
    fun `tenant history filters records and sorts newest first`() {
        val chargeRepo = InMemoryChargeRepo()
        val paymentRepo = InMemoryPaymentRepo()
        val tenantRepo = InMemoryTenantRepo(
            mutableListOf(
                Tenant("T1", "Ravi", "A-101", BigDecimal("5000.00")),
                Tenant("T2", "Aman", "A-102", BigDecimal("6000.00")),
            )
        )
        val dashboard = DashboardQueryService(chargeRepo, paymentRepo, tenantRepo)

        chargeRepo.replaceMonthCharges(
            "2026-01",
            listOf(
                TenantMonthlyCharge("T1", "2026-01", BigDecimal("5000.00"), BigDecimal("50.00"), BigDecimal("0.00"), BigDecimal("5050.00")),
                TenantMonthlyCharge("T2", "2026-01", BigDecimal("6000.00"), BigDecimal("60.00"), BigDecimal("0.00"), BigDecimal("6060.00")),
            )
        )
        chargeRepo.replaceMonthCharges(
            "2026-03",
            listOf(
                TenantMonthlyCharge("T1", "2026-03", BigDecimal("5000.00"), BigDecimal("150.00"), BigDecimal("0.00"), BigDecimal("5150.00")),
            )
        )
        paymentRepo.insertPayment(
            com.testapp.renttracker.model.PaymentRecord(
                id = "pay-1",
                tenantId = "T1",
                billingMonthId = "2026-01",
                component = PaymentComponent.Combined,
                amountPaid = BigDecimal("1000.00"),
                paidOn = LocalDate.parse("2026-01-09"),
            )
        )
        paymentRepo.insertPayment(
            com.testapp.renttracker.model.PaymentRecord(
                id = "pay-2",
                tenantId = "T1",
                billingMonthId = "2026-03",
                component = PaymentComponent.Rent,
                amountPaid = BigDecimal("2000.00"),
                paidOn = LocalDate.parse("2026-03-09"),
            )
        )
        paymentRepo.insertPayment(
            com.testapp.renttracker.model.PaymentRecord(
                id = "pay-3",
                tenantId = "T2",
                billingMonthId = "2026-03",
                component = PaymentComponent.Combined,
                amountPaid = BigDecimal("1500.00"),
                paidOn = LocalDate.parse("2026-03-10"),
            )
        )

        val history = dashboard.getTenantHistory("T1")
        assertEquals(listOf(LocalDate.parse("2026-03-09"), LocalDate.parse("2026-01-09")), history.payments.map { it.paidOn })
        assertEquals(listOf("2026-03", "2026-01"), history.rentCharges.map { it.billingMonthId })
        assertEquals(listOf("2026-03", "2026-01"), history.electricityCharges.map { it.billingMonthId })
        assertEquals(BigDecimal("3000.00"), history.totalPayments)
        assertEquals(BigDecimal("7200.00"), history.totalDue)
        assertEquals(2, history.payments.size)
    }

    private fun assertMonthCharge(
        charge: TenantMonthlyCharge,
        rent: String,
        electricity: String,
        adjustment: String,
        totalDue: String,
    ) {
        assertEquals(BigDecimal(rent), charge.rentAmount)
        assertEquals(BigDecimal(electricity), charge.electricityShare)
        assertEquals(BigDecimal(adjustment), charge.adjustmentAmount)
        assertEquals(BigDecimal(totalDue), charge.totalDue)
    }
}
