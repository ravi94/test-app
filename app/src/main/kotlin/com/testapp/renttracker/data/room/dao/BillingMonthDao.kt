package com.testapp.renttracker.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.testapp.renttracker.data.room.entity.BillingMonthEntity

@Dao
interface BillingMonthDao {
    @Query("SELECT * FROM billing_months WHERE id = :monthId LIMIT 1")
    suspend fun getById(monthId: String): BillingMonthEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(month: BillingMonthEntity)

    @Update
    suspend fun update(month: BillingMonthEntity)
}
