package com.example.carrerastaxi.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.carrerastaxi.core.TripManager
import com.example.carrerastaxi.core.CalculationEngine
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * ViewModel que expone estado del taxímetro en tiempo real para la UI.
 */
class MeterViewModel(application: Application) : AndroidViewModel(application) {

    private val fused = LocationServices.getFusedLocationProviderClient(application)
    private val tarifa = CalculationEngine.TarifaConfig(baseFare = 7.0, pricePerKm = 5.0, pricePerMinute = 1.0)
    private val tripManager = TripManager(application.applicationContext, tarifa)

    private val _fare = MutableStateFlow(0.0)
    val fare: StateFlow<Double> = _fare

    private val _state = MutableStateFlow("STOPPED")
    val state: StateFlow<String> = _state

    private val _distanceKm = MutableStateFlow(0.0)
    val distanceKm: StateFlow<Double> = _distanceKm

    private val _timeSeconds = MutableStateFlow(0L)
    val timeSeconds: StateFlow<Long> = _timeSeconds

    private val _progressToGoal = MutableStateFlow(0f)
    val progressToGoal: StateFlow<Float> = _progressToGoal

    private var dailyGoal: Double = 0.0

    fun setDailyGoal(goal: Double) {
        dailyGoal = max(0.0, goal)
        updateProgress()
    }

    fun startTrip() {
        tripManager.startTrip()
        _state.value = "RUNNING"
        viewModelScope.launch { refreshSnapshot() }
    }

    fun stopTrip() {
        tripManager.stopTrip(true)
        _state.value = "STOPPED"
        viewModelScope.launch { refreshSnapshot() }
    }

    fun emergency() {
        // Save last known point and emit emergency state for UI to act on
        _state.value = "EMERGENCY"
        // TODO: persist emergency event, notify server, send alert
    }

    fun onNewPoint() {
        viewModelScope.launch { refreshSnapshot() }
    }

    private suspend fun refreshSnapshot() {
        _distanceKm.value = tripManager.getTotalDistanceKm()
        _timeSeconds.value = tripManager.getTotalTimeSeconds()
        _fare.value = tripManager.getTotalEarnings()
        _state.value = if (tripManager.isActive()) "RUNNING" else "STOPPED"
        updateProgress()
    }

    private fun updateProgress() {
        _progressToGoal.value = if (dailyGoal <= 0.0) 0f else (tripManager.getTotalEarnings() / dailyGoal).toFloat().coerceIn(0f, 1f)
    }
}
