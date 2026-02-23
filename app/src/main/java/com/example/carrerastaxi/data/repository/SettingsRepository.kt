package com.example.carrerastaxi.data.repository

import android.content.Context
import com.example.carrerastaxi.utils.Prefs

class SettingsRepository(private val context: Context) {
    fun load() = Prefs.load(context)
    fun saveTariffs(base: Double, km: Double, min: Double) = Prefs.saveTariffs(context, base, km, min)
    fun saveFuel(price: Double, kml: Double) = Prefs.saveFuel(context, price, kml)
    fun saveToggles(sound: Boolean, vibra: Boolean) = Prefs.saveToggles(context, sound, vibra)
    fun saveAutoStart(auto: Boolean) = Prefs.saveAutoStart(context, auto)
    fun saveGoal(goal: Double) = Prefs.saveGoal(context, goal)
    fun saveJourney(active: Boolean) = Prefs.saveJourneyActive(context, active)
}
