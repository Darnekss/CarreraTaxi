package com.example.carrerastaxi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long?,
    val distanceMeters: Double,
    val distanceWithPassengerMeters: Double,
    val distanceWithoutPassengerMeters: Double,
    val durationSeconds: Long,
    val durationWithPassengerSeconds: Long,
    val durationWithoutPassengerSeconds: Long,
    val earnings: Double,
    val passengerCount: Int,
    val state: String,
    val date: String
)
