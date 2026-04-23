package com.testapp.renttracker.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.testapp.renttracker.data.room.entity.FlatEntity

@Dao
interface FlatDao {
    @Query("SELECT COUNT(*) FROM flats")
    suspend fun countAll(): Int

    @Query("SELECT * FROM flats WHERE is_active = 1")
    suspend fun getActiveFlats(): List<FlatEntity>

    @Query("SELECT * FROM flats WHERE id = :flatId LIMIT 1")
    suspend fun getFlatById(flatId: String): FlatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(flat: FlatEntity)

    @Update
    suspend fun update(flat: FlatEntity)
}
