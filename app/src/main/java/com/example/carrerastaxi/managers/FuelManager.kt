package com.example.carrerastaxi.managers

class FuelManager(
    private var kmPerLiter: Double = 10.0,
    private var pricePerLiter: Double = 7.0
) {
    fun updateConfig(kmPerL: Double, price: Double) {
        kmPerLiter = kmPerL
        pricePerLiter = price
    }

    fun fuelCost(distanceKm: Double): Pair<Double, Double> {
        if (kmPerLiter <= 0) return 0.0 to 0.0
        val liters = distanceKm / kmPerLiter
        val cost = liters * pricePerLiter
        return liters to cost
    }
}
