package com.example.carrerastaxi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TripDao {
    @Insert
    suspend fun insert(trip: TripEntity): Long

    @Query("SELECT * FROM trips WHERE date = :date")
    suspend fun tripsByDate(date: String): List<TripEntity>

    @Query("SELECT * FROM trips ORDER BY startTime DESC LIMIT 1")
    suspend fun lastTrip(): TripEntity?
}
