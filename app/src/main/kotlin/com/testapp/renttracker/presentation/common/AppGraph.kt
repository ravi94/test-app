package com.testapp.renttracker.presentation.common

import com.testapp.renttracker.data.room.entity.FlatEntity
import com.testapp.renttracker.data.room.entity.TenantEntity
import com.testapp.renttracker.data.repo.RoomBillingMonthRepository
import com.testapp.renttracker.data.repo.RoomFlatRepository
import com.testapp.renttracker.data.repo.RoomFlatUsageRepository
import com.testapp.renttracker.data.repo.RoomPaymentRecordRepository
import com.testapp.renttracker.data.repo.RoomTenantBalanceRepository
import com.testapp.renttracker.data.repo.RoomTenantMonthlyChargeRepository
import com.testapp.renttracker.data.repo.RoomTenantRepository
import com.testapp.renttracker.data.repo.UuidIdGenerator
import com.testapp.renttracker.data.room.RentTrackerDatabase
import com.testapp.renttracker.presentation.billing.MonthlyBillingViewModel
import com.testapp.renttracker.presentation.dashboard.DashboardViewModel
import com.testapp.renttracker.presentation.payment.PaymentViewModel
import com.testapp.renttracker.service.BillingMonthService
import com.testapp.renttracker.service.DashboardQueryService
import com.testapp.renttracker.service.PaymentService
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal

class AppGraph(
    private val db: RentTrackerDatabase,
) {
    private val flatRepo = RoomFlatRepository(db.flatDao())
    private val tenantRepo = RoomTenantRepository(db.tenantDao())
    private val monthRepo = RoomBillingMonthRepository(db.billingMonthDao())
    private val usageRepo = RoomFlatUsageRepository(db.flatUsageDao())
    private val chargeRepo = RoomTenantMonthlyChargeRepository(db.tenantMonthlyChargeDao())
    private val paymentRepo = RoomPaymentRecordRepository(db.paymentRecordDao())
    private val balanceRepo = RoomTenantBalanceRepository(db.tenantBalanceDao())
    private val idGenerator = UuidIdGenerator()

    private val billingService = BillingMonthService(
        flatRepo = flatRepo,
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
        idGenerator = idGenerator,
    )
    private val dashboardQueryService = DashboardQueryService(
        chargeRepo = chargeRepo,
        paymentRepo = paymentRepo,
    )

    fun billingViewModelFactory() = SimpleViewModelFactory {
        MonthlyBillingViewModel(billingService)
    }

    fun paymentViewModelFactory() = SimpleViewModelFactory {
        PaymentViewModel(paymentService)
    }

    fun dashboardViewModelFactory() = SimpleViewModelFactory {
        DashboardViewModel(dashboardQueryService)
    }

    fun seedDemoDataIfEmpty() {
        runBlocking {
            if (db.flatDao().countAll() == 0) {
                db.flatDao().upsert(
                    FlatEntity(
                        id = "F1",
                        unitLabel = "A-101",
                        fixedMonthlyRent = BigDecimal("5000.00"),
                        isActive = true,
                        notes = "Demo flat",
                    )
                )
                db.flatDao().upsert(
                    FlatEntity(
                        id = "F2",
                        unitLabel = "A-102",
                        fixedMonthlyRent = BigDecimal("6000.00"),
                        isActive = true,
                        notes = "Demo flat",
                    )
                )
            }
            if (db.tenantDao().countAll() == 0) {
                db.tenantDao().upsert(
                    TenantEntity(
                        id = "T1",
                        name = "Tenant One",
                        flatId = "F1",
                        phone = null,
                        isActive = true,
                        notes = "Demo tenant",
                    )
                )
                db.tenantDao().upsert(
                    TenantEntity(
                        id = "T2",
                        name = "Tenant Two",
                        flatId = "F2",
                        phone = null,
                        isActive = true,
                        notes = "Demo tenant",
                    )
                )
            }
        }
    }
}
