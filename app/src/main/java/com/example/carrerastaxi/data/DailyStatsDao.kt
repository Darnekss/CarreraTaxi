package com.example.carrerastaxi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DailyStatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: DailyStatsEntity)

    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    suspend fun statsByDate(date: String): DailyStatsEntity?

    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT 30")
    suspend fun statsByDateRange(): List<DailyStatsEntity>

    @Query("DELETE FROM daily_stats")
    suspend fun clearAll()

    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT :limit")
    suspend fun lastN(limit: Int): List<DailyStatsEntity>
}
