package com.testapp.renttracker.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.testapp.renttracker.data.room.entity.TenantMonthlyChargeEntity

@Dao
interface TenantMonthlyChargeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(charges: List<TenantMonthlyChargeEntity>)

    @Query("DELETE FROM tenant_monthly_charges WHERE billing_month_id = :monthId")
    suspend fun deleteByMonth(monthId: String)

    @Query("SELECT * FROM tenant_monthly_charges WHERE billing_month_id = :monthId")
    suspend fun getByMonth(monthId: String): List<TenantMonthlyChargeEntity>

    @Query("SELECT * FROM tenant_monthly_charges")
    suspend fun getAll(): List<TenantMonthlyChargeEntity>

    @Query("DELETE FROM tenant_monthly_charges WHERE tenant_id = :tenantId")
    suspend fun deleteByTenant(tenantId: String)

    @Transaction
    suspend fun replaceByMonth(monthId: String, charges: List<TenantMonthlyChargeEntity>) {
        deleteByMonth(monthId)
        if (charges.isNotEmpty()) {
            insertAll(charges)
        }
    }
}
