package com.example.carrerastaxi.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.carrerastaxi.data.AppDatabase
import com.example.carrerastaxi.data.repository.StatsRepository
import com.example.carrerastaxi.domain.usecases.GetTodayStatsUseCase
import com.example.carrerastaxi.domain.usecases.GetHistoryUseCase
import com.example.carrerastaxi.utils.DistanceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = StatsRepository(AppDatabase.getDatabase(app))
    private val todayUseCase = GetTodayStatsUseCase(repo)
    private val historyUseCase = GetHistoryUseCase(com.example.carrerastaxi.data.repository.HistoryRepository(AppDatabase.getDatabase(app)))

    data class StatsUi(
        val km: String = "0.0 km",
        val kmCon: String = "0.0 km",
        val kmSin: String = "0.0 km",
        val tiempo: String = "00:00:00",
        val tiempoCon: String = "00:00:00",
        val tiempoLibre: String = "00:00:00",
        val bruta: String = "0 Bs",
        val gas: String = "0 Bs",
        val neta: String = "0 Bs",
        val historyNet: List<Float> = emptyList()
    )

    private val _ui = MutableLiveData(StatsUi())
    val ui: LiveData<StatsUi> = _ui

    fun load() {
        viewModelScope.launch {
            val today = DistanceUtils.getTodayDateISO()
            val stats = withContext(Dispatchers.IO) { todayUseCase(today) }
            val hist = withContext(Dispatchers.IO) { historyUseCase() }
            _ui.value = StatsUi(
                km = String.format("%.1f km", stats?.totalKm ?: 0.0),
                kmCon = String.format("%.1f km", stats?.kmWithPassenger ?: 0.0),
                kmSin = String.format("%.1f km", stats?.kmWithoutPassenger ?: 0.0),
                tiempo = DistanceUtils.formatTime(stats?.totalTimeSec ?: 0),
                tiempoCon = DistanceUtils.formatTime(stats?.timeWithPassengerSec ?: 0),
                tiempoLibre = DistanceUtils.formatTime(stats?.timeWithoutPassengerSec ?: 0),
                bruta = DistanceUtils.formatMoney(stats?.grossEarnings ?: 0.0, ""),
                gas = DistanceUtils.formatMoney(stats?.fuelCost ?: 0.0, ""),
                neta = DistanceUtils.formatMoney(stats?.netEarnings ?: 0.0, ""),
                historyNet = hist.takeLast(7).map { it.netEarnings.toFloat() }
            )
        }
    }
}
