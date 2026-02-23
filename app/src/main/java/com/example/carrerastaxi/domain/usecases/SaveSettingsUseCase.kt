package com.example.carrerastaxi.domain.usecases

import com.example.carrerastaxi.data.repository.SettingsRepository

class SaveSettingsUseCase(private val repo: SettingsRepository) {
    operator fun invoke(
        base: Double,
        km: Double,
        min: Double,
        gas: Double,
        kml: Double,
        sound: Boolean,
        vibra: Boolean,
        auto: Boolean,
        goal: Double
    ) {
        repo.saveTariffs(base, km, min)
        repo.saveFuel(gas, kml)
        repo.saveToggles(sound, vibra)
        repo.saveAutoStart(auto)
        repo.saveGoal(goal)
    }
}
