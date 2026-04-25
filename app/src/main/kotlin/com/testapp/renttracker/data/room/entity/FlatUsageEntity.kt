package com.testapp.renttracker.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import java.math.BigDecimal

@Entity(
    tableName = "flat_usage",
    primaryKeys = ["flat_label", "billing_month_id"],
    indices = [Index(value = ["billing_month_id"])],
)
data class FlatUsageEntity(
    @ColumnInfo(name = "flat_label") val flatLabel: String,
    @ColumnInfo(name = "billing_month_id") val billingMonthId: String,
    @ColumnInfo(name = "units_consumed") val unitsConsumed: BigDecimal,
)
