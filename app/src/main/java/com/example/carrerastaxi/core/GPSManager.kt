package com.example.carrerastaxi.core

import android.content.Context
import android.location.Location
import com.example.carrerastaxi.models.LocationPoint
import com.example.carrerastaxi.utils.GeoUtils
import com.google.android.gms.location.FusedLocationProviderClient
import kotlin.math.abs

/**
 * Small wrapper that filters raw Android `Location` objects and emits valid `LocationPoint`s.
 */
class GPSManager(
    private val context: Context,
    private val fusedClient: FusedLocationProviderClient
) {

    private var lastPoint: LocationPoint? = null
    private var listener: ((LocationPoint) -> Unit)? = null

    // Filters configuration
    var maxAccuracyMeters: Float = 20f
    var maxSpeedKmh: Double = 120.0
    var maxJumpMetersPerSec: Double = 50.0
    var minDistanceThresholdMeters: Double = 0.5

    fun setListener(l: (LocationPoint) -> Unit) {
        listener = l
    }

    fun onRawLocation(location: Location) {
        val point = LocationPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            timeMs = location.time,
            accuracy = location.accuracy,
            speedMs = location.speed
        )

        if (!isValid(point)) return

        // Avoid reporting micro-movements below threshold
        val prev = lastPoint
        if (prev != null) {
            val d = GeoUtils.haversineDistanceMeters(prev.latitude, prev.longitude, point.latitude, point.longitude)
            if (d < minDistanceThresholdMeters && abs(point.speedMs) < 0.5f) {
                // Consider stationary noise; drop
                return
            }
        }

        lastPoint = point
        listener?.invoke(point)
    }

    private fun isValid(pt: LocationPoint): Boolean {
        if (pt.accuracy > maxAccuracyMeters) return false

        // Speed unrealistic
        val speedKmh = GeoUtils.msToKmh(pt.speedMs)
        if (speedKmh > maxSpeedKmh) return false

        val prev = lastPoint
        if (prev != null) {
            val dt = (pt.timeMs - prev.timeMs) / 1000.0
            if (dt > 0) {
                val d = GeoUtils.haversineDistanceMeters(prev.latitude, prev.longitude, pt.latitude, pt.longitude)
                val v = d / dt
                if (v > maxJumpMetersPerSec) return false
            }
        }

        return true
    }
}
