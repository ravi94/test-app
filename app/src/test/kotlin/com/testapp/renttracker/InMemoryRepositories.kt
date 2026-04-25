package com.testapp.renttracker

import com.testapp.renttracker.model.*
import com.testapp.renttracker.repo.*

class InMemoryTenantRepo(private val tenants: MutableList<Tenant> = mutableListOf()) : TenantRepository {
    override fun getActiveTenants(): List<Tenant> = tenants.filter { it.isActive }
    override fun getAllTenants(): List<Tenant> = tenants.toList()
    override fun getActiveTenantByFlatLabel(flatLabel: String): Tenant? =
        tenants.firstOrNull { it.flatLabel == flatLabel && it.isActive }
    override fun upsertTenant(tenant: Tenant) {
        val index = tenants.indexOfFirst { it.id == tenant.id }
        if (index >= 0) {
            tenants[index] = tenant
        } else {
            tenants += tenant
        }
    }
    override fun deleteTenantById(tenantId: String) {
        tenants.removeAll { it.id == tenantId }
    }
}

class InMemoryBillingMonthRepo : BillingMonthRepository {
    private val months = linkedMapOf<String, BillingMonth>()
    override fun getMonth(monthId: String): BillingMonth? = months[monthId]
    override fun createMonth(month: BillingMonth) {
        months[month.id] = month
    }

    override fun updateMonth(month: BillingMonth) {
        months[month.id] = month
    }
}

class InMemoryFlatUsageRepo : FlatUsageRepository {
    private val map = linkedMapOf<Pair<String, String>, FlatUsage>()

    override fun upsertUsage(usage: FlatUsage) {
        map[usage.flatLabel to usage.billingMonthId] = usage
    }

    override fun getUsageByMonth(monthId: String): List<FlatUsage> = map.values.filter { it.billingMonthId == monthId }
}

class InMemoryChargeRepo : TenantMonthlyChargeRepository {
    private val byMonth = linkedMapOf<String, List<TenantMonthlyCharge>>()

    override fun replaceMonthCharges(monthId: String, charges: List<TenantMonthlyCharge>) {
        byMonth[monthId] = charges
    }

    override fun getChargesByMonth(monthId: String): List<TenantMonthlyCharge> = byMonth[monthId].orEmpty()

    override fun getAllCharges(): List<TenantMonthlyCharge> = byMonth.values.flatten()

    override fun deleteChargesByTenant(tenantId: String) {
        byMonth.replaceAll { _, charges -> charges.filterNot { it.tenantId == tenantId } }
    }
}

class InMemoryPaymentRepo : PaymentRecordRepository {
    private val payments = mutableListOf<PaymentRecord>()

    override fun insertPayment(payment: PaymentRecord) {
        payments += payment
    }

    override fun getPaymentsByMonth(monthId: String): List<PaymentRecord> = payments.filter { it.billingMonthId == monthId }

    override fun getPaymentsByTenantAndMonth(tenantId: String, monthId: String): List<PaymentRecord> {
        return payments.filter { it.tenantId == tenantId && it.billingMonthId == monthId }
    }

    override fun getAllPayments(): List<PaymentRecord> = payments.toList()

    override fun deletePaymentsByTenant(tenantId: String) {
        payments.removeAll { it.tenantId == tenantId }
    }
}

class InMemoryBalanceRepo : TenantBalanceRepository {
    private val balances = linkedMapOf<Pair<String, String>, TenantBalance>()

    override fun upsertBalance(balance: TenantBalance) {
        balances[balance.tenantId to balance.asOfMonthId] = balance
    }

    override fun getBalance(tenantId: String, asOfMonthId: String): TenantBalance? = balances[tenantId to asOfMonthId]

    override fun getAllBalancesForMonth(asOfMonthId: String): List<TenantBalance> {
        return balances.values.filter { it.asOfMonthId == asOfMonthId }
    }

    override fun deleteBalancesByTenant(tenantId: String) {
        balances.keys.removeAll { (existingTenantId, _) -> existingTenantId == tenantId }
    }
}

class SequenceIdGenerator : IdGenerator {
    private var counter = 1
    override fun newId(): String = "pay-${counter++}"
}
