package com.example.carrerastaxi.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.carrerastaxi.data.repository.SettingsRepository

class ConfigViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepository(app)

    data class UiState(
        val base: Double = 7.0,
        val km: Double = 5.0,
        val min: Double = 1.0,
        val gas: Double = 7.0,
        val kml: Double = 10.0,
        val sound: Boolean = false,
        val vibra: Boolean = true,
        val auto: Boolean = false,
        val goal: Double = 300.0,
        val journeyActive: Boolean = true
    )

    private val _ui = MutableLiveData(UiState())
    val ui: LiveData<UiState> = _ui

    fun load() {
        val cfg = repo.load()
        _ui.value = UiState(
            base = cfg.base,
            km = cfg.km,
            min = cfg.min,
            gas = cfg.gas,
            kml = cfg.kmPerL,
            sound = cfg.sound,
            vibra = cfg.vibra,
            auto = cfg.autoStart,
            goal = cfg.goal,
            journeyActive = cfg.journeyActive
        )
    }

    fun save(state: UiState) {
        repo.saveTariffs(state.base, state.km, state.min)
        repo.saveFuel(state.gas, state.kml)
        repo.saveToggles(state.sound, state.vibra)
        repo.saveAutoStart(state.auto)
        repo.saveGoal(state.goal)
        repo.saveJourney(state.journeyActive)
    }

    fun updateJourney(active: Boolean) {
        val current = _ui.value ?: return
        _ui.value = current.copy(journeyActive = active)
        repo.saveJourney(active)
    }
}
