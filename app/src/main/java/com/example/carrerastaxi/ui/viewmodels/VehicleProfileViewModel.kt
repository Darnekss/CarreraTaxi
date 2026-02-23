package com.example.carrerastaxi.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.carrerastaxi.data.AppDatabase
import com.example.carrerastaxi.data.repository.VehicleProfileRepositoryImpl
import com.example.carrerastaxi.domain.model.VehicleProfile
import com.example.carrerastaxi.domain.usecases.GetActiveProfileUseCase
import com.example.carrerastaxi.domain.usecases.ListProfilesUseCase
import com.example.carrerastaxi.domain.usecases.SetActiveProfileUseCase
import com.example.carrerastaxi.domain.usecases.UpsertProfileUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VehicleProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = VehicleProfileRepositoryImpl(AppDatabase.getDatabase(app))
    private val getActive = GetActiveProfileUseCase(repo)
    private val listProfiles = ListProfilesUseCase(repo)
    private val setActive = SetActiveProfileUseCase(repo)
    private val upsert = UpsertProfileUseCase(repo)

    private val _profiles = MutableLiveData<List<VehicleProfile>>(emptyList())
    val profiles: LiveData<List<VehicleProfile>> = _profiles

    private val _active = MutableLiveData<VehicleProfile?>()
    val active: LiveData<VehicleProfile?> = _active

    fun load() {
        viewModelScope.launch {
            val activeProfile = withContext(Dispatchers.IO) { getActive() }
            val list = withContext(Dispatchers.IO) { listProfiles() }
            _active.value = activeProfile ?: list.firstOrNull()
            _profiles.value = list
        }
    }

    fun activate(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            setActive(id)
            load()
        }
    }

    fun save(profile: VehicleProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            upsert(profile)
            load()
        }
    }
}
