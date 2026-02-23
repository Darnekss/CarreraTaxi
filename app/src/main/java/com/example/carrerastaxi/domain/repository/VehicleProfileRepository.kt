package com.example.carrerastaxi.domain.repository

import com.example.carrerastaxi.domain.model.VehicleProfile

interface VehicleProfileRepository {
    suspend fun getActive(): VehicleProfile?
    suspend fun list(): List<VehicleProfile>
    suspend fun upsert(profile: VehicleProfile): VehicleProfile
    suspend fun setActive(id: Long)
}
