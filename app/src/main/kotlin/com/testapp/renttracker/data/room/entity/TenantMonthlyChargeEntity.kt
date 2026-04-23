package com.testapp.renttracker.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.math.BigDecimal

@Entity(
    tableName = "tenant_monthly_charges",
    primaryKeys = ["tenant_id", "billing_month_id"],
    foreignKeys = [
        ForeignKey(
            entity = TenantEntity::class,
            parentColumns = ["id"],
            childColumns = ["tenant_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = BillingMonthEntity::class,
            parentColumns = ["id"],
            childColumns = ["billing_month_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["billing_month_id"], name = "idx_charges_month")],
)
data class TenantMonthlyChargeEntity(
    @ColumnInfo(name = "tenant_id") val tenantId: String,
    @ColumnInfo(name = "billing_month_id") val billingMonthId: String,
    @ColumnInfo(name = "rent_amount") val rentAmount: BigDecimal,
    @ColumnInfo(name = "electricity_share") val electricityShare: BigDecimal,
    @ColumnInfo(name = "adjustment_amount") val adjustmentAmount: BigDecimal,
    @ColumnInfo(name = "total_due") val totalDue: BigDecimal,
)
