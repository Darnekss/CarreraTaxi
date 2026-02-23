package com.example.carrerastaxi.domain.usecases

import com.example.carrerastaxi.domain.model.VehicleProfile
import com.example.carrerastaxi.domain.repository.VehicleProfileRepository

class UpsertProfileUseCase(private val repo: VehicleProfileRepository) {
    suspend operator fun invoke(profile: VehicleProfile) = repo.upsert(profile)
}
