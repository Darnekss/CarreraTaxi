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
import com.example.carrerastaxi.managers.AppSession
import com.example.carrerastaxi.managers.TripManager
import com.example.carrerastaxi.models.LocationPoint
import com.example.carrerastaxi.utils.GeoUtils
import com.google.android.gms.location.*
import kotlin.math.abs

class LocationService : Service() {
    private val binder = LocalBinder()
    private lateinit var fused: FusedLocationProviderClient
    private var lastPoint: LocationPoint? = null
    private val tripManager: TripManager = AppSession.tripManager
    private var cachedPrefs: com.example.carrerastaxi.utils.Prefs.Config? = null
    private val profileRepo by lazy { com.example.carrerastaxi.data.repository.VehicleProfileRepositoryImpl(com.example.carrerastaxi.data.AppDatabase.getDatabase(this)) }
    private val getActiveProfile by lazy { com.example.carrerastaxi.domain.usecases.GetActiveProfileUseCase(profileRepo) }

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
        createChannel()
        preloadProfile()
        startForeground(NOTIF_ID, buildNotification("GPS activo"))
    }

    override fun onBind(intent: Intent?): IBinder = binder

    inner class LocalBinder : Binder() {
        fun getService(): LocationService = this@LocationService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_FINALIZE_TRIP) {
            tripManager.finishTrip()
            updateNotification("Carrera finalizada")
            broadcastSnapshot()
            return START_STICKY
        } else if (intent?.action == ACTION_PAUSE_TRIP) {
            tripManager.pauseTrip()
            updateNotification("Carrera pausada")
            broadcastSnapshot()
            return START_STICKY
        } else if (intent?.action == ACTION_RESUME_TRIP) {
            tripManager.resumeTrip()
            updateNotification("Carrera reanudada")
            broadcastSnapshot()
            return START_STICKY
        }
        startUpdates()
        return START_STICKY
    }

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
        if (location.time < now - 30_000) return
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
            if (d > 200 && pt.speedMs < 1.0f) return
        }
        lastPoint = pt
        tripManager.onLocation(pt)
        broadcastSnapshot(pt)
        val snap = tripManager.snapshot(System.currentTimeMillis())
        val prefs = cachedPrefs ?: com.example.carrerastaxi.utils.Prefs.load(this).also { cachedPrefs = it }
        val text = "Estado ${tripManager.tripState().name} | ${"%.1f".format(snap.totalKm)} km | ${"%.0f".format(snap.gross)} Bs | Meta ${"%.0f".format(snap.gross)}/${"%.0f".format(prefs.goal)}"
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
        val finalizeIntent = PendingIntent.getService(
            this, 1,
            Intent(this, LocationService::class.java).setAction(ACTION_FINALIZE_TRIP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pauseIntent = PendingIntent.getService(
            this, 2,
            Intent(this, LocationService::class.java).setAction(ACTION_PAUSE_TRIP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val resumeIntent = PendingIntent.getService(
            this, 3,
            Intent(this, LocationService::class.java).setAction(ACTION_RESUME_TRIP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TAXBolivia")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(intent)
            .addAction(R.drawable.ic_launcher_foreground, "Finalizar", finalizeIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Pausar", pauseIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Reanudar", resumeIntent)
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

    private fun broadcastSnapshot(pt: LocationPoint? = null) {
        val snap = tripManager.snapshot(System.currentTimeMillis())
        val intent = Intent(ACTION_SNAPSHOT).apply {
            putExtra("km", snap.totalKm)
            putExtra("kmWith", snap.kmWithPassenger)
            putExtra("kmWithout", snap.kmWithoutPassenger)
            putExtra("timeTotal", snap.timeTotal)
            putExtra("timeWith", snap.timeWithPassenger)
            putExtra("timeWithout", snap.timeWithoutPassenger)
            putExtra("gross", snap.gross)
            putExtra("state", tripManager.tripState().name)
            putExtra("lat", pt?.latitude ?: lastPoint?.latitude ?: 0.0)
            putExtra("lon", pt?.longitude ?: lastPoint?.longitude ?: 0.0)
            putExtra("speed", pt?.speedMs ?: 0f)
        }
        sendBroadcast(intent)
    }

    companion object {
        private const val CHANNEL_ID = "taxbolivia_gps"
        private const val NOTIF_ID = 2001
        const val ACTION_SNAPSHOT = "com.example.carrerastaxi.SNAPSHOT"
        const val ACTION_FINALIZE_TRIP = "com.example.carrerastaxi.FINALIZE_TRIP"
        const val ACTION_PAUSE_TRIP = "com.example.carrerastaxi.PAUSE_TRIP"
        const val ACTION_RESUME_TRIP = "com.example.carrerastaxi.RESUME_TRIP"
    }

    private fun preloadProfile() {
        kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
            val profile = getActiveProfile() ?: com.example.carrerastaxi.domain.model.VehicleProfile(
                id = 0, type = "TAXI", baseFare = 7.0, pricePerKm = 5.0, pricePerMin = 1.0, kmPerLiter = 10.0, fuelPrice = 7.0, isActive = true
            )
            tripManager.setProfile(profile)
        }
    }
}
