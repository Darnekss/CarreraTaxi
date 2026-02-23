package com.example.carrerastaxi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey val date: String,
    val totalKm: Double,
    val kmWithPassenger: Double,
    val kmWithoutPassenger: Double,
    val totalTimeSec: Long,
    val timeWithPassengerSec: Long,
    val timeWithoutPassengerSec: Long,
    val grossEarnings: Double,
    val fuelCost: Double,
    val netEarnings: Double
)
