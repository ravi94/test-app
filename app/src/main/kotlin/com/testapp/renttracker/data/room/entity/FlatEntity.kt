package com.testapp.renttracker.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "flats")
data class FlatEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "unit_label") val unitLabel: String,
    @ColumnInfo(name = "fixed_monthly_rent") val fixedMonthlyRent: BigDecimal,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "notes") val notes: String?,
)
