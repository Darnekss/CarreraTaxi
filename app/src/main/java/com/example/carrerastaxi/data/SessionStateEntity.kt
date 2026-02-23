package com.example.carrerastaxi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_state")
data class SessionStateEntity(
    @PrimaryKey val id: Int = 1,
    val journeyActive: Boolean,
    val tripState: String,
    val totalMeters: Double,
    val metersWithPassenger: Double,
    val metersWithoutPassenger: Double,
    val secondsTotal: Long,
    val secondsWithPassenger: Long,
    val secondsWithoutPassenger: Long,
    val secondsStopped: Long,
    val secondsMoving: Long,
    val earnings: Double,
    val earningsDistance: Double,
    val earningsTime: Double,
    val tripMeters: Double,
    val tripSeconds: Long,
    val tripEarnings: Double,
    val lastLat: Double,
    val lastLon: Double,
    val updatedAt: Long
)
