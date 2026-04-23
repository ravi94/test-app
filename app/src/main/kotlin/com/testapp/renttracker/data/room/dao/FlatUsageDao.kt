package com.testapp.renttracker.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.testapp.renttracker.data.room.entity.FlatUsageEntity

@Dao
interface FlatUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(usage: FlatUsageEntity)

    @Query("SELECT * FROM flat_usage WHERE billing_month_id = :monthId")
    suspend fun getByMonth(monthId: String): List<FlatUsageEntity>
}
