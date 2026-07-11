package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.ScenicPin
import com.example.domain.ScenicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.FlowPreview

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ScenicViewModel @Inject constructor(
    private val repository: ScenicRepository,
    val settingsManager: com.example.data.SettingsManager,
    val firebaseBackupManager: com.example.data.FirebaseBackupManager
) : ViewModel() {

    private var isRestoringBackup = false

    private val _quickScoutTrigger = Channel<Unit>(Channel.BUFFERED)
    val quickScoutTrigger = _quickScoutTrigger.receiveAsFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    fun setHasUnsavedChanges(value: Boolean) {
        _hasUnsavedChanges.value = value
    }

    fun triggerQuickScoutFromTile() {
        viewModelScope.launch {
            _quickScoutTrigger.send(Unit)
        }
    }

    val allPins: StateFlow<List<ScenicPin>> = repository.getAllPins()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            @OptIn(FlowPreview::class)
            allPins
                .debounce(500)
                .collect { pins ->
                    if (firebaseBackupManager.isLoggedIn && !isRestoringBackup) {
                        val result = firebaseBackupManager.backupPins(pins)
                        result.onSuccess {
                            val now = System.currentTimeMillis()
                            settingsManager.setLastSyncTime(now)
                            settingsManager.setLastLocalChangeTime(now)
                        }
                    }
                }
        }

        viewModelScope.launch {
            // At startup, if signed in, automatically restore/sync pins from the cloud to merge them
            if (firebaseBackupManager.isLoggedIn) {
                restorePinsFromBackup(onSuccess = {}, onFailure = {})
            }
        }
    }

    private val _selectedPinId = MutableStateFlow<Long?>(null)
    val selectedPinId: StateFlow<Long?> = _selectedPinId.asStateFlow()

    val selectedPin: StateFlow<ScenicPin?> = combine(_selectedPinId, allPins) { id, pins ->
        if (id == null) null else pins.find { it.id == id }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun selectPin(pin: ScenicPin?) {
        _selectedPinId.value = pin?.id
    }

    fun addPin(
        name: String,
        latitude: Double,
        longitude: Double,
        landscapeType: String,
        timeOfDayCategory: String,
        filmStock: String = "",
        iso: Int = 100,
        aperture: String = "f/8",
        notes: String = "",
        photoUri: String? = null,
        shutterSpeed: String = "1/125s",
        context: Context
    ) {
        viewModelScope.launch {
            val pin = ScenicPin(
                name = name,
                latitude = latitude,
                longitude = longitude,
                timestamp = System.currentTimeMillis(),
                landscapeType = landscapeType,
                timeOfDayCategory = timeOfDayCategory,
                filmStock = filmStock,
                iso = iso,
                aperture = aperture,
                notes = notes,
                photoUri = photoUri,
                shutterSpeed = shutterSpeed
            )
            repository.insertPin(pin)
            settingsManager.setLastLocalChangeTime(System.currentTimeMillis())
        }
    }

    fun updatePin(pin: ScenicPin) {
        viewModelScope.launch {
            val updated = pin.copy(timestamp = System.currentTimeMillis())
            repository.updatePin(updated)
            settingsManager.setLastLocalChangeTime(System.currentTimeMillis())
        }
    }

    fun deletePin(pin: ScenicPin) {
        viewModelScope.launch {
            repository.deletePin(pin)
            settingsManager.setLastLocalChangeTime(System.currentTimeMillis())
            if (_selectedPinId.value == pin.id) {
                _selectedPinId.value = null
            }
        }
    }

    fun syncWeatherForPin(pinId: Long) {
        viewModelScope.launch {
            repository.syncWeatherForPin(pinId)
            settingsManager.setLastLocalChangeTime(System.currentTimeMillis())
        }
    }

    fun backupPinsToCloud(onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) {
        viewModelScope.launch {
            val pins = allPins.value
            val result = firebaseBackupManager.backupPins(pins)
            result.onSuccess {
                val now = System.currentTimeMillis()
                settingsManager.setLastSyncTime(now)
                settingsManager.setLastLocalChangeTime(now) // synchronized
                onSuccess()
            }.onFailure { exception ->
                onFailure(exception)
            }
        }
    }

    fun restorePinsFromBackup(onSuccess: (Int) -> Unit, onFailure: (Throwable) -> Unit) {
        viewModelScope.launch {
            isRestoringBackup = true
            try {
                val existingPins = allPins.value
                val toInsert = mutableListOf<ScenicPin>()
                val result = firebaseBackupManager.restorePins { pin ->
                    val alreadyExists = existingPins.any { it.timestamp == pin.timestamp && it.name == pin.name }
                    if (!alreadyExists) {
                        toInsert.add(pin)
                    }
                }
                result.onSuccess { count ->
                    if (toInsert.isNotEmpty()) {
                        repository.insertPins(toInsert)
                    }
                    val now = System.currentTimeMillis()
                    settingsManager.setLastSyncTime(now)
                    settingsManager.setLastLocalChangeTime(now) // synchronized
                    onSuccess(count)
                }.onFailure { exception ->
                    onFailure(exception)
                }
            } finally {
                isRestoringBackup = false
            }
        }
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) {
        viewModelScope.launch {
            val result = firebaseBackupManager.signInWithGoogle(idToken)
            result.onSuccess {
                onSuccess()
            }.onFailure { exception ->
                onFailure(exception)
            }
        }
    }

    /**
     * Fetches current weather for the given coordinates by delegating to the repository.
     * 
     * Keeps the presentation layer decoupled from Direct API/Network clients.
     */
    suspend fun fetchCurrentWeather(latitude: Double, longitude: Double): Result<com.example.data.WeatherResponse> {
        return repository.fetchWeather(latitude, longitude)
    }

}
