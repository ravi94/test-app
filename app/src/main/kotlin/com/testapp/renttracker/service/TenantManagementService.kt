package com.testapp.renttracker.service

import com.testapp.renttracker.error.ErrorCodes
import com.testapp.renttracker.error.ValidationError
import com.testapp.renttracker.model.Tenant
import com.testapp.renttracker.repo.PaymentRecordRepository
import com.testapp.renttracker.repo.TenantBalanceRepository
import com.testapp.renttracker.repo.TenantMonthlyChargeRepository
import com.testapp.renttracker.repo.TenantRepository

class TenantManagementService(
    private val tenantRepo: TenantRepository,
    private val chargeRepo: TenantMonthlyChargeRepository,
    private val paymentRepo: PaymentRecordRepository,
    private val balanceRepo: TenantBalanceRepository,
) {
    fun getAllTenants(): List<Tenant> = tenantRepo.getAllTenants().sortedBy { it.name.lowercase() }

    fun deleteTenantAndData(tenantId: String) {
        val tenant = tenantRepo.getAllTenants().firstOrNull { it.id == tenantId }
            ?: throw ValidationError(ErrorCodes.TENANT_NOT_FOUND, "Tenant not found", "tenantId")

        chargeRepo.deleteChargesByTenant(tenant.id)
        paymentRepo.deletePaymentsByTenant(tenant.id)
        balanceRepo.deleteBalancesByTenant(tenant.id)
        tenantRepo.deleteTenantById(tenant.id)
    }
}
