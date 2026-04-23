package com.testapp.renttracker.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tenants",
    foreignKeys = [
        ForeignKey(
            entity = FlatEntity::class,
            parentColumns = ["id"],
            childColumns = ["flat_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["flat_id"], name = "idx_tenants_flat_id")],
)
data class TenantEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "flat_id") val flatId: String,
    @ColumnInfo(name = "phone") val phone: String?,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "notes") val notes: String?,
)
