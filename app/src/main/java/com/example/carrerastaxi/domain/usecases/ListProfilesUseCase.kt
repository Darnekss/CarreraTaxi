package com.example.carrerastaxi.domain.usecases

import com.example.carrerastaxi.domain.repository.VehicleProfileRepository

class ListProfilesUseCase(private val repo: VehicleProfileRepository) {
    suspend operator fun invoke() = repo.list()
}
