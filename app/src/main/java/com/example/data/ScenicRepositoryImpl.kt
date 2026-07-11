package com.example.data

import android.content.Context
import android.util.Log
import com.example.domain.CelestialCalculator
import com.example.domain.ScenicPin
import com.example.domain.ScenicRepository
import com.example.background.ScoutGlanceWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ScenicRepositoryImpl(
    private val pinDao: ScenicPinDao,
    private val weatherApi: WeatherApi,
    private val context: Context
) : ScenicRepository {

    private suspend fun triggerWidgetUpdate() {
        try {
            ScoutGlanceWidget().updateAll(context)
            com.example.background.ScenicDialWidget().updateAll(context)
            com.example.background.ScenicListWidget().updateAll(context)
            Log.d("ScenicRepository", "Triggered Glance widget update successfully")
        } catch (e: Exception) {
            Log.e("ScenicRepository", "Failed to update Glance widgets: ${e.localizedMessage}")
        }
    }

    override fun getAllPins(): Flow<List<ScenicPin>> {
        return pinDao.getAllPins().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPinById(id: Long): Flow<ScenicPin?> {
        return pinDao.getPinById(id).map { it?.toDomain() }
    }

    override suspend fun insertPin(pin: ScenicPin): Long = withContext(Dispatchers.IO) {
        // Calculate celestial values offline before inserting!
        val celestialResult = CelestialCalculator.calculate(pin.latitude, pin.longitude, pin.timestamp)
        
        // Fetch real-time weather at the time of save so it is accurate
        var temperature = pin.temperature
        var weatherStatus = pin.weatherStatus
        var cloudCoverage = pin.cloudCoverage
        var humidity = pin.humidity
        var windSpeed = pin.windSpeed
        var isWeatherSynced = pin.isWeatherSynced
        
        if (!isWeatherSynced) {
            try {
                val apiKey = com.example.BuildConfig.OPENWEATHER_API_KEY
                if (apiKey.isNotEmpty()) {
                    val response = weatherApi.getWeather(
                        lat = pin.latitude,
                        lon = pin.longitude,
                        apiKey = apiKey
                    )
                    temperature = response.main.temp
                    weatherStatus = response.weather.firstOrNull()?.main ?: "Clear"
                    cloudCoverage = response.clouds.all
                    humidity = response.main.humidity
                    windSpeed = response.wind?.speed
                    isWeatherSynced = true
                    Log.d("ScenicRepository", "Successfully fetched weather on save for coordinates ${pin.latitude}, ${pin.longitude}")
                } else {
                    Log.e("ScenicRepository", "Weather API key is empty.")
                }
            } catch (e: Exception) {
                Log.e("ScenicRepository", "Failed to fetch weather on save: ${e.localizedMessage}")
            }
        }

        val enrichedPin = pin.copy(
            sunAltitude = celestialResult.sunAltitude,
            sunAzimuth = celestialResult.sunAzimuth,
            moonAltitude = celestialResult.moonAltitude,
            moonAzimuth = celestialResult.moonAzimuth,
            moonPhase = celestialResult.moonPhase,
            goldenHourStart = celestialResult.goldenHourStart,
            goldenHourEnd = celestialResult.goldenHourEnd,
            twilightStart = celestialResult.twilightStart,
            twilightEnd = celestialResult.twilightEnd,
            temperature = temperature,
            weatherStatus = weatherStatus,
            cloudCoverage = cloudCoverage,
            humidity = humidity,
            windSpeed = windSpeed,
            isWeatherSynced = isWeatherSynced
        )
        val resultId = pinDao.insertPin(ScenicPinEntity.fromDomain(enrichedPin))
        triggerWidgetUpdate()
        resultId
    }

    override suspend fun insertPins(pins: List<ScenicPin>) = withContext(Dispatchers.IO) {
        val enrichedPins = pins.map { pin ->
            val celestialResult = CelestialCalculator.calculate(pin.latitude, pin.longitude, pin.timestamp)
            pin.copy(
                sunAltitude = celestialResult.sunAltitude,
                sunAzimuth = celestialResult.sunAzimuth,
                moonAltitude = celestialResult.moonAltitude,
                moonAzimuth = celestialResult.moonAzimuth,
                moonPhase = celestialResult.moonPhase,
                goldenHourStart = celestialResult.goldenHourStart,
                goldenHourEnd = celestialResult.goldenHourEnd,
                twilightStart = celestialResult.twilightStart,
                twilightEnd = celestialResult.twilightEnd
            )
        }
        pinDao.insertPins(enrichedPins.map { ScenicPinEntity.fromDomain(it) })
        triggerWidgetUpdate()
    }

    override suspend fun updatePin(pin: ScenicPin) = withContext(Dispatchers.IO) {
        // Recalculate celestial parameters if location or timestamp changed
        val celestialResult = CelestialCalculator.calculate(pin.latitude, pin.longitude, pin.timestamp)
        val updatedPin = pin.copy(
            sunAltitude = celestialResult.sunAltitude,
            sunAzimuth = celestialResult.sunAzimuth,
            moonAltitude = celestialResult.moonAltitude,
            moonAzimuth = celestialResult.moonAzimuth,
            moonPhase = celestialResult.moonPhase,
            goldenHourStart = celestialResult.goldenHourStart,
            goldenHourEnd = celestialResult.goldenHourEnd,
            twilightStart = celestialResult.twilightStart,
            twilightEnd = celestialResult.twilightEnd
        )
        pinDao.updatePin(ScenicPinEntity.fromDomain(updatedPin))
        triggerWidgetUpdate()
    }

    override suspend fun deletePin(pin: ScenicPin) = withContext(Dispatchers.IO) {
        pinDao.deletePin(ScenicPinEntity.fromDomain(pin))
        triggerWidgetUpdate()
    }

    override suspend fun syncWeatherForPin(pinId: Long) = withContext(Dispatchers.IO) {
        val localEntity = pinDao.getPinById(pinId).firstOrNull() ?: return@withContext
        
        try {
            val apiKey = com.example.BuildConfig.OPENWEATHER_API_KEY
            if (apiKey.isNotEmpty()) {
                val response = weatherApi.getWeather(
                    lat = localEntity.latitude,
                    lon = localEntity.longitude,
                    apiKey = apiKey
                )
                
                val updatedEntity = localEntity.copy(
                    temperature = response.main.temp,
                    weatherStatus = response.weather.firstOrNull()?.main ?: "Clear",
                    cloudCoverage = response.clouds.all,
                    humidity = response.main.humidity,
                    windSpeed = response.wind?.speed,
                    isWeatherSynced = true
                )
                pinDao.updatePin(updatedEntity)
                Log.d("ScenicRepository", "Successfully synced weather for pin $pinId")
                triggerWidgetUpdate()
            } else {
                Log.e("ScenicRepository", "Weather API key is empty.")
            }
        } catch (e: Exception) {
            Log.e("ScenicRepository", "Weather sync failed for pin $pinId: ${e.localizedMessage}")
        }
    }
}
