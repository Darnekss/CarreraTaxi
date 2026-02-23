package com.example.carrerastaxi.managers

import com.example.carrerastaxi.data.TrafficEntity
import kotlin.math.max

class TrafficManager {
    fun levelForSpeed(speedKmh: Double): String = when {
        speedKmh < 5 -> "RED"
        speedKmh < 10 -> "YELLOW"
        else -> "GREEN"
    }

    fun createTrafficPoint(lat: Double, lon: Double, speedKmh: Double, now: Long): TrafficEntity {
        return TrafficEntity(
            lat = lat,
            lon = lon,
            speedKmh = speedKmh,
            level = levelForSpeed(speedKmh),
            timestamp = now
        )
    }
}
