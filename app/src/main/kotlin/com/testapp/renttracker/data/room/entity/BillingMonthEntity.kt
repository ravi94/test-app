package com.testapp.renttracker.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.testapp.renttracker.model.BillingMonthStatus
import java.math.BigDecimal

@Entity(tableName = "billing_months")
data class BillingMonthEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "electricity_total_amount") val electricityTotalAmount: BigDecimal,
    @ColumnInfo(name = "status") val status: BillingMonthStatus,
)
