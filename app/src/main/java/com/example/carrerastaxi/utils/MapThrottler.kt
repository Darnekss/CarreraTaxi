package com.example.carrerastaxi.utils

import android.os.Handler
import android.os.Looper
import org.osmdroid.util.GeoPoint

/**
 * Agrupa puntos de GPS y llama a `callback` en intervalos para reducir redraws.
 */
class MapThrottler(
    private val intervalMs: Long = 1000L,
    private val callback: (List<GeoPoint>) -> Unit
) {
    private val buffer = mutableListOf<GeoPoint>()
    private val handler = Handler(Looper.getMainLooper())
    private var scheduled = false

    private val flushRunnable = Runnable {
        val toFlush: List<GeoPoint>
        synchronized(buffer) {
            toFlush = buffer.toList()
            buffer.clear()
            scheduled = false
        }
        if (toFlush.isNotEmpty()) callback(toFlush)
    }

    fun addPoint(point: GeoPoint) {
        synchronized(buffer) {
            // avoid exact duplicate consecutive points
            if (buffer.isNotEmpty()) {
                val last = buffer.last()
                if (last.latitude == point.latitude && last.longitude == point.longitude) return
            }
            buffer.add(point)
        }
        if (!scheduled) {
            scheduled = true
            handler.postDelayed(flushRunnable, intervalMs)
        }
    }

    fun flushNow() {
        handler.removeCallbacks(flushRunnable)
        flushRunnable.run()
    }
}
