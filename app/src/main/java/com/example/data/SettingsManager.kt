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

    private val _showWeatherWidget = MutableStateFlow(prefs.getBoolean("show_weather_widget", true))
    val showWeatherWidget: StateFlow<Boolean> = _showWeatherWidget.asStateFlow()

    private val _weatherWidgetStyle = MutableStateFlow(prefs.getString("weather_widget_style", "Glassmorphic") ?: "Glassmorphic")
    val weatherWidgetStyle: StateFlow<String> = _weatherWidgetStyle.asStateFlow()

    private val _weatherWidgetContent = MutableStateFlow(prefs.getString("weather_widget_content", "Full Details") ?: "Full Details")
    val weatherWidgetContent: StateFlow<String> = _weatherWidgetContent.asStateFlow()

    fun setShowWeatherWidget(value: Boolean) {
        prefs.edit().putBoolean("show_weather_widget", value).apply()
        _showWeatherWidget.value = value
    }

    fun setWeatherWidgetStyle(value: String) {
        prefs.edit().putString("weather_widget_style", value).apply()
        _weatherWidgetStyle.value = value
    }

    fun setWeatherWidgetContent(value: String) {
        prefs.edit().putString("weather_widget_content", value).apply()
        _weatherWidgetContent.value = value
    }

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
