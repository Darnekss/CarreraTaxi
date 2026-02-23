package com.example.carrerastaxi.core

import android.content.Context
import com.example.carrerastaxi.data.AppDatabase
import com.example.carrerastaxi.data.CarreraEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

/**
 * Provee estadísticas y análisis sobre las carreras almacenadas.
 */
class StatisticsManager(private val context: Context) {

    private val dao by lazy { AppDatabase.getDatabase(context).carreraDao() }

    suspend fun totalEarningsByDate(fecha: String): Double = withContext(Dispatchers.IO) {
        dao.getTotalGananciaByDate(fecha) ?: 0.0
    }

    suspend fun totalEarningsByRange(fechaInicio: String, fechaFin: String): Double = withContext(Dispatchers.IO) {
        dao.getTotalGananciaByRange(fechaInicio, fechaFin) ?: 0.0
    }

    suspend fun totalDistanceByRange(fechaInicio: String, fechaFin: String): Double = withContext(Dispatchers.IO) {
        val list = dao.getCarrerasByRange(fechaInicio, fechaFin).first()
        list.sumOf { it.distanciaKm }
    }

    suspend fun earningsPerKm(fechaInicio: String, fechaFin: String): Double = withContext(Dispatchers.IO) {
        val earnings = totalEarningsByRange(fechaInicio, fechaFin)
        val km = totalDistanceByRange(fechaInicio, fechaFin)
        if (km <= 0.0) 0.0 else earnings / km
    }

    suspend fun earningsPerHour(fechaInicio: String, fechaFin: String): Double = withContext(Dispatchers.IO) {
        val list = dao.getCarrerasByRange(fechaInicio, fechaFin).first()
        val totalSeconds = list.sumOf { it.duracionSegundos }
        val hours = totalSeconds / 3600.0
        val earnings = list.sumOf { it.ganancia }
        if (hours <= 0.0) 0.0 else earnings / hours
    }

    suspend fun averageEarningsPerTrip(fechaInicio: String, fechaFin: String): Double = withContext(Dispatchers.IO) {
        val list = dao.getCarrerasByRange(fechaInicio, fechaFin).first()
        if (list.isEmpty()) 0.0 else list.sumOf { it.ganancia } / list.size
    }

    suspend fun bestDay(fechaInicio: String, fechaFin: String): String? = withContext(Dispatchers.IO) {
        val list = dao.getCarrerasByRange(fechaInicio, fechaFin).first()
        val grouped = list.groupBy { it.fecha }
        grouped.maxByOrNull { entry -> entry.value.sumOf { it.ganancia } }?.key
    }

    suspend fun bestHour(fechaInicio: String, fechaFin: String): Int? = withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val list = dao.getCarrerasByRange(fechaInicio, fechaFin).first()
        val grouped = list.groupBy { carrera ->
            try {
                val d = sdf.parse(carrera.horaInicio)
                val cal = Calendar.getInstance()
                cal.time = d
                cal.get(Calendar.HOUR_OF_DAY)
            } catch (t: Throwable) { 0 }
        }
        grouped.maxByOrNull { entry -> entry.value.sumOf { it.ganancia } }?.key
    }

    suspend fun bestTrip(fechaInicio: String, fechaFin: String): CarreraEntity? = withContext(Dispatchers.IO) {
        val list = dao.getCarrerasByRange(fechaInicio, fechaFin).first()
        list.maxByOrNull { it.ganancia }
    }

    suspend fun worstTrip(fechaInicio: String, fechaFin: String): CarreraEntity? = withContext(Dispatchers.IO) {
        val list = dao.getCarrerasByRange(fechaInicio, fechaFin).first()
        list.minByOrNull { it.ganancia }
    }

    suspend fun grossEarnings(fechaInicio: String, fechaFin: String): Double = withContext(Dispatchers.IO) {
        dao.getTotalGananciaByRange(fechaInicio, fechaFin) ?: 0.0
    }

    suspend fun estimatedFuelCost(fechaInicio: String, fechaFin: String, pricePerLiter: Double, kmPerLiter: Double): Double = withContext(Dispatchers.IO) {
        if (kmPerLiter <= 0.0) return@withContext 0.0
        val totalKm = totalDistanceByRange(fechaInicio, fechaFin)
        val liters = totalKm / kmPerLiter
        liters * pricePerLiter
    }

    suspend fun netEarnings(fechaInicio: String, fechaFin: String, pricePerLiter: Double, kmPerLiter: Double): Double = withContext(Dispatchers.IO) {
        val gross = grossEarnings(fechaInicio, fechaFin)
        val fuel = estimatedFuelCost(fechaInicio, fechaFin, pricePerLiter, kmPerLiter)
        gross - fuel
    }
}
