package com.example.carrerastaxi.managers

import com.example.carrerastaxi.data.DailyStatsDao
import com.example.carrerastaxi.data.DailyStatsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StatsManager(
    private val dao: DailyStatsDao,
    private val scope: CoroutineScope
) {
    fun saveDaily(stats: DailyStatsEntity) {
        scope.launch(Dispatchers.IO) {
            dao.upsert(stats)
        }
    }
}
