package com.example.carrerastaxi.domain.usecases

import com.example.carrerastaxi.domain.repository.VehicleProfileRepository

class SetActiveProfileUseCase(private val repo: VehicleProfileRepository) {
    suspend operator fun invoke(id: Long) = repo.setActive(id)
}
