package com.example.carrerastaxi.utils

object FuelCalculator {
    private const val BS_PER_KM = 0.35 // Ejemplo: 0.35 Bs por kilómetro

    fun calculateFuelCost(distanceMeters: Double): Double {
        val distanceKm = distanceMeters / 1000.0
        return distanceKm * BS_PER_KM
    }
}
