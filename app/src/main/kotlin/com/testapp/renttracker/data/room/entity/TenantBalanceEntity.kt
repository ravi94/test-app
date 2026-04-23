package com.testapp.renttracker.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.math.BigDecimal

@Entity(
    tableName = "tenant_balances",
    primaryKeys = ["tenant_id", "as_of_month_id"],
    foreignKeys = [
        ForeignKey(
            entity = TenantEntity::class,
            parentColumns = ["id"],
            childColumns = ["tenant_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["tenant_id"], name = "idx_balances_tenant")],
)
data class TenantBalanceEntity(
    @ColumnInfo(name = "tenant_id") val tenantId: String,
    @ColumnInfo(name = "as_of_month_id") val asOfMonthId: String,
    @ColumnInfo(name = "balance_amount") val balanceAmount: BigDecimal,
)
