package com.example.carrerastaxi.data.repository

import com.example.carrerastaxi.data.AppDatabase
import com.example.carrerastaxi.data.DailyStatsEntity

class HistoryRepository(private val db: AppDatabase) {
    suspend fun list(limit: Int = 30): List<DailyStatsEntity> = db.dailyStatsDao().statsByDateRange()
}
