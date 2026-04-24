package com.testapp.renttracker

import com.testapp.renttracker.model.Flat
import com.testapp.renttracker.model.PaymentComponent
import com.testapp.renttracker.model.RecordPaymentInput
import com.testapp.renttracker.model.Tenant
import com.testapp.renttracker.presentation.dashboard.DashboardViewModel
import com.testapp.renttracker.service.BillingMonthService
import com.testapp.renttracker.service.DashboardQueryService
import com.testapp.renttracker.service.PaymentService
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val mainDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `open tenant history loads detail and close returns to summary`() {
        val flatRepo = InMemoryFlatRepo(mutableListOf(Flat("F1", "A-101", BigDecimal("5000.00"))))
        val tenantRepo = InMemoryTenantRepo(mutableListOf(Tenant("T1", "Ravi", "F1")))
        val monthRepo = InMemoryBillingMonthRepo()
        val usageRepo = InMemoryFlatUsageRepo()
        val chargeRepo = InMemoryChargeRepo()
        val paymentRepo = InMemoryPaymentRepo()
        val balanceRepo = InMemoryBalanceRepo()

        val billingService = BillingMonthService(flatRepo, tenantRepo, monthRepo, usageRepo, chargeRepo, balanceRepo)
        val paymentService = PaymentService(paymentRepo, chargeRepo, balanceRepo, SequenceIdGenerator())
        val dashboardService = DashboardQueryService(chargeRepo, paymentRepo, tenantRepo)
        val viewModel = DashboardViewModel(dashboardService)

        billingService.createBillingMonth("2026-02")
        billingService.setElectricityRate("2026-02", BigDecimal("8.00"))
        billingService.upsertFlatUsage("2026-02", "F1", BigDecimal("10"))
        billingService.computeMonthCharges("2026-02")
        paymentService.recordPayment(
            RecordPaymentInput(
                tenantId = "T1",
                billingMonthId = "2026-02",
                component = PaymentComponent.Combined,
                amountPaid = BigDecimal("3000.00"),
                paidOn = LocalDate.parse("2026-02-10"),
            )
        )

        viewModel.refresh()
        advanceUntilIdle()
        waitFor { viewModel.state.value.summary != null }

        assertEquals(1, viewModel.state.value.summary?.tenantRows?.size)
        assertNull(viewModel.state.value.selectedTenantHistory)

        viewModel.openTenantHistory("T1")
        advanceUntilIdle()
        waitFor { viewModel.state.value.selectedTenantHistory != null }

        val history = viewModel.state.value.selectedTenantHistory
        assertNotNull(history)
        assertEquals("Ravi", history.tenantName)
        assertEquals(BigDecimal("3000.00"), history.totalPayments)

        viewModel.closeTenantHistory()

        assertNull(viewModel.state.value.selectedTenantHistory)
        assertEquals(1, viewModel.state.value.summary?.tenantRows?.size)
    }

    private fun advanceUntilIdle() {
        mainDispatcher.scheduler.advanceUntilIdle()
    }

    private fun waitFor(condition: () -> Boolean) {
        repeat(20) {
            if (condition()) return
            Thread.sleep(20)
        }
        check(condition()) { "Condition was not met in time" }
    }
}
