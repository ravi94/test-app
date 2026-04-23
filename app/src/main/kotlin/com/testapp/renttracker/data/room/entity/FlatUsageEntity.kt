package com.testapp.renttracker.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.math.BigDecimal

@Entity(
    tableName = "flat_usage",
    primaryKeys = ["flat_id", "billing_month_id"],
    foreignKeys = [
        ForeignKey(
            entity = FlatEntity::class,
            parentColumns = ["id"],
            childColumns = ["flat_id"],
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
    indices = [Index(value = ["billing_month_id"])],
)
data class FlatUsageEntity(
    @ColumnInfo(name = "flat_id") val flatId: String,
    @ColumnInfo(name = "billing_month_id") val billingMonthId: String,
    @ColumnInfo(name = "units_consumed") val unitsConsumed: BigDecimal,
)
