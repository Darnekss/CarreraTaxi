package com.example.carrerastaxi.utils

import android.content.Context

object Prefs {
    private const val FILE = "tax_prefs"

    private const val KEY_BASE = "tarifa_base"
    private const val KEY_KM = "precio_km"
    private const val KEY_MIN = "precio_min"
    private const val KEY_GAS = "precio_gas"
    private const val KEY_KML = "km_litro"
    private const val KEY_SOUND = "sound"
    private const val KEY_VIBRA = "vibra"
    private const val KEY_AUTO = "auto_start"
    private const val KEY_GOAL = "meta_diaria"
    private const val KEY_JOURNEY = "journey_active"

    fun saveTariffs(ctx: Context, base: Double, km: Double, min: Double) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putFloat(KEY_BASE, base.toFloat())
            .putFloat(KEY_KM, km.toFloat())
            .putFloat(KEY_MIN, min.toFloat())
            .apply()
    }

    fun saveFuel(ctx: Context, price: Double, kmPerL: Double) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putFloat(KEY_GAS, price.toFloat())
            .putFloat(KEY_KML, kmPerL.toFloat())
            .apply()
    }

    fun saveToggles(ctx: Context, sound: Boolean, vibra: Boolean) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_SOUND, sound)
            .putBoolean(KEY_VIBRA, vibra)
            .apply()
    }

    fun saveAutoStart(ctx: Context, enabled: Boolean) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_AUTO, enabled)
            .apply()
    }

    fun saveGoal(ctx: Context, goal: Double) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putFloat(KEY_GOAL, goal.toFloat())
            .apply()
    }

    fun saveJourneyActive(ctx: Context, active: Boolean) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_JOURNEY, active)
            .apply()
    }

    data class Config(
        val base: Double,
        val km: Double,
        val min: Double,
        val gas: Double,
        val kmPerL: Double,
        val sound: Boolean,
        val vibra: Boolean,
        val autoStart: Boolean,
        val goal: Double,
        val journeyActive: Boolean
    )

    fun load(ctx: Context): Config {
        val sp = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val base = sp.getFloat(KEY_BASE, 7f).toDouble()
        val km = sp.getFloat(KEY_KM, 5f).toDouble()
        val min = sp.getFloat(KEY_MIN, 1f).toDouble()
        val gas = sp.getFloat(KEY_GAS, 7f).toDouble()
        val kmPerL = sp.getFloat(KEY_KML, 10f).toDouble()
        val sound = sp.getBoolean(KEY_SOUND, false)
        val vibra = sp.getBoolean(KEY_VIBRA, true)
        val auto = sp.getBoolean(KEY_AUTO, false)
        val goal = sp.getFloat(KEY_GOAL, 300f).toDouble()
        val journey = sp.getBoolean(KEY_JOURNEY, true)
        return Config(base, km, min, gas, kmPerL, sound, vibra, auto, goal, journey)
    }
}
