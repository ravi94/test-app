package com.testapp.renttracker

import com.testapp.renttracker.presentation.onboarding.TenantOnboardingViewModel
import com.testapp.renttracker.service.TenantOnboardingService
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class TenantOnboardingViewModelTest {
    @Test
    fun `submit shows validation error for invalid monthly rent`() {
        val service = TenantOnboardingService(
            tenantRepo = InMemoryTenantRepo(),
            balanceRepo = InMemoryBalanceRepo(),
            idGenerator = ViewModelFixedIdGenerator("TEN-1"),
        )
        val viewModel = TenantOnboardingViewModel(service)

        viewModel.setName("Ravi")
        viewModel.setFlatLabel("A-101")
        viewModel.setMonthlyRent("abc")
        viewModel.setBillingStartMonth("2026-04")
        viewModel.setInitialDue("0.00")
        viewModel.submit()

        assertEquals("INVALID_AMOUNT: Monthly rent must be a valid number", viewModel.state.value.error)
    }

    @Test
    fun `submit resets form and publishes success message`() {
        val service = TenantOnboardingService(
            tenantRepo = InMemoryTenantRepo(),
            balanceRepo = InMemoryBalanceRepo(),
            idGenerator = ViewModelFixedIdGenerator("TEN-2"),
        )
        val viewModel = TenantOnboardingViewModel(service)

        viewModel.setName("Aman")
        viewModel.setPhone("8888888888")
        viewModel.setFlatLabel("A-102")
        viewModel.setMonthlyRent("6000.00")
        viewModel.setNotes("New joiner")
        viewModel.setBillingStartMonth("2026-05")
        viewModel.setInitialDue("450.50")
        viewModel.submit()

        waitFor { viewModel.state.value.message != null || viewModel.state.value.error != null }

        val state = viewModel.state.value
        assertEquals("Tenant created", state.message)
        assertEquals("", state.name)
        assertEquals("", state.phone)
        assertEquals("", state.flatLabel)
        assertEquals("", state.monthlyRent)
        assertEquals("", state.notes)
        assertEquals("0.00", state.initialDue)
    }

    private fun waitFor(condition: () -> Boolean) {
        repeat(20) {
            if (condition()) return
            Thread.sleep(20)
        }
        check(condition()) { "Condition was not met in time" }
    }
}

private class ViewModelFixedIdGenerator(private val id: String) : com.testapp.renttracker.repo.IdGenerator {
    override fun newId(): String = id
}
