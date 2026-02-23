package com.example.carrerastaxi.data.repository

import com.example.carrerastaxi.data.AppDatabase
import com.example.carrerastaxi.data.DailyStatsEntity

class StatsRepository(private val db: AppDatabase) {
    suspend fun getToday(date: String): DailyStatsEntity? = db.dailyStatsDao().statsByDate(date)
    suspend fun lastNDays(n: Int): List<DailyStatsEntity> = db.dailyStatsDao().lastN(n)
}
