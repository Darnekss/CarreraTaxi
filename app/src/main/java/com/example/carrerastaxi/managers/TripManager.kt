package com.example.carrerastaxi.managers

import com.example.carrerastaxi.utils.GeoUtils
import com.example.carrerastaxi.models.LocationPoint
import kotlin.math.max

class TripManager {
    enum class JourneyState { INACTIVE, ACTIVE }
    enum class TripState { LIBRE, CON_PASAJERO, PAUSADO, FINALIZADO, ESPERANDO }

    private var journeyState = JourneyState.INACTIVE
    private var tripState = TripState.LIBRE

    private var startJourneyTime = 0L
    private var startTripTime = 0L
    private var lastPoint: LocationPoint? = null
    private var lastSpeedKmh = 0.0

    var totalMetersDay = 0.0; private set
    var metersWithPassenger = 0.0; private set
    var metersWithoutPassenger = 0.0; private set
    var secondsDay = 0L; private set
    var secondsWithPassenger = 0L; private set
    var secondsWithoutPassenger = 0L; private set
    var secondsStopped = 0L; private set
    var secondsMoving = 0L; private set
    var earningsDistance = 0.0; private set
    var earningsTime = 0.0; private set
    var earnings = 0.0; private set
    private var tripMeters = 0.0
    private var tripSeconds = 0L
    private var tripEarnings = 0.0
    private var tripStoppedSeconds = 0L

    private var pricePerKm = 5.0
    private var pricePerMin = 1.0
    private var baseFare = 7.0
    private var kmPerLiter = 10.0
    private var fuelPrice = 7.0
    private var lastStopAccum = 0L
    private var prevActiveState: TripState = TripState.LIBRE

    fun startJourney(now: Long) {
        journeyState = JourneyState.ACTIVE
        startJourneyTime = now
        resetCounters()
    }

    fun endJourney(now: Long): DailySnapshot {
        val snap = snapshot(now)
        journeyState = JourneyState.INACTIVE
        resetCounters()
        return snap
    }

    fun startTrip(now: Long) {
        tripState = TripState.CON_PASAJERO
        startTripTime = now
        tripMeters = 0.0
        tripSeconds = 0L
        tripEarnings = baseFare
        earnings += baseFare
        prevActiveState = TripState.CON_PASAJERO
    }

    fun pauseTrip() { tripState = TripState.PAUSADO }
    fun resumeTrip() { tripState = TripState.CON_PASAJERO }

    fun finishTrip(now: Long = System.currentTimeMillis()): TripSummary {
        val summary = TripSummary(
            distanceKm = tripMeters / 1000.0,
            durationSec = tripSeconds,
            stoppedSec = tripStoppedSeconds,
            earnings = tripEarnings
        )
        tripState = TripState.LIBRE
        tripMeters = 0.0
        tripSeconds = 0L
        tripEarnings = 0.0
        tripStoppedSeconds = 0
        lastStopAccum = 0
        return summary
    }

    fun setTariffs(base: Double, perKm: Double, perMin: Double) {
        baseFare = base
        pricePerKm = perKm
        pricePerMin = perMin
    }

    fun setProfile(profile: com.example.carrerastaxi.domain.model.VehicleProfile) {
        setTariffs(profile.baseFare, profile.pricePerKm, profile.pricePerMin)
        kmPerLiter = profile.kmPerLiter
        fuelPrice = profile.fuelPrice
    }

    fun onLocation(point: LocationPoint) {
        val prev = lastPoint
        lastPoint = point
        if (journeyState != JourneyState.ACTIVE) return
        if (prev != null) {
            val dt = max(1L, (point.timeMs - prev.timeMs) / 1000)
            val d = GeoUtils.haversineDistanceMeters(prev.latitude, prev.longitude, point.latitude, point.longitude)
            val speedKmh = GeoUtils.msToKmh(point.speedMs)
            lastSpeedKmh = speedKmh
            totalMetersDay += d
            secondsDay += dt

            val stateForSpeed = when {
                speedKmh <= 3 -> SpeedState.DETENIDO
                speedKmh <= 15 -> SpeedState.TRAFICO
                else -> SpeedState.MOVIMIENTO
            }

            if (speedKmh <= 0.5) {
                lastStopAccum += dt
                if (lastStopAccum >= 60 && tripState == TripState.CON_PASAJERO) {
                    prevActiveState = TripState.CON_PASAJERO
                    tripState = TripState.ESPERANDO
                }
            } else {
                if (tripState == TripState.ESPERANDO) {
                    tripState = prevActiveState
                }
                lastStopAccum = 0
            }

            when (tripState) {
                TripState.CON_PASAJERO, TripState.ESPERANDO -> {
                    metersWithPassenger += d
                    secondsWithPassenger += dt
                    tripMeters += d
                    tripSeconds += dt
                    if (stateForSpeed == SpeedState.DETENIDO) {
                        secondsStopped += dt
                        tripStoppedSeconds += dt
                    } else {
                        secondsMoving += dt
                    }

                    val distanceKm = d / 1000.0
                    val minutes = dt / 60.0
                    val (distWeight, timeWeight) = when (stateForSpeed) {
                        SpeedState.DETENIDO -> 0.0 to 1.0
                        SpeedState.TRAFICO -> 0.3 to 0.7
                        SpeedState.MOVIMIENTO -> 0.8 to 0.2
                    }
                    val distCharge = distanceKm * pricePerKm * distWeight
                    val timeCharge = minutes * pricePerMin * timeWeight
                    earningsDistance += distCharge
                    earningsTime += timeCharge
                    earnings += distCharge + timeCharge
                    tripEarnings += distCharge + timeCharge
                }
                TripState.LIBRE, TripState.PAUSADO -> {
                    metersWithoutPassenger += d
                    secondsWithoutPassenger += dt
                    if (speedKmh <= 3) secondsStopped += dt else secondsMoving += dt
                }
                TripState.FINALIZADO -> {}
            }
        } else {
            // first point no distance, still store
        }
    }

