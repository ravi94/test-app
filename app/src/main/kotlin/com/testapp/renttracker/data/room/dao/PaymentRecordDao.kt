package com.testapp.renttracker.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.testapp.renttracker.data.room.entity.PaymentRecordEntity

@Dao
interface PaymentRecordDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(payment: PaymentRecordEntity)

    @Query("SELECT * FROM payment_records WHERE billing_month_id = :monthId")
    suspend fun getByMonth(monthId: String): List<PaymentRecordEntity>

    @Query("SELECT * FROM payment_records WHERE tenant_id = :tenantId AND billing_month_id = :monthId")
    suspend fun getByTenantAndMonth(tenantId: String, monthId: String): List<PaymentRecordEntity>

    @Query("SELECT * FROM payment_records")
    suspend fun getAll(): List<PaymentRecordEntity>
}
