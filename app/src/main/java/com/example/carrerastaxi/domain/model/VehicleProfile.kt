package com.example.carrerastaxi.domain.model

data class VehicleProfile(
    val id: Long = 0,
    val type: String,
    val baseFare: Double,
    val pricePerKm: Double,
    val pricePerMin: Double,
    val kmPerLiter: Double,
    val fuelPrice: Double,
    val isActive: Boolean
)