    fun snapshot(now: Long): DailySnapshot {
        return DailySnapshot(
            totalKm = totalMetersDay / 1000.0,
            kmWithPassenger = metersWithPassenger / 1000.0,
            kmWithoutPassenger = metersWithoutPassenger / 1000.0,
            timeTotal = secondsDay,
            timeWithPassenger = secondsWithPassenger,
            timeWithoutPassenger = secondsWithoutPassenger,
            timeStopped = secondsStopped,
            timeMoving = secondsMoving,
            gross = earnings,
            earningsDistance = earningsDistance,
            earningsTime = earningsTime
        )
    }

    fun toSession(lastLat: Double, lastLon: Double): SessionSnapshot {
        return SessionSnapshot(
            journeyActive = journeyState == JourneyState.ACTIVE,
            tripState = tripState,
            totalMeters = totalMetersDay,
            metersWithPassenger = metersWithPassenger,
            metersWithoutPassenger = metersWithoutPassenger,
            secondsTotal = secondsDay,
            secondsWithPassenger = secondsWithPassenger,
            secondsWithoutPassenger = secondsWithoutPassenger,
            secondsStopped = secondsStopped,
            secondsMoving = secondsMoving,
            earnings = earnings,
            earningsDistance = earningsDistance,
            earningsTime = earningsTime,
            tripMeters = tripMeters,
            tripSeconds = tripSeconds,
            tripEarnings = tripEarnings,
            lastLat = lastLat,
            lastLon = lastLon
        )
    }

    fun restoreFrom(session: SessionSnapshot) {
        journeyState = if (session.journeyActive) JourneyState.ACTIVE else JourneyState.INACTIVE
        tripState = session.tripState
        totalMetersDay = session.totalMeters
        metersWithPassenger = session.metersWithPassenger
        metersWithoutPassenger = session.metersWithoutPassenger
        secondsDay = session.secondsTotal
        secondsWithPassenger = session.secondsWithPassenger
        secondsWithoutPassenger = session.secondsWithoutPassenger
        secondsStopped = session.secondsStopped
        secondsMoving = session.secondsMoving
        earnings = session.earnings
        earningsDistance = session.earningsDistance
        earningsTime = session.earningsTime
        tripMeters = session.tripMeters
        tripSeconds = session.tripSeconds
        tripEarnings = session.tripEarnings
    }

    fun tripState(): TripState = tripState
    fun journeyActive(): Boolean = journeyState == JourneyState.ACTIVE
    fun currentTripMeters(): Double = tripMeters
    fun currentTripSeconds(): Long = tripSeconds
    fun currentTripEarnings(): Double = tripEarnings
    fun currentSpeedKmh(): Double = lastSpeedKmh

    private fun resetCounters() {
        totalMetersDay = 0.0
        metersWithPassenger = 0.0
        metersWithoutPassenger = 0.0
        secondsDay = 0
        secondsWithPassenger = 0
        secondsWithoutPassenger = 0
        secondsStopped = 0
        secondsMoving = 0
        earnings = 0.0
        earningsDistance = 0.0
        earningsTime = 0.0
        lastPoint = null
        tripMeters = 0.0
        tripSeconds = 0L
        tripEarnings = 0.0
        tripStoppedSeconds = 0L
        lastStopAccum = 0
    }

    data class DailySnapshot(
        val totalKm: Double,
        val kmWithPassenger: Double,
        val kmWithoutPassenger: Double,
        val timeTotal: Long,
        val timeWithPassenger: Long,
        val timeWithoutPassenger: Long,
        val timeStopped: Long,
        val timeMoving: Long,
        val gross: Double,
        val earningsDistance: Double,
        val earningsTime: Double
    )

    data class TripSummary(
        val distanceKm: Double,
        val durationSec: Long,
        val stoppedSec: Long,
        val earnings: Double
    )

    private enum class SpeedState { DETENIDO, TRAFICO, MOVIMIENTO }

    data class SessionSnapshot(
        val journeyActive: Boolean,
        val tripState: TripState,
        val totalMeters: Double,
        val metersWithPassenger: Double,
        val metersWithoutPassenger: Double,
        val secondsTotal: Long,
        val secondsWithPassenger: Long,
        val secondsWithoutPassenger: Long,
        val secondsStopped: Long,
        val secondsMoving: Long,
        val earnings: Double,
        val earningsDistance: Double,
        val earningsTime: Double,
        val tripMeters: Double,
        val tripSeconds: Long,
        val tripEarnings: Double,
        val lastLat: Double,
        val lastLon: Double
    )
}
