package com.example.carrerastaxi.domain.usecases

import com.example.carrerastaxi.data.repository.StatsRepository

class GetTodayStatsUseCase(private val repo: StatsRepository) {
    suspend operator fun invoke(date: String) = repo.getToday(date)
}
