package com.example.carrerastaxi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VehicleProfileDao {
    @Query("SELECT * FROM vehicle_profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): VehicleProfileEntity?

    @Query("SELECT * FROM vehicle_profiles")
    suspend fun getAll(): List<VehicleProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: VehicleProfileEntity): Long

    @Query("UPDATE vehicle_profiles SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE vehicle_profiles SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: Long)
}
