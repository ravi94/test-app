package com.testapp.renttracker.data.repo

import com.testapp.renttracker.data.room.entity.BillingMonthEntity
import com.testapp.renttracker.data.room.entity.FlatEntity
import com.testapp.renttracker.data.room.entity.FlatUsageEntity
import com.testapp.renttracker.data.room.entity.PaymentRecordEntity
import com.testapp.renttracker.data.room.entity.TenantBalanceEntity
import com.testapp.renttracker.data.room.entity.TenantEntity
import com.testapp.renttracker.data.room.entity.TenantMonthlyChargeEntity
import com.testapp.renttracker.model.BillingMonth
import com.testapp.renttracker.model.Flat
import com.testapp.renttracker.model.FlatUsage
import com.testapp.renttracker.model.PaymentRecord
import com.testapp.renttracker.model.Tenant
import com.testapp.renttracker.model.TenantBalance
import com.testapp.renttracker.model.TenantMonthlyCharge

internal fun FlatEntity.toDomain(): Flat =
    Flat(id = id, unitLabel = unitLabel, fixedMonthlyRent = fixedMonthlyRent, isActive = isActive, notes = notes)

internal fun Flat.toEntity(): FlatEntity =
    FlatEntity(id = id, unitLabel = unitLabel, fixedMonthlyRent = fixedMonthlyRent, isActive = isActive, notes = notes)

internal fun TenantEntity.toDomain(): Tenant =
    Tenant(id = id, name = name, flatId = flatId, phone = phone, isActive = isActive, notes = notes)

internal fun Tenant.toEntity(): TenantEntity =
    TenantEntity(id = id, name = name, flatId = flatId, phone = phone, isActive = isActive, notes = notes)

internal fun BillingMonthEntity.toDomain(): BillingMonth =
    BillingMonth(id = id, electricityRatePerUnit = electricityRatePerUnit, status = status)

internal fun BillingMonth.toEntity(): BillingMonthEntity =
    BillingMonthEntity(id = id, electricityRatePerUnit = electricityRatePerUnit, status = status)

internal fun FlatUsageEntity.toDomain(): FlatUsage =
    FlatUsage(flatId = flatId, billingMonthId = billingMonthId, unitsConsumed = unitsConsumed)

internal fun FlatUsage.toEntity(): FlatUsageEntity =
    FlatUsageEntity(flatId = flatId, billingMonthId = billingMonthId, unitsConsumed = unitsConsumed)

internal fun TenantMonthlyChargeEntity.toDomain(): TenantMonthlyCharge =
    TenantMonthlyCharge(
        tenantId = tenantId,
        billingMonthId = billingMonthId,
        rentAmount = rentAmount,
        electricityShare = electricityShare,
        adjustmentAmount = adjustmentAmount,
        totalDue = totalDue,
    )

internal fun TenantMonthlyCharge.toEntity(): TenantMonthlyChargeEntity =
    TenantMonthlyChargeEntity(
        tenantId = tenantId,
        billingMonthId = billingMonthId,
        rentAmount = rentAmount,
        electricityShare = electricityShare,
        adjustmentAmount = adjustmentAmount,
        totalDue = totalDue,
    )

internal fun PaymentRecordEntity.toDomain(): PaymentRecord =
    PaymentRecord(
        id = id,
        tenantId = tenantId,
        billingMonthId = billingMonthId,
        component = component,
        amountPaid = amountPaid,
        paidOn = paidOn,
        note = note,
    )

internal fun PaymentRecord.toEntity(): PaymentRecordEntity =
    PaymentRecordEntity(
        id = id,
        tenantId = tenantId,
        billingMonthId = billingMonthId,
        component = component,
        amountPaid = amountPaid,
        paidOn = paidOn,
        note = note,
    )

internal fun TenantBalanceEntity.toDomain(): TenantBalance =
    TenantBalance(tenantId = tenantId, asOfMonthId = asOfMonthId, balanceAmount = balanceAmount)

internal fun TenantBalance.toEntity(): TenantBalanceEntity =
    TenantBalanceEntity(tenantId = tenantId, asOfMonthId = asOfMonthId, balanceAmount = balanceAmount)
