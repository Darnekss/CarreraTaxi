package com.example.carrerastaxi.service

import android.app.Service
import android.app.PendingIntent
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import android.location.Location
import com.example.carrerastaxi.R
import com.example.carrerastaxi.ui.MainActivity
import com.example.carrerastaxi.utils.DistanceUtils

/**
 * Servicio en primer plano que mantiene el GPS activo incluso con pantalla apagada
 */
class TaxiMeterService : Service() {

    private val binder = LocalBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var isRunning = false
    // Adaptive update parameters
    private var currentIntervalMs: Long = 2000L
    private var currentMinDistanceMeters: Float = 5f
    private var lastAdjustmentTime: Long = 0L
    private val adjustmentDebounceMs: Long = 10_000L // don't change more often than 10s

    // Callback para actualizar la UI desde MainActivity
    private var locationUpdateCallback: ((Location) -> Unit)? = null
    // Callback para puntos filtrados (LocationPoint)
    private var filteredLocationCallback: ((com.example.carrerastaxi.models.LocationPoint) -> Unit)? = null
    // New managers
    private var gpsManager: com.example.carrerastaxi.core.GPSManager? = null
    private var tripManager: com.example.carrerastaxi.core.TripManager? = null

    inner class LocalBinder : Binder() {
        fun getService(): TaxiMeterService = this@TaxiMeterService
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        // Initialize managers with sensible defaults
        gpsManager = com.example.carrerastaxi.core.GPSManager(this, fusedLocationClient)
        val tarifa = com.example.carrerastaxi.core.CalculationEngine.TarifaConfig(
            baseFare = 10.0,
            pricePerKm = 5.0,
            pricePerMinute = 1.0
        )
        tripManager = com.example.carrerastaxi.core.TripManager(this, tarifa)
        gpsManager?.setListener { pt ->
            // Forward filtered points to trip manager
            tripManager?.onLocationPoint(pt)
            // Forward filtered point to any UI listener
            filteredLocationCallback?.invoke(pt)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startLocationUpdates()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        startLocationUpdates()
        return binder
    }

    /**
     * Inicia las actualizaciones de ubicacion de alta precision
     */
    private fun startLocationUpdates() {
        if (isRunning) return

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            currentIntervalMs
        ).apply {
            setMinUpdateDistanceMeters(currentMinDistanceMeters)
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                    for (location in result.locations) {
                        // Forward to GPS manager for filtering and processing
                        gpsManager?.onRawLocation(location)
                        updateNotification(location)
                        // Adaptive frequency: adjust request parameters based on speed
                        tryAdjustUpdateFrequency(location)
                    }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            isRunning = true
            startForeground(NOTIFICATION_ID, createNotification())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Detiene las actualizaciones de ubicacion
     */
    fun stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback!!)
            isRunning = false
        }
    }

    private fun tryAdjustUpdateFrequency(location: Location) {
        val speedKmh = DistanceUtils.convertSpeedMsToKmh(location.speed)
        val now = System.currentTimeMillis()
        if (now - lastAdjustmentTime < adjustmentDebounceMs) return

        val (newInterval, newMinDist) = when {
            speedKmh < 10.0 -> Pair(1000L, 1f)
            speedKmh <= 40.0 -> Pair(2000L, 5f)
            else -> Pair(5000L, 10f)
        }

        if (newInterval != currentIntervalMs || newMinDist != currentMinDistanceMeters) {
            // Apply change
            currentIntervalMs = newInterval
            currentMinDistanceMeters = newMinDist
            lastAdjustmentTime = now
            // Re-request location updates with new parameters
            if (locationCallback != null) {
                try {
                    fusedLocationClient.removeLocationUpdates(locationCallback!!)
                } catch (t: Throwable) { t.printStackTrace() }
                try {
                    val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, currentIntervalMs)
                        .setMinUpdateDistanceMeters(currentMinDistanceMeters)
                        .setWaitForAccurateLocation(false)
                        .build()
                    fusedLocationClient.requestLocationUpdates(req, locationCallback!!, Looper.getMainLooper())
                } catch (t: SecurityException) {
                    t.printStackTrace()
                }
            }
        }
    }

    /**
     * Registra el callback para actualizar ubicacion
     */
    fun setLocationUpdateCallback(callback: (Location) -> Unit) {
        locationUpdateCallback = callback
    }

    /**
     * Registra el callback para puntos filtrados (LocationPoint)
     */
    fun setFilteredLocationCallback(callback: (com.example.carrerastaxi.models.LocationPoint) -> Unit) {
        filteredLocationCallback = callback
    }

    /**
     * Crear notificacion inicial para primer plano
     */
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val startIntent = Intent(this, MainActivity::class.java).apply { action = ACTION_START }
        val pauseIntent = Intent(this, MainActivity::class.java).apply { action = ACTION_PAUSE }
        val finishIntent = Intent(this, MainActivity::class.java).apply { action = ACTION_FINISH }

        val startPending = PendingIntent.getActivity(
            this, 1, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pausePending = PendingIntent.getActivity(
            this, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val finishPending = PendingIntent.getActivity(
            this, 3, finishIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CarrerasTaxi")
            .setContentText("Carrera en curso...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(0, "Iniciar", startPending)
            .addAction(0, "Pausar", pausePending)
            .addAction(0, "Finalizar", finishPending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    /**
     * Actualiza la notificacion con datos en vivo
     */
    private fun updateNotification(location: Location) {
        val speedKmh = DistanceUtils.convertSpeedMsToKmh(location.speed)
        val text = "Velocidad: ${String.format("%.1f", speedKmh)} km/h"

        val startIntent = Intent(this, MainActivity::class.java).apply { action = ACTION_START }
        val pauseIntent = Intent(this, MainActivity::class.java).apply { action = ACTION_PAUSE }
        val finishIntent = Intent(this, MainActivity::class.java).apply { action = ACTION_FINISH }

        val startPending = PendingIntent.getActivity(
            this, 1, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pausePending = PendingIntent.getActivity(
            this, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val finishPending = PendingIntent.getActivity(
            this, 3, finishIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CarrerasTaxi - EN CARRERA")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .addAction(0, "Iniciar", startPending)
            .addAction(0, "Pausar", pausePending)
            .addAction(0, "Finalizar", finishPending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Crea el canal de notificacion (obligatorio en Android 8+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CarrerasTaxi GPS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones del servicio de GPS activo"
                enableVibration(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    companion object {
        private const val CHANNEL_ID = "carrerastaxi_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.carrerastaxi.ACTION_START"
        const val ACTION_PAUSE = "com.example.carrerastaxi.ACTION_PAUSE"
        const val ACTION_FINISH = "com.example.carrerastaxi.ACTION_FINISH"
    }
}
