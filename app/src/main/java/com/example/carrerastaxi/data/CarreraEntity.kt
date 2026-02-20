package com.example.carrerastaxi.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Entidad para almacenar los datos de cada carrera en Room Database
 */
@Entity(tableName = "carreras")
data class CarreraEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "fecha")
    val fecha: String, // Formato: "2025-02-20"

    @ColumnInfo(name = "hora_inicio")
    val horaInicio: String, // Formato: "14:30:45"

    @ColumnInfo(name = "hora_fin")
    val horaFin: String, // Formato: "15:45:20"

    @ColumnInfo(name = "duracion_segundos")
    val duracionSegundos: Long, // Total de segundos

    @ColumnInfo(name = "distancia_km")
    val distanciaKm: Double, // 45.32 km

    @ColumnInfo(name = "gasolina_consumida")
    val gasolinaConsumida: Double, // 15.42 Bs

    @ColumnInfo(name = "monto_cobrado")
    val montoCobrado: Double, // 100.00 Bs

    @ColumnInfo(name = "ganancia")
    val ganancia: Double, // 84.58 Bs

    @ColumnInfo(name = "forma_pago")
    val formaPago: String, // "Efectivo", "Tarjeta", "Apps"

    @ColumnInfo(name = "puntos_gps")
    val puntosGPS: String, // JSON array de coordenadas

    @ColumnInfo(name = "velocidad_promedio")
    val velocidadPromedio: Double, // 32.5 km/h

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
