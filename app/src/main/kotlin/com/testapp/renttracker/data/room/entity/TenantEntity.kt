package com.testapp.renttracker.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(
    tableName = "tenants",
    indices = [Index(value = ["flat_label"], name = "idx_tenants_flat_label")],
)
data class TenantEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "flat_label") val flatLabel: String,
    @ColumnInfo(name = "monthly_rent") val monthlyRent: BigDecimal,
    @ColumnInfo(name = "phone") val phone: String?,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "notes") val notes: String?,
)
