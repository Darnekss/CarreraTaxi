package com.example.carrerastaxi.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.carrerastaxi.R
import com.example.carrerastaxi.models.LocationPoint
import com.example.carrerastaxi.utils.GeoUtils
import com.google.android.gms.location.*
import kotlin.math.abs

class LocationService : Service() {
    private val binder = LocalBinder()
    private lateinit var fused: FusedLocationProviderClient
    private var callback: ((LocationPoint) -> Unit)? = null
    private var lastPoint: LocationPoint? = null

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
        createChannel()
        startForeground(NOTIF_ID, buildNotification("GPS activo"))
    }

    override fun onBind(intent: Intent?): IBinder = binder

    inner class LocalBinder : Binder() {
        fun getService(): LocationService = this@LocationService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startUpdates()
        return START_STICKY
    }

    fun setListener(cb: (LocationPoint) -> Unit) { callback = cb }

    fun startUpdates() {
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMinUpdateIntervalMillis(1000)
            .setMinUpdateDistanceMeters(1f)
            .setWaitForAccurateLocation(true)
            .setMaxUpdateAgeMillis(3000)
            .build()
        fused.requestLocationUpdates(req, locationCallback, Looper.getMainLooper())
    }

    fun stopUpdates() {
        fused.removeLocationUpdates(locationCallback)
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            for (loc in result.locations) {
                process(loc)
            }
        }
    }

    private fun process(location: Location) {
        val now = System.currentTimeMillis()
        if (location.time < now - 30_000) return // descarta posiciones cacheadas viejas

        val pt = LocationPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            timeMs = location.time,
            accuracy = location.accuracy,
            speedMs = location.speed
        )
        if (!isValid(pt)) return
        val prev = lastPoint
        if (prev != null) {
            val d = GeoUtils.haversineDistanceMeters(prev.latitude, prev.longitude, pt.latitude, pt.longitude)
            if (d < 0.5 && abs(pt.speedMs) < 0.5f) return
            if (d > 200 && pt.speedMs < 1.0f) return // salto improbable
        }
        lastPoint = pt
        callback?.invoke(pt)
        val text = "Vel: ${"%.1f".format(GeoUtils.msToKmh(pt.speedMs))} km/h"
        updateNotification(text)
    }

    private fun isValid(pt: LocationPoint): Boolean {
        if (pt.accuracy > 20f) return false
        if (pt.latitude == 0.0 && pt.longitude == 0.0) return false
        val speedKmh = GeoUtils.msToKmh(pt.speedMs)
        if (speedKmh > 120) return false
        val prev = lastPoint
        if (prev != null) {
            val dt = (pt.timeMs - prev.timeMs) / 1000.0
            if (dt > 0) {
                val d = GeoUtils.haversineDistanceMeters(prev.latitude, prev.longitude, pt.latitude, pt.longitude)
                val v = d / dt
                if (v > 50.0) return false
            }
        }
        return true
    }

    private fun buildNotification(text: String): Notification {
        val intent = PendingIntent.getActivity(
            this, 0,
            Intent(this, com.example.carrerastaxi.ui.MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TAXBolivia")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(intent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm?.notify(NOTIF_ID, buildNotification(text))
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "GPS", NotificationManager.IMPORTANCE_LOW)
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(ch)
        }
    }

    companion object {
        private const val CHANNEL_ID = "taxbolivia_gps"
        private const val NOTIF_ID = 2001
    }
}
