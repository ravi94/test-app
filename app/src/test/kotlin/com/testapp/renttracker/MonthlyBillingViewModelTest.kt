package com.testapp.renttracker

import com.testapp.renttracker.model.Tenant
import com.testapp.renttracker.presentation.billing.MonthlyBillingViewModel
import com.testapp.renttracker.service.BillingMonthService
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MonthlyBillingViewModelTest {
    @Test
    fun `billing view model loads tenants and updates selected tenant`() {
        val tenantRepo = InMemoryTenantRepo(
            mutableListOf(
                Tenant("T1", "Ravi", "A-101", BigDecimal("5000.00")),
                Tenant("T2", "Aman", "A-102", BigDecimal("6000.00")),
            )
        )
        val monthRepo = InMemoryBillingMonthRepo()
        val usageRepo = InMemoryFlatUsageRepo()
        val chargeRepo = InMemoryChargeRepo()
        val balanceRepo = InMemoryBalanceRepo()
        val service = BillingMonthService(tenantRepo, monthRepo, usageRepo, chargeRepo, balanceRepo)

        val viewModel = MonthlyBillingViewModel(service, tenantRepo, monthRepo, usageRepo)

        assertEquals(listOf("T1", "T2"), viewModel.state.value.availableTenants.map { it.id })
        assertEquals("T1", viewModel.state.value.selectedTenantId)

        viewModel.setSelectedTenant("T2")

        assertEquals("T2", viewModel.state.value.selectedTenantId)
    }

    @Test
    fun `save billing entry creates month saves usage and recomputes month`() {
        val tenantRepo = InMemoryTenantRepo(
            mutableListOf(
                Tenant("T1", "Ravi", "A-101", BigDecimal("5000.00")),
                Tenant("T2", "Aman", "A-102", BigDecimal("6000.00")),
            )
        )
        val monthRepo = InMemoryBillingMonthRepo()
        val usageRepo = InMemoryFlatUsageRepo()
        val chargeRepo = InMemoryChargeRepo()
        val balanceRepo = InMemoryBalanceRepo()
        val service = BillingMonthService(tenantRepo, monthRepo, usageRepo, chargeRepo, balanceRepo)

        val viewModel = MonthlyBillingViewModel(service, tenantRepo, monthRepo, usageRepo)
        viewModel.setMonth("2026-04")
        viewModel.setSelectedTenant("T1")
        viewModel.setElectricityRateInput("8.00")
        viewModel.setSelectedTenantUnitsInput("10")
        viewModel.saveBillingEntry()

        waitFor { viewModel.state.value.message != null || viewModel.state.value.error != null }

        val createdMonth = monthRepo.getMonth("2026-04")
        assertNotNull(createdMonth)
        assertEquals(BigDecimal("8.00"), createdMonth.electricityRatePerUnit)

        val usage = usageRepo.getUsageByMonth("2026-04").associateBy { it.flatLabel }
        assertEquals(BigDecimal("10.00"), usage.getValue("A-101").unitsConsumed)

        val charges = chargeRepo.getChargesByMonth("2026-04").associateBy { it.tenantId }
        assertEquals(BigDecimal("5080.00"), charges.getValue("T1").totalDue)
        assertEquals(BigDecimal("6000.00"), charges.getValue("T2").totalDue)

        viewModel.setSelectedTenantUnitsInput("12")
        viewModel.saveBillingEntry()
        waitFor { viewModel.state.value.message == "Billing updated" }

        val updatedUsage = usageRepo.getUsageByMonth("2026-04").associateBy { it.flatLabel }
        assertEquals(BigDecimal("12.00"), updatedUsage.getValue("A-101").unitsConsumed)
    }

    private fun waitFor(condition: () -> Boolean) {
        repeat(20) {
            if (condition()) return
            Thread.sleep(20)
        }
        check(condition()) { "Condition was not met in time" }
    }
}
