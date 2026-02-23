package com.example.carrerastaxi.models

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val timeMs: Long,
    val accuracy: Float,
    val speedMs: Float
)
