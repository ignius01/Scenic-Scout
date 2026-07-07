package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("scenic_settings", Context.MODE_PRIVATE)

    // Using StateFlows to emit live settings changes to compose
    private val _useFahrenheit = MutableStateFlow(prefs.getBoolean("use_fahrenheit", false))
    val useFahrenheit: StateFlow<Boolean> = _useFahrenheit.asStateFlow()

    private val _useDmsCoordinates = MutableStateFlow(prefs.getBoolean("use_dms_coordinates", false))
    val useDmsCoordinates: StateFlow<Boolean> = _useDmsCoordinates.asStateFlow()

    private val _use24HourFormat = MutableStateFlow(prefs.getBoolean("use_24_hour_format", false))
    val use24HourFormat: StateFlow<Boolean> = _use24HourFormat.asStateFlow()

    private val _enableHaptic = MutableStateFlow(prefs.getBoolean("enable_haptic", true))
    val enableHaptic: StateFlow<Boolean> = _enableHaptic.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(prefs.getLong("last_sync_time", 0L))
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _lastLocalChangeTime = MutableStateFlow(prefs.getLong("last_local_change_time", 0L))
    val lastLocalChangeTime: StateFlow<Long> = _lastLocalChangeTime.asStateFlow()

    fun setUseFahrenheit(value: Boolean) {
        prefs.edit().putBoolean("use_fahrenheit", value).apply()
        _useFahrenheit.value = value
    }

    fun setUseDmsCoordinates(value: Boolean) {
        prefs.edit().putBoolean("use_dms_coordinates", value).apply()
        _useDmsCoordinates.value = value
    }

    fun setUse24HourFormat(value: Boolean) {
        prefs.edit().putBoolean("use_24_hour_format", value).apply()
        _use24HourFormat.value = value
    }

    fun setEnableHaptic(value: Boolean) {
        prefs.edit().putBoolean("enable_haptic", value).apply()
        _enableHaptic.value = value
    }

    fun setLastSyncTime(value: Long) {
        prefs.edit().putLong("last_sync_time", value).apply()
        _lastSyncTime.value = value
    }

    fun setLastLocalChangeTime(value: Long) {
        prefs.edit().putLong("last_local_change_time", value).apply()
        _lastLocalChangeTime.value = value
    }
}
