package com.example.carrerastaxi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "traffic")
data class TrafficEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lat: Double,
    val lon: Double,
    val speedKmh: Double,
    val level: String,
    val timestamp: Long
)
