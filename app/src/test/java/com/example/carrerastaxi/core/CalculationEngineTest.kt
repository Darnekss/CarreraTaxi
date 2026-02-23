package com.example.carrerastaxi.core

import com.example.carrerastaxi.models.LocationPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculationEngineTest {

    @Test
    fun stopped_charges_by_time() {
        val config = CalculationEngine.TarifaConfig(
            baseFare = 2.0,
            pricePerKm = 10.0,
            pricePerMinute = 6.0
        )
        val engine = CalculationEngine(config)

        val prev: LocationPoint? = LocationPoint(0.0, 0.0, 1000L, 5f, 0f)
        val curr = LocationPoint(0.0, 0.0, 70000L, 5f, 0f) // speed 0 -> stopped

        // Simulate 60 seconds stopped
        engine.processInterval(prev, curr, 60)

        val expectedTimeEarnings = (config.pricePerMinute / 60.0) * 60.0
        val expectedTotal = config.baseFare + expectedTimeEarnings

        assertEquals(expectedTotal, engine.totalEarnings, 1e-6)
    }

    @Test
    fun moving_charges_by_distance() {
        val config = CalculationEngine.TarifaConfig(
            baseFare = 1.0,
            pricePerKm = 8.0,
            pricePerMinute = 6.0
        )
        val engine = CalculationEngine(config)

        val prev = LocationPoint(0.0, 0.0, 0L, 5f, 10f) // moving
        // ~1 km to latitude 0.009
        val curr = LocationPoint(0.009, 0.0, 60000L, 5f, 10f)

        // Simulate 60s interval
        engine.processInterval(prev, curr, 60)

        val distanceKm = engine.totalDistanceMeters / 1000.0
        val expectedDistanceEarnings = distanceKm * config.pricePerKm
        val expectedTimeEarnings = (config.pricePerMinute / 60.0) * 60.0 * config.movingTimeFactor
        val expectedTotal = config.baseFare + expectedDistanceEarnings + expectedTimeEarnings

        assertEquals(expectedTotal, engine.totalEarnings, 1e-3)
    }

    @Test
    fun traffic_charges_mix_time_and_distance() {
        val config = CalculationEngine.TarifaConfig(
            baseFare = 0.5,
            pricePerKm = 10.0,
            pricePerMinute = 6.0,
            trafficDistanceFactor = 0.2,
            trafficTimeFactor = 1.0
        )
        val engine = CalculationEngine(config)

        val prev = LocationPoint(0.0, 0.0, 0L, 5f, 2.5f) // ~9km/h -> traffic
        val curr = LocationPoint(0.001, 0.0, 30000L, 5f, 2.5f)

        engine.processInterval(prev, curr, 30)

        val distanceKm = engine.totalDistanceMeters / 1000.0
        val expectedDistanceEarnings = distanceKm * config.pricePerKm * config.trafficDistanceFactor
        val expectedTimeEarnings = (config.pricePerMinute / 60.0) * 30.0 * config.trafficTimeFactor
        val expectedTotal = config.baseFare + expectedDistanceEarnings + expectedTimeEarnings

        assertEquals(expectedTotal, engine.totalEarnings, 1e-3)
    }
}
