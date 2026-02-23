package com.example.carrerastaxi.core

import android.content.Context
import com.example.carrerastaxi.models.LocationPoint
import com.example.carrerastaxi.data.AppDatabase
import com.example.carrerastaxi.data.CarreraEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Maneja el ciclo de vida de una carrera: start / stop / guardar.
 */
class TripManager(private val context: Context, tarifaConfig: CalculationEngine.TarifaConfig) {

    private val calc = CalculationEngine(tarifaConfig)
    private val points = mutableListOf<LocationPoint>()
    private var startedAt: Long = 0

    private val db by lazy { AppDatabase.getDatabase(context).carreraDao() }

    fun startTrip() {
        calc.reset()
        points.clear()
        startedAt = System.currentTimeMillis()
    }

    fun stopTrip(save: Boolean = true) {
        if (save && points.isNotEmpty()) {
            saveTripAsync()
        }
        points.clear()
        startedAt = 0
    }

    fun onLocationPoint(pt: LocationPoint) {
        val prev = points.lastOrNull()
        val deltaSec = if (prev != null) ((pt.timeMs - prev.timeMs) / 1000L).coerceAtLeast(1L) else 1L
        calc.processInterval(prev, pt, deltaSec)
        points.add(pt)
    }

    // Expose read-only snapshot values for UI
    fun isActive(): Boolean = startedAt > 0

    fun getTotalDistanceKm(): Double = calc.totalDistanceMeters / 1000.0

    fun getTotalTimeSeconds(): Long = calc.totalTimeSeconds

    fun getStoppedTimeSeconds(): Long = calc.stoppedTimeSeconds

    fun getMovingTimeSeconds(): Long = calc.movingTimeSeconds

    fun getEarningsDistance(): Double = calc.earningsDistance

    fun getEarningsTime(): Double = calc.earningsTime

    fun getTotalEarnings(): Double = calc.totalEarnings

    fun getLastPoint(): LocationPoint? = points.lastOrNull()

    private fun saveTripAsync() {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val fecha = sdfDate.format(Date(startedAt))
        val horaInicio = sdfTime.format(Date(startedAt))
        val horaFin = sdfTime.format(Date(points.last().timeMs))

        val distanciaKm = calc.totalDistanceMeters / 1000.0
        val duracionSeg = calc.totalTimeSeconds
        val montoCobrado = calc.totalEarnings

        // Estimación de gasolina simple: will be filled by user prefs later
        val gasolinaConsumida = 0.0

        val velocidadPromedio = if (duracionSeg > 0) (distanciaKm / (duracionSeg / 3600.0)) else 0.0

        // Simplify route to reduce stored points (epsilon meters configurable)
        val geoPoints = points.map { org.osmdroid.util.GeoPoint(it.latitude, it.longitude) }
        val simplified = com.example.carrerastaxi.utils.RouteUtils.simplify(geoPoints, 5.0) // 5 meters
        val puntosJson = simplified.joinToString(prefix = "[", postfix = "]") { p -> "{\"lat\":${p.latitude},\"lon\":${p.longitude}}" }

        val entity = CarreraEntity(
            fecha = fecha,
            horaInicio = horaInicio,
            horaFin = horaFin,
            duracionSegundos = duracionSeg,
            distanciaKm = distanciaKm,
            gasolinaConsumida = gasolinaConsumida,
            montoCobrado = montoCobrado,
            ganancia = montoCobrado, // placeholder: cost will be subtracted later
            formaPago = "",
            puntosGPS = puntosJson,
            velocidadPromedio = velocidadPromedio
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.insertCarrera(entity)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }
}
