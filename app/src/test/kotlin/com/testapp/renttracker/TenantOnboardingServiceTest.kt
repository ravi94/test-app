package com.testapp.renttracker

import com.testapp.renttracker.error.ErrorCodes
import com.testapp.renttracker.error.ValidationError
import com.testapp.renttracker.model.CreateTenantOnboardingInput
import com.testapp.renttracker.model.Tenant
import com.testapp.renttracker.service.BillingMonthService
import com.testapp.renttracker.service.DashboardQueryService
import com.testapp.renttracker.service.TenantOnboardingService
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TenantOnboardingServiceTest {
    @Test
    fun `creating tenant persists profile and seeds no balance when initial due is zero`() {
        val tenantRepo = InMemoryTenantRepo()
        val balanceRepo = InMemoryBalanceRepo()
        val service = TenantOnboardingService(tenantRepo, balanceRepo, FixedIdGenerator("TEN-1"))

        val tenant = service.createTenant(
            CreateTenantOnboardingInput(
                name = "Ravi Kumar",
                flatLabel = "A-101",
                monthlyRent = BigDecimal("5000.00"),
                phone = "9999999999",
                notes = "Shifting this week",
                billingStartMonth = "2026-04",
                initialDue = BigDecimal("0.00"),
            )
        )

        assertEquals("TEN-1", tenant.id)
        assertEquals("Ravi Kumar", tenant.name)
        assertEquals("A-101", tenant.flatLabel)
        assertEquals(BigDecimal("5000.00"), tenant.monthlyRent)
        assertEquals("2026-04", tenant.billingStartMonth)
        assertEquals("9999999999", tenant.phone)
        assertEquals("Shifting this week", tenant.notes)
        assertEquals(listOf("TEN-1"), tenantRepo.getActiveTenants().map { it.id })
        assertNull(balanceRepo.getBalance("TEN-1", "2026-03"))
    }

    @Test
    fun `creating tenant with due seeds negative opening balance for previous month and flows into first billing month`() {
        val tenantRepo = InMemoryTenantRepo()
        val monthRepo = InMemoryBillingMonthRepo()
        val usageRepo = InMemoryFlatUsageRepo()
        val chargeRepo = InMemoryChargeRepo()
        val balanceRepo = InMemoryBalanceRepo()
        val onboardingService = TenantOnboardingService(tenantRepo, balanceRepo, FixedIdGenerator("TEN-2"))
        val billingService = BillingMonthService(tenantRepo, monthRepo, usageRepo, chargeRepo, balanceRepo)
        val dashboardService = DashboardQueryService(chargeRepo, InMemoryPaymentRepo(), tenantRepo)

        onboardingService.createTenant(
            CreateTenantOnboardingInput(
                name = "Aman",
                flatLabel = "A-101",
                monthlyRent = BigDecimal("5000.00"),
                billingStartMonth = "2026-03",
                initialDue = BigDecimal("750.00"),
            )
        )

        val openingBalance = balanceRepo.getBalance("TEN-2", "2026-02")
        assertNotNull(openingBalance)
        assertEquals(BigDecimal("-750.00"), openingBalance.balanceAmount)

        billingService.createBillingMonth("2026-02")
        billingService.setElectricityRate("2026-02", BigDecimal("8.00"))
        billingService.upsertFlatUsage("2026-02", "A-101", BigDecimal("10"))
        billingService.computeMonthCharges("2026-02")
        assertEquals(emptyList(), chargeRepo.getChargesByMonth("2026-02"))

        billingService.createBillingMonth("2026-03")
        billingService.setElectricityRate("2026-03", BigDecimal("8.00"))
        billingService.upsertFlatUsage("2026-03", "A-101", BigDecimal("10"))
        billingService.computeMonthCharges("2026-03")

        val marchCharge = chargeRepo.getChargesByMonth("2026-03").single()
        assertEquals(BigDecimal("-750.00"), marchCharge.adjustmentAmount)
        assertEquals(BigDecimal("5830.00"), marchCharge.totalDue)

        val summary = dashboardService.getOverallSummary()
        val row = summary.tenantRows.single()
        assertEquals(BigDecimal("5830.00"), row.totalBilled)
        assertEquals(BigDecimal("5830.00"), row.balance)

        val history = dashboardService.getTenantHistory("TEN-2")
        assertEquals(BigDecimal("5830.00"), history.totalDue)
    }

    @Test
    fun `monthly rent must be positive`() {
        val service = TenantOnboardingService(InMemoryTenantRepo(), InMemoryBalanceRepo(), FixedIdGenerator("TEN-3"))

        val error = assertFailsWith<ValidationError> {
            service.createTenant(
                CreateTenantOnboardingInput(
                    name = "Ravi",
                    flatLabel = "A-101",
                    monthlyRent = BigDecimal.ZERO,
                    billingStartMonth = "2026-04",
                    initialDue = BigDecimal.ZERO,
                )
            )
        }

        assertEquals(ErrorCodes.INVALID_AMOUNT, error.code)
    }

    @Test
    fun `active flat label cannot have two active tenants`() {
        val tenantRepo = InMemoryTenantRepo(
            mutableListOf(Tenant("T1", "Existing", "A-101", BigDecimal("5000.00"), "2026-01"))
        )
        val service = TenantOnboardingService(tenantRepo, InMemoryBalanceRepo(), FixedIdGenerator("TEN-4"))

        val error = assertFailsWith<ValidationError> {
            service.createTenant(
                CreateTenantOnboardingInput(
                    name = "New Tenant",
                    flatLabel = "A-101",
                    monthlyRent = BigDecimal("5000.00"),
                    billingStartMonth = "2026-04",
                    initialDue = BigDecimal.ZERO,
                )
            )
        }

        assertEquals(ErrorCodes.FLAT_ALREADY_OCCUPIED, error.code)
    }
}

private class FixedIdGenerator(private val id: String) : com.testapp.renttracker.repo.IdGenerator {
    override fun newId(): String = id
}
