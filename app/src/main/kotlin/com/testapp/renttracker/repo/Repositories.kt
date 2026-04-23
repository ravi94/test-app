package com.testapp.renttracker.repo

import com.testapp.renttracker.model.*
import java.math.BigDecimal

interface FlatRepository {
    fun getActiveFlats(): List<Flat>
    fun getFlatById(flatId: String): Flat?
}

interface TenantRepository {
    fun getActiveTenants(): List<Tenant>
    fun getActiveTenantByFlat(flatId: String): Tenant?
}

interface BillingMonthRepository {
    fun getMonth(monthId: String): BillingMonth?
    fun createMonth(month: BillingMonth)
    fun updateMonth(month: BillingMonth)
}

interface FlatUsageRepository {
    fun upsertUsage(usage: FlatUsage)
    fun getUsageByMonth(monthId: String): List<FlatUsage>
}

interface TenantMonthlyChargeRepository {
    fun replaceMonthCharges(monthId: String, charges: List<TenantMonthlyCharge>)
    fun getChargesByMonth(monthId: String): List<TenantMonthlyCharge>
}

interface PaymentRecordRepository {
    fun insertPayment(payment: PaymentRecord)
    fun getPaymentsByMonth(monthId: String): List<PaymentRecord>
    fun getPaymentsByTenantAndMonth(tenantId: String, monthId: String): List<PaymentRecord>
}

interface TenantBalanceRepository {
    fun upsertBalance(balance: TenantBalance)
    fun getBalance(tenantId: String, asOfMonthId: String): TenantBalance?
    fun getAllBalancesForMonth(asOfMonthId: String): List<TenantBalance>
}

interface IdGenerator {
    fun newId(): String
}

object MathScale {
    val TWO = BigDecimal("0.01")
}
