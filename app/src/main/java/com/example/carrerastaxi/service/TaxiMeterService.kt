package com.example.carrerastaxi.service

import android.app.Service
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
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

    // Callback para actualizar la UI desde MainActivity
    private var locationUpdateCallback: ((Location) -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): TaxiMeterService = this@TaxiMeterService
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
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
     * Inicia las actualizaciones de ubicación de alta precisión
     */
    private fun startLocationUpdates() {
        if (isRunning) return

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000 // Actualizar cada 2 segundos
        ).apply {
            setMinUpdateDistanceMeters(5f) // Actualizar si se mueve 5 metros
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    locationUpdateCallback?.invoke(location)
                    updateNotification(location)
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
     * Detiene las actualizaciones de ubicación
     */
    fun stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback!!)
            isRunning = false
        }
    }

    /**
     * Registra el callback para actualizar ubicación
     */
    fun setLocationUpdateCallback(callback: (Location) -> Unit) {
        locationUpdateCallback = callback
    }

    /**
     * Crear notificación inicial para primer plano
     */
    private fun createNotification(): NotificationCompat.Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚖 CarrerasTaxi")
            .setContentText("Carrera en curso...")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    /**
     * Actualiza la notificación con datos en vivo
     */
    private fun updateNotification(location: Location) {
        val speedKmh = DistanceUtils.convertSpeedMsToKmh(location.speed)
        val text = "⚡ ${String.format("%.1f", speedKmh)} km/h"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚖 CarrerasTaxi - EN CARRERA")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Crea el canal de notificación (obligatorio en Android 8+)
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
    }
}
