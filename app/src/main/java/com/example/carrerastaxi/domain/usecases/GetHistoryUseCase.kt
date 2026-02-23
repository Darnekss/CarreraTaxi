package com.example.carrerastaxi.domain.usecases

import com.example.carrerastaxi.data.repository.HistoryRepository

class GetHistoryUseCase(private val repo: HistoryRepository) {
    suspend operator fun invoke() = repo.list()
}
