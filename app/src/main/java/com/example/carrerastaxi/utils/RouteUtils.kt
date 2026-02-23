package com.example.carrerastaxi.utils

import org.osmdroid.util.GeoPoint
import kotlin.math.abs

object RouteUtils {
    // Douglas-Peucker simplification for GeoPoint list. Epsilon in meters.
    fun simplify(points: List<GeoPoint>, epsilonMeters: Double): List<GeoPoint> {
        if (points.size < 3) return points.toList()

        val kept = BooleanArray(points.size)
        kept[0] = true
        kept[points.size - 1] = true

        fun perpendicularDistance(indexA: Int, indexB: Int, indexX: Int): Double {
            val a = points[indexA]
            val b = points[indexB]
            val x = points[indexX]
            // compute distance from x to line a-b
            val dx = b.longitude - a.longitude
            val dy = b.latitude - a.latitude
            if (abs(dx) < 1e-12 && abs(dy) < 1e-12) {
                return GeoUtils.haversineDistanceMeters(a.latitude, a.longitude, x.latitude, x.longitude)
            }
            // Project x onto line a-b and compute distance
            val t = ((x.longitude - a.longitude) * dx + (x.latitude - a.latitude) * dy) / (dx * dx + dy * dy)
            val projLon = a.longitude + t * dx
            val projLat = a.latitude + t * dy
            return GeoUtils.haversineDistanceMeters(projLat, projLon, x.latitude, x.longitude)
        }

        fun dp(first: Int, last: Int) {
            if (last <= first + 1) return
            var maxDist = 0.0
            var index = -1
            for (i in first + 1 until last) {
                val d = perpendicularDistance(first, last, i)
                if (d > maxDist) {
                    maxDist = d
                    index = i
                }
            }
            if (maxDist > epsilonMeters && index >= 0) {
                kept[index] = true
                dp(first, index)
                dp(index, last)
            }
        }

        dp(0, points.size - 1)
        val out = mutableListOf<GeoPoint>()
        for (i in points.indices) if (kept[i]) out.add(points[i])
        return out
    }
}
