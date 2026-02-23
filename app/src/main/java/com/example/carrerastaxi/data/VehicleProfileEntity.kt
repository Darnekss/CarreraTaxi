package com.example.carrerastaxi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicle_profiles")
data class VehicleProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val baseFare: Double,
    val pricePerKm: Double,
    val pricePerMin: Double,
    val kmPerLiter: Double,
    val fuelPrice: Double,
    val isActive: Boolean = false
)
