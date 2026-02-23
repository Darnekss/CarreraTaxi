package com.example.carrerastaxi.data.repository

import com.example.carrerastaxi.data.AppDatabase
import com.example.carrerastaxi.data.VehicleProfileEntity
import com.example.carrerastaxi.domain.model.VehicleProfile
import com.example.carrerastaxi.domain.repository.VehicleProfileRepository

class VehicleProfileRepositoryImpl(private val db: AppDatabase) : VehicleProfileRepository {
    private val dao = db.vehicleProfileDao()

    override suspend fun getActive(): VehicleProfile? {
        return dao.getActive()?.toDomain()
    }

    override suspend fun list(): List<VehicleProfile> {
        return dao.getAll().map { it.toDomain() }
    }

    override suspend fun upsert(profile: VehicleProfile): VehicleProfile {
        val entity = profile.toEntity()
        val id = dao.upsert(entity.copy(id = if (profile.id == 0L) 0 else profile.id))
        return entity.copy(id = if (profile.id == 0L) id else profile.id).toDomain()
    }

    override suspend fun setActive(id: Long) {
        dao.clearActive()
        dao.setActive(id)
    }

    private fun VehicleProfileEntity.toDomain() = VehicleProfile(
        id = id,
        type = type,
        baseFare = baseFare,
        pricePerKm = pricePerKm,
        pricePerMin = pricePerMin,
        kmPerLiter = kmPerLiter,
        fuelPrice = fuelPrice,
        isActive = isActive
    )

    private fun VehicleProfile.toEntity() = VehicleProfileEntity(
        id = id,
        type = type,
        baseFare = baseFare,
        pricePerKm = pricePerKm,
        pricePerMin = pricePerMin,
        kmPerLiter = kmPerLiter,
        fuelPrice = fuelPrice,
        isActive = isActive
    )
}
