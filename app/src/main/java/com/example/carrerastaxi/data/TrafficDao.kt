package com.example.carrerastaxi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TrafficDao {
    @Insert
    suspend fun insert(entity: TrafficEntity)

    @Query("SELECT * FROM traffic WHERE timestamp > :since")
    suspend fun trafficSince(since: Long): List<TrafficEntity>
}
