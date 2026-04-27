package com.testapp.renttracker.repo

import com.testapp.renttracker.model.BillingMonth
import com.testapp.renttracker.model.FlatUsage
import com.testapp.renttracker.model.PaymentRecord
import com.testapp.renttracker.model.Tenant
import com.testapp.renttracker.model.TenantBalance
import com.testapp.renttracker.model.TenantMonthlyCharge
import java.math.BigDecimal

interface TenantRepository {
    fun getActiveTenants(): List<Tenant>
    fun getAllTenants(): List<Tenant>
    fun getActiveTenantByFlatLabel(flatLabel: String): Tenant?
    fun upsertTenant(tenant: Tenant)
    fun deleteTenantById(tenantId: String)
}

interface BillingMonthRepository {
    fun getMonth(monthId: String): BillingMonth?
    fun createMonth(month: BillingMonth)
    fun updateMonth(month: BillingMonth)
}

interface FlatUsageRepository {
    fun upsertUsage(usage: FlatUsage)
    fun getUsageByMonth(monthId: String): List<FlatUsage>
    fun getUsage(flatLabel: String, monthId: String): FlatUsage?
}

interface TenantMonthlyChargeRepository {
    fun replaceMonthCharges(monthId: String, charges: List<TenantMonthlyCharge>)
    fun getChargesByMonth(monthId: String): List<TenantMonthlyCharge>
    fun getAllCharges(): List<TenantMonthlyCharge>
    fun deleteChargesByTenant(tenantId: String)
}

interface PaymentRecordRepository {
    fun insertPayment(payment: PaymentRecord)
    fun getPaymentsByMonth(monthId: String): List<PaymentRecord>
    fun getPaymentsByTenantAndMonth(tenantId: String, monthId: String): List<PaymentRecord>
    fun getAllPayments(): List<PaymentRecord>
    fun deletePaymentsByTenant(tenantId: String)
}

interface TenantBalanceRepository {
    fun upsertBalance(balance: TenantBalance)
    fun getBalance(tenantId: String, asOfMonthId: String): TenantBalance?
    fun getAllBalancesForMonth(asOfMonthId: String): List<TenantBalance>
    fun deleteBalancesByTenant(tenantId: String)
}

interface IdGenerator {
    fun newId(): String
}

object MathScale {
    val TWO = BigDecimal("0.01")
}
