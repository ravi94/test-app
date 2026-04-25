package com.testapp.renttracker

import com.testapp.renttracker.model.PaymentComponent
import com.testapp.renttracker.model.PaymentRecord
import com.testapp.renttracker.model.Tenant
import com.testapp.renttracker.model.TenantBalance
import com.testapp.renttracker.model.TenantMonthlyCharge
import com.testapp.renttracker.service.TenantManagementService
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TenantManagementServiceTest {
    @Test
    fun `delete tenant removes tenant and complete related data`() {
        val tenantRepo = InMemoryTenantRepo(
            mutableListOf(
                Tenant("T1", "Ravi", "A-101", BigDecimal("5000.00")),
                Tenant("T2", "Aman", "A-102", BigDecimal("6000.00")),
            )
        )
        val chargeRepo = InMemoryChargeRepo()
        val paymentRepo = InMemoryPaymentRepo()
        val balanceRepo = InMemoryBalanceRepo()
        val service = TenantManagementService(tenantRepo, chargeRepo, paymentRepo, balanceRepo)

        chargeRepo.replaceMonthCharges(
            "2026-04",
            listOf(
                TenantMonthlyCharge("T1", "2026-04", BigDecimal("5000.00"), BigDecimal("80.00"), BigDecimal("0.00"), BigDecimal("5080.00")),
                TenantMonthlyCharge("T2", "2026-04", BigDecimal("6000.00"), BigDecimal("90.00"), BigDecimal("0.00"), BigDecimal("6090.00")),
            )
        )
        paymentRepo.insertPayment(
            PaymentRecord(
                id = "pay-1",
                tenantId = "T1",
                billingMonthId = "2026-04",
                component = PaymentComponent.Combined,
                amountPaid = BigDecimal("2000.00"),
                paidOn = LocalDate.parse("2026-04-10"),
            )
        )
        balanceRepo.upsertBalance(TenantBalance("T1", "2026-04", BigDecimal("-3080.00")))

        service.deleteTenantAndData("T1")

        assertEquals(listOf("T2"), tenantRepo.getAllTenants().map { it.id })
        assertTrue(chargeRepo.getAllCharges().none { it.tenantId == "T1" })
        assertTrue(paymentRepo.getAllPayments().none { it.tenantId == "T1" })
        assertEquals(null, balanceRepo.getBalance("T1", "2026-04"))
    }
}
