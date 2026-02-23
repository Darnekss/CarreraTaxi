package com.example.carrerastaxi.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    data class Snapshot(
        val km: Double = 0.0,
        val kmWith: Double = 0.0,
        val kmWithout: Double = 0.0,
        val timeTotal: Long = 0,
        val timeWith: Long = 0,
        val timeWithout: Long = 0,
        val gross: Double = 0.0,
        val state: String = "LIBRE",
        val lat: Double = 0.0,
        val lon: Double = 0.0,
        val speedMs: Float = 0f
    )

    private val _state = MutableLiveData(Snapshot())
    val state: LiveData<Snapshot> = _state

    fun updateFromIntent(intent: android.content.Intent) {
        val snap = Snapshot(
            km = intent.getDoubleExtra("km", 0.0),
            kmWith = intent.getDoubleExtra("kmWith", 0.0),
            kmWithout = intent.getDoubleExtra("kmWithout", 0.0),
            timeTotal = intent.getLongExtra("timeTotal", 0),
            timeWith = intent.getLongExtra("timeWith", 0),
            timeWithout = intent.getLongExtra("timeWithout", 0),
            gross = intent.getDoubleExtra("gross", 0.0),
            state = intent.getStringExtra("state") ?: "LIBRE",
            lat = intent.getDoubleExtra("lat", 0.0),
            lon = intent.getDoubleExtra("lon", 0.0),
            speedMs = intent.getFloatExtra("speed", 0f)
        )
        _state.postValue(snap)
    }
}
