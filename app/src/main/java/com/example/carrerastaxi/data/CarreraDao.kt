package com.example.carrerastaxi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object para operaciones de base de datos con carreras
 */
@Dao
interface CarreraDao {

    /**
     * Inserta una nueva carrera en la base de datos
     */
    @Insert
    suspend fun insertCarrera(carrera: CarreraEntity): Long

    /**
     * Actualiza una carrera existente
     */
    @Update
    suspend fun updateCarrera(carrera: CarreraEntity)

    /**
     * Elimina una carrera
     */
    @Delete
    suspend fun deleteCarrera(carrera: CarreraEntity)

    /**
     * Obtiene todas las carreras ordenadas por fecha descendente
     */
    @Query("SELECT * FROM carreras ORDER BY timestamp DESC")
    fun getAllCarreras(): Flow<List<CarreraEntity>>

    /**
     * Obtiene una carrera por ID
     */
    @Query("SELECT * FROM carreras WHERE id = :id")
    suspend fun getCarreraById(id: Int): CarreraEntity?

    /**
     * Obtiene carreras de un día específico
     */
    @Query("SELECT * FROM carreras WHERE fecha = :fecha ORDER BY timestamp DESC")
    fun getCarrerasByDate(fecha: String): Flow<List<CarreraEntity>>

    /**
     * Obtiene suma total de ganancias por fecha
     */
    @Query("SELECT SUM(ganancia) FROM carreras WHERE fecha = :fecha")
    suspend fun getTotalGananciaByDate(fecha: String): Double?

    /**
     * Obtiene suma total de ganancias en un rango de fechas
     */
    @Query("SELECT SUM(ganancia) FROM carreras WHERE fecha BETWEEN :fechaInicio AND :fechaFin")
    suspend fun getTotalGananciaByRange(fechaInicio: String, fechaFin: String): Double?

    /**
     * Obtiene la distancia total recorrida
     */
    @Query("SELECT SUM(distancia_km) FROM carreras WHERE fecha = :fecha")
    suspend fun getTotalDistanciaByDate(fecha: String): Double?

    /**
     * Obtiene el número de carreras en un día
     */
    @Query("SELECT COUNT(*) FROM carreras WHERE fecha = :fecha")
    suspend fun getCountByDate(fecha: String): Int

    /**
     * Elimina todas las carreras más antiguas de N días
     */
    @Query("DELETE FROM carreras WHERE fecha < :fechaLimite")
    suspend fun deleteOldCarreras(fechaLimite: String)

    /**
     * Obtiene estadísticas del mes actual
     */
    @Query("""
        SELECT 
            COUNT(*) as totalCarreras,
            COALESCE(SUM(ganancia), 0.0) as totalGanancia,
            COALESCE(SUM(distancia_km), 0.0) as totalDistancia,
            COALESCE(AVG(velocidad_promedio), 0.0) as velocidadPromedio
        FROM carreras 
        WHERE strftime('%Y-%m', fecha) = :yearMonth
    """)
    suspend fun getMonthlyStats(yearMonth: String): MonthlyStats?
}

/**
 * Clase de datos para estadísticas mensuales
 */
data class MonthlyStats(
    val totalCarreras: Int,
    val totalGanancia: Double,
    val totalDistancia: Double,
    val velocidadPromedio: Double
)
