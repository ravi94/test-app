package com.testapp.renttracker.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.testapp.renttracker.data.room.entity.TenantEntity

@Dao
interface TenantDao {
    @Query("SELECT COUNT(*) FROM tenants")
    suspend fun countAll(): Int

    @Query("SELECT * FROM tenants WHERE is_active = 1")
    suspend fun getActiveTenants(): List<TenantEntity>

    @Query("SELECT * FROM tenants WHERE flat_id = :flatId AND is_active = 1 LIMIT 1")
    suspend fun getActiveTenantByFlat(flatId: String): TenantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tenant: TenantEntity)
}
