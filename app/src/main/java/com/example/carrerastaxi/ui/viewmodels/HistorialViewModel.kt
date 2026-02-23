package com.example.carrerastaxi.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.carrerastaxi.data.AppDatabase
import com.example.carrerastaxi.data.DailyStatsEntity
import com.example.carrerastaxi.data.repository.HistoryRepository
import com.example.carrerastaxi.domain.usecases.GetHistoryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistorialViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = HistoryRepository(AppDatabase.getDatabase(app))
    private val useCase = GetHistoryUseCase(repo)

    private val _items = MutableLiveData<List<DailyStatsEntity>>(emptyList())
    val items: LiveData<List<DailyStatsEntity>> = _items

    fun load() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { useCase() }
            _items.value = list
        }
    }
}
