package com.testapp.renttracker.presentation.common

import com.testapp.renttracker.data.room.entity.TenantEntity
import com.testapp.renttracker.data.repo.RoomBillingMonthRepository
import com.testapp.renttracker.data.repo.RoomFlatUsageRepository
import com.testapp.renttracker.data.repo.RoomPaymentRecordRepository
import com.testapp.renttracker.data.repo.RoomTenantBalanceRepository
import com.testapp.renttracker.data.repo.RoomTenantMonthlyChargeRepository
import com.testapp.renttracker.data.repo.RoomTenantRepository
import com.testapp.renttracker.data.repo.UuidIdGenerator
import com.testapp.renttracker.data.room.RentTrackerDatabase
import com.testapp.renttracker.presentation.billing.MonthlyBillingViewModel
import com.testapp.renttracker.presentation.dashboard.DashboardViewModel
import com.testapp.renttracker.presentation.onboarding.TenantOnboardingViewModel
import com.testapp.renttracker.presentation.payment.PaymentViewModel
import com.testapp.renttracker.service.BillingMonthService
import com.testapp.renttracker.service.DashboardQueryService
import com.testapp.renttracker.service.PaymentService
import com.testapp.renttracker.service.TenantManagementService
import com.testapp.renttracker.service.TenantOnboardingService
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal

class AppGraph(
    private val db: RentTrackerDatabase,
) {
    private val tenantRepo = RoomTenantRepository(db.tenantDao())
    private val monthRepo = RoomBillingMonthRepository(db.billingMonthDao())
    private val usageRepo = RoomFlatUsageRepository(db.flatUsageDao())
    private val chargeRepo = RoomTenantMonthlyChargeRepository(db.tenantMonthlyChargeDao())
    private val paymentRepo = RoomPaymentRecordRepository(db.paymentRecordDao())
    private val balanceRepo = RoomTenantBalanceRepository(db.tenantBalanceDao())
    private val idGenerator = UuidIdGenerator()

    private val billingService = BillingMonthService(
        tenantRepo = tenantRepo,
        monthRepo = monthRepo,
        usageRepo = usageRepo,
        chargeRepo = chargeRepo,
        balanceRepo = balanceRepo,
    )
    private val paymentService = PaymentService(
        paymentRepo = paymentRepo,
        chargeRepo = chargeRepo,
        balanceRepo = balanceRepo,
        monthRepo = monthRepo,
        idGenerator = idGenerator,
    )
    private val dashboardQueryService = DashboardQueryService(
        chargeRepo = chargeRepo,
        paymentRepo = paymentRepo,
        tenantRepo = tenantRepo,
    )
    private val tenantOnboardingService = TenantOnboardingService(
        tenantRepo = tenantRepo,
        balanceRepo = balanceRepo,
        idGenerator = idGenerator,
    )
    private val tenantManagementService = TenantManagementService(
        tenantRepo = tenantRepo,
        chargeRepo = chargeRepo,
        paymentRepo = paymentRepo,
        balanceRepo = balanceRepo,
    )

    fun billingViewModelFactory() = SimpleViewModelFactory {
        MonthlyBillingViewModel(
            billingService = billingService,
            tenantRepo = tenantRepo,
            monthRepo = monthRepo,
            usageRepo = usageRepo,
        )
    }

    fun paymentViewModelFactory() = SimpleViewModelFactory {
        PaymentViewModel(
            paymentService = paymentService,
            tenantRepo = tenantRepo,
        )
    }

    fun dashboardViewModelFactory() = SimpleViewModelFactory {
        DashboardViewModel(
            dashboardQueryService = dashboardQueryService,
            tenantManagementService = tenantManagementService,
        )
    }

    fun tenantOnboardingViewModelFactory() = SimpleViewModelFactory {
        TenantOnboardingViewModel(
            onboardingService = tenantOnboardingService,
        )
    }

    fun seedDemoDataIfEmpty() {
        runBlocking {
            if (db.tenantDao().countAll() == 0) {
                db.tenantDao().upsert(
                    TenantEntity(
                        id = "T1",
                        name = "Tenant One",
                        flatLabel = "A-101",
                        monthlyRent = BigDecimal("5000.00"),
                        billingStartMonth = "2026-02",
                        phone = null,
                        isActive = true,
                        notes = "Demo tenant",
                    )
                )
                db.tenantDao().upsert(
                    TenantEntity(
                        id = "T2",
                        name = "Tenant Two",
                        flatLabel = "A-102",
                        monthlyRent = BigDecimal("6000.00"),
                        billingStartMonth = "2026-02",
                        phone = null,
                        isActive = true,
                        notes = "Demo tenant",
                    )
                )
            }
        }
    }
}
