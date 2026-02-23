package com.example.carrerastaxi.core

import com.example.carrerastaxi.models.LocationPoint
import com.example.carrerastaxi.utils.GeoUtils
import kotlin.math.max

/**
 * Motor de cálculo híbrido basado en velocidad.
 * Actualiza métricas incrementalmente por intervalos.
 */
class CalculationEngine(private val config: TarifaConfig) {

    var totalDistanceMeters: Double = 0.0
        private set
    var totalTimeSeconds: Long = 0
        private set
    var stoppedTimeSeconds: Long = 0
        private set
    var movingTimeSeconds: Long = 0
        private set

    var earningsDistance: Double = 0.0
        private set
    var earningsTime: Double = 0.0
        private set

    val totalEarnings: Double
        get() = config.baseFare + earningsDistance + earningsTime + config.extrasTotal()

    private var lastState: MotionState = MotionState.STOPPED

    fun processInterval(prev: LocationPoint?, curr: LocationPoint, deltaSeconds: Long) {
        if (deltaSeconds <= 0) return

        totalTimeSeconds += deltaSeconds

        val speedKmh = GeoUtils.msToKmh(curr.speedMs).coerceAtLeast(0.0)

        val state = when {
            speedKmh <= config.stoppedThresholdKmh -> MotionState.STOPPED
            speedKmh <= config.trafficThresholdKmh -> MotionState.TRAFFIC
            else -> MotionState.MOVING
        }

        when (state) {
            MotionState.STOPPED -> stoppedTimeSeconds += deltaSeconds
            MotionState.TRAFFIC -> movingTimeSeconds += deltaSeconds
            MotionState.MOVING -> movingTimeSeconds += deltaSeconds
        }

        // Distance calculation
        var deltaMeters = 0.0
        if (prev != null) {
            deltaMeters = GeoUtils.haversineDistanceMeters(prev.latitude, prev.longitude, curr.latitude, curr.longitude)
        }

        // Assign billing weights
        when (state) {
            MotionState.STOPPED -> {
                // charge by time only
                earningsTime += perSecondTimeCharge(deltaSeconds)
            }
            MotionState.TRAFFIC -> {
                // primarily by time, small part by distance
                totalDistanceMeters += deltaMeters
                earningsDistance += deltaMeters / 1000.0 * config.pricePerKm * config.trafficDistanceFactor
                earningsTime += perSecondTimeCharge(deltaSeconds) * config.trafficTimeFactor
            }
            MotionState.MOVING -> {
                totalDistanceMeters += deltaMeters
                earningsDistance += deltaMeters / 1000.0 * config.pricePerKm
                // small time charge for moving (e.g., stops at lights)
                earningsTime += perSecondTimeCharge(deltaSeconds) * config.movingTimeFactor
            }
        }

        lastState = state
    }

    private fun perSecondTimeCharge(seconds: Long): Double {
        val perSec = config.pricePerMinute / 60.0
        return perSec * seconds
    }

    fun reset() {
        totalDistanceMeters = 0.0
        totalTimeSeconds = 0
        stoppedTimeSeconds = 0
        movingTimeSeconds = 0
        earningsDistance = 0.0
        earningsTime = 0.0
        lastState = MotionState.STOPPED
    }

    enum class MotionState { STOPPED, TRAFFIC, MOVING }

    data class TarifaConfig(
        val baseFare: Double = 0.0,
        val pricePerKm: Double = 0.0,
        val pricePerMinute: Double = 0.0,
        val extras: Map<String, Double> = emptyMap(),
        val stoppedThresholdKmh: Double = 3.0,
        val trafficThresholdKmh: Double = 15.0,
        val trafficTimeFactor: Double = 1.0, // multiplier for time charges in traffic
        val trafficDistanceFactor: Double = 0.2, // distance weight in traffic
        val movingTimeFactor: Double = 0.2 // small time charge when moving
    ) {
        fun extrasTotal(): Double = extras.values.sum()
    }
}
