package com.testapp.renttracker.data.repo

import com.testapp.renttracker.data.room.dao.BillingMonthDao
import com.testapp.renttracker.data.room.dao.FlatDao
import com.testapp.renttracker.data.room.dao.FlatUsageDao
import com.testapp.renttracker.data.room.dao.PaymentRecordDao
import com.testapp.renttracker.data.room.dao.TenantBalanceDao
import com.testapp.renttracker.data.room.dao.TenantDao
import com.testapp.renttracker.data.room.dao.TenantMonthlyChargeDao
import com.testapp.renttracker.model.BillingMonth
import com.testapp.renttracker.model.Flat
import com.testapp.renttracker.model.FlatUsage
import com.testapp.renttracker.model.PaymentRecord
import com.testapp.renttracker.model.Tenant
import com.testapp.renttracker.model.TenantBalance
import com.testapp.renttracker.model.TenantMonthlyCharge
import com.testapp.renttracker.repo.BillingMonthRepository
import com.testapp.renttracker.repo.FlatRepository
import com.testapp.renttracker.repo.FlatUsageRepository
import com.testapp.renttracker.repo.IdGenerator
import com.testapp.renttracker.repo.PaymentRecordRepository
import com.testapp.renttracker.repo.TenantBalanceRepository
import com.testapp.renttracker.repo.TenantMonthlyChargeRepository
import com.testapp.renttracker.repo.TenantRepository
import kotlinx.coroutines.runBlocking
import java.util.UUID

class RoomFlatRepository(private val dao: FlatDao) : FlatRepository {
    override fun getActiveFlats(): List<Flat> = runBlocking { dao.getActiveFlats().map { it.toDomain() } }

    override fun getFlatById(flatId: String): Flat? = runBlocking { dao.getFlatById(flatId)?.toDomain() }
}

class RoomTenantRepository(private val dao: TenantDao) : TenantRepository {
    override fun getActiveTenants(): List<Tenant> = runBlocking { dao.getActiveTenants().map { it.toDomain() } }

    override fun getActiveTenantByFlat(flatId: String): Tenant? = runBlocking {
        dao.getActiveTenantByFlat(flatId)?.toDomain()
    }
}

class RoomBillingMonthRepository(private val dao: BillingMonthDao) : BillingMonthRepository {
    override fun getMonth(monthId: String): BillingMonth? = runBlocking { dao.getById(monthId)?.toDomain() }

    override fun createMonth(month: BillingMonth) = runBlocking { dao.insert(month.toEntity()) }

    override fun updateMonth(month: BillingMonth) = runBlocking { dao.update(month.toEntity()) }
}

class RoomFlatUsageRepository(private val dao: FlatUsageDao) : FlatUsageRepository {
    override fun upsertUsage(usage: FlatUsage) = runBlocking { dao.upsert(usage.toEntity()) }

    override fun getUsageByMonth(monthId: String): List<FlatUsage> = runBlocking {
        dao.getByMonth(monthId).map { it.toDomain() }
    }
}

class RoomTenantMonthlyChargeRepository(private val dao: TenantMonthlyChargeDao) : TenantMonthlyChargeRepository {
    override fun replaceMonthCharges(monthId: String, charges: List<TenantMonthlyCharge>) = runBlocking {
        dao.replaceByMonth(monthId, charges.map { it.toEntity() })
    }

    override fun getChargesByMonth(monthId: String): List<TenantMonthlyCharge> = runBlocking {
        dao.getByMonth(monthId).map { it.toDomain() }
    }
}

class RoomPaymentRecordRepository(private val dao: PaymentRecordDao) : PaymentRecordRepository {
    override fun insertPayment(payment: PaymentRecord) = runBlocking { dao.insert(payment.toEntity()) }

    override fun getPaymentsByMonth(monthId: String): List<PaymentRecord> = runBlocking {
        dao.getByMonth(monthId).map { it.toDomain() }
    }

    override fun getPaymentsByTenantAndMonth(tenantId: String, monthId: String): List<PaymentRecord> = runBlocking {
        dao.getByTenantAndMonth(tenantId, monthId).map { it.toDomain() }
    }
}

class RoomTenantBalanceRepository(private val dao: TenantBalanceDao) : TenantBalanceRepository {
    override fun upsertBalance(balance: TenantBalance) = runBlocking { dao.upsert(balance.toEntity()) }

    override fun getBalance(tenantId: String, asOfMonthId: String): TenantBalance? = runBlocking {
        dao.getBalance(tenantId, asOfMonthId)?.toDomain()
    }

    override fun getAllBalancesForMonth(asOfMonthId: String): List<TenantBalance> = runBlocking {
        dao.getAllForMonth(asOfMonthId).map { it.toDomain() }
    }
}

class UuidIdGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
