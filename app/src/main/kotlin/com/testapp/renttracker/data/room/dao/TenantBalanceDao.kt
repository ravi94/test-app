package com.testapp.renttracker.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.testapp.renttracker.data.room.entity.TenantBalanceEntity

@Dao
interface TenantBalanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(balance: TenantBalanceEntity)

    @Query("SELECT * FROM tenant_balances WHERE tenant_id = :tenantId AND as_of_month_id = :asOfMonthId LIMIT 1")
    suspend fun getBalance(tenantId: String, asOfMonthId: String): TenantBalanceEntity?

    @Query("SELECT * FROM tenant_balances WHERE as_of_month_id = :asOfMonthId")
    suspend fun getAllForMonth(asOfMonthId: String): List<TenantBalanceEntity>
}
