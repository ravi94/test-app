package com.testapp.renttracker.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.testapp.renttracker.model.BillingMonthStatus
import java.math.BigDecimal

@Entity(tableName = "billing_months")
data class BillingMonthEntity(
    @PrimaryKey val id: String,
    // Reuse the existing column to avoid a schema migration; its value is now interpreted as the per-unit rate.
    @ColumnInfo(name = "electricity_total_amount") val electricityRatePerUnit: BigDecimal,
    @ColumnInfo(name = "status") val status: BillingMonthStatus,
)
