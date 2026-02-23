package com.example.carrerastaxi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: SessionStateEntity)

    @Query("SELECT * FROM session_state WHERE id = 1 LIMIT 1")
    suspend fun load(): SessionStateEntity?

    @Query("DELETE FROM session_state")
    suspend fun clear()
}
