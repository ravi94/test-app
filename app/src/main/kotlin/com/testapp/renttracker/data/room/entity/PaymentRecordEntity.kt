package com.testapp.renttracker.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.testapp.renttracker.model.PaymentComponent
import java.math.BigDecimal
import java.time.LocalDate

@Entity(
    tableName = "payment_records",
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
    indices = [Index(value = ["tenant_id", "billing_month_id"], name = "idx_payments_tenant_month")],
)
data class PaymentRecordEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "tenant_id") val tenantId: String,
    @ColumnInfo(name = "billing_month_id") val billingMonthId: String,
    @ColumnInfo(name = "component") val component: PaymentComponent,
    @ColumnInfo(name = "amount_paid") val amountPaid: BigDecimal,
    @ColumnInfo(name = "paid_on") val paidOn: LocalDate,
    @ColumnInfo(name = "note") val note: String?,
)
