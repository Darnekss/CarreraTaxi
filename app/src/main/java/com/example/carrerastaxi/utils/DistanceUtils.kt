package com.example.carrerastaxi.utils

import org.maplibre.android.geometry.LatLng
import com.google.android.gms.location.Location
import kotlin.math.*

/**
 * Utilidades para cálculos de distancia y velocidad
 */
object DistanceUtils {

    // Costo por km de gasolina: 350 km = 260 Bs
    private const val TOTAL_DISTANCE_REFERENCE = 350.0
    private const val TOTAL_COST_REFERENCE = 260.0
    private const val COST_PER_KM = TOTAL_COST_REFERENCE / TOTAL_DISTANCE_REFERENCE // 0.7428 Bs/km

    /**
     * Calcula la distancia entre dos ubicaciones en metros
     * Usa la fórmula de Haversine
     */
    fun calculateDistance(location1: Location, location2: Location): Float {
        return location1.distanceTo(location2)
    }

    /**
     * Calcula la distancia entre dos coordenadas en metros
     * Usa la fórmula de Haversine
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // Radio de la tierra en metros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    /**
     * Convierte metros a kilómetros
     */
    fun metersToKilometers(meters: Float): Double {
        return (meters / 1000.0)
    }

    /**
     * Convierte metros a kilómetros
     */
    fun metersToKilometers(meters: Double): Double {
        return meters / 1000.0
    }

    /**
     * Convierte velocidad en m/s a km/h
     * La ubicación retorna velocidad en m/s
     */
    fun convertSpeedMsToKmh(speedMs: Float): Double {
        return (speedMs * 3.6).toDouble()
    }

    /**
     * Calcula el consumo de gasolina basado en la distancia recorrida
     * Regla: 350 km = 260 Bs
     * Por lo tanto: distancia * (260/350) = costo de gasolina
     */
    fun calculateGasolineConsumption(distanceKm: Double): Double {
        return distanceKm * COST_PER_KM
    }

    /**
     * Calcula la ganancia neta: monto cobrado - gasolina consumida
     */
    fun calculateProfit(montoCobrado: Double, gasolinaConsumida: Double): Double {
        return (montoCobrado - gasolinaConsumida).coerceAtLeast(0.0)
    }

    /**
     * Calcula la velocidad promedio en km/h
     * velocidadPromedio = distancia (km) / tiempo (horas)
     */
    fun calculateAverageSpeed(distanceKm: Double, durationSeconds: Long): Double {
        if (durationSeconds == 0L) return 0.0
        val durationHours = durationSeconds / 3600.0
        return distanceKm / durationHours
    }

    /**
     * Convierte segundos a formato HH:mm:ss
     */
    fun formatTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    /**
     * Convierte epochTimeMillis a formato de fecha DD/MM/YYYY
     */
    fun formatDate(epochMillis: Long): String {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = epochMillis
        }
        return String.format(
            "%02d/%02d/%04d",
            calendar.get(java.util.Calendar.DAY_OF_MONTH),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.YEAR)
        )
    }

    /**
     * Convierte epochTimeMillis a formato de fecha YYYY-MM-DD
     */
    fun formatDateISO(epochMillis: Long): String {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = epochMillis
        }
        return String.format(
            "%04d-%02d-%02d",
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Convierte epochTimeMillis a formato de hora HH:mm:ss
     */
    fun formatTimeOfDay(epochMillis: Long): String {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = epochMillis
        }
        return String.format(
            "%02d:%02d:%02d",
            calendar.get(java.util.Calendar.HOUR_OF_DAY),
            calendar.get(java.util.Calendar.MINUTE),
            calendar.get(java.util.Calendar.SECOND)
        )
    }

    /**
     * Obtiene la fecha actual en formato YYYY-MM-DD
     */
    fun getTodayDateISO(): String {
        return formatDateISO(System.currentTimeMillis())
    }

    /**
     * Obtiene la hora actual en formato HH:mm:ss
     */
    fun getCurrentTimeOfDay(): String {
        return formatTimeOfDay(System.currentTimeMillis())
    }

    /**
     * Convierte LatLng a String para guardar en BD
     * Formato: lat,lng|lat,lng|...
     */
    fun latLngListToString(points: List<LatLng>): String {
        return points.joinToString("|") { "${it.latitude},${it.longitude}" }
    }

    /**
     * Convierte String a lista de LatLng
     * Inverso de latLngListToString
     */
    fun stringToLatLngList(pointsString: String): List<LatLng> {
        if (pointsString.isEmpty()) return emptyList()
        return pointsString.split("|").mapNotNull { point ->
            val parts = point.split(",")
            if (parts.size == 2) {
                try {
                    LatLng(parts[0].toDouble(), parts[1].toDouble())
                } catch (e: NumberFormatException) {
                    null
                }
            } else {
                null
            }
        }
    }

    /**
     * Redondea un Double a N decimales
     */
    fun roundToDecimals(value: Double, decimals: Int = 2): Double {
        val factor = 10.0.pow(decimals)
        return kotlin.math.round(value * factor) / factor
    }

    /**
     * Formatea un Double como dinero (2 decimales)
     */
    fun formatMoney(value: Double, currency: String = "Bs"): String {
        return String.format("%.2f $currency", value)
    }

    /**
     * Formatea distancia en km (2 decimales)
     */
    fun formatDistance(km: Double): String {
        return String.format("%.2f km", km)
    }

    /**
     * Formatea velocidad en km/h (0 decimales)
     */
    fun formatSpeed(kmh: Double): String {
        return String.format("%.0f km/h", kmh)
    }
}
