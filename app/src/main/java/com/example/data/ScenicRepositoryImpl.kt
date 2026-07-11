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

    private suspend fun triggerWidgetAndSyncUpdate() {
        triggerWidgetUpdate()
        com.example.background.FirebaseSyncManager.enqueueSync(context)
    }

    override fun getAllPins(): Flow<List<ScenicPin>> {
        return pinDao.getAllPins().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPinById(id: Long): Flow<ScenicPin?> {
        return pinDao.getPinById(id).map { it?.toDomain() }
    }

    override suspend fun fetchWeather(lat: Double, lon: Double): Result<WeatherResponse> = withContext(Dispatchers.IO) {
        val roundedLat = Math.round(lat * 100.0) / 100.0
        val roundedLon = Math.round(lon * 100.0) / 100.0
        val currentTime = System.currentTimeMillis()
        val cacheExpiryTime = 15 * 60 * 1000 // 15 minutes in milliseconds
        
        try {
            val cachedWeather = pinDao.getWeatherCache(roundedLat, roundedLon)
            if (cachedWeather != null && (currentTime - cachedWeather.timestamp) < cacheExpiryTime) {
                Log.d("ScenicRepository", "Weather Cache Hit for coordinates: $roundedLat, $roundedLon")
                val weatherResponse = WeatherResponse(
                    main = MainData(temp = cachedWeather.temperature, humidity = cachedWeather.humidity),
                    weather = listOf(WeatherData(description = cachedWeather.weatherStatus, main = cachedWeather.weatherStatus)),
                    clouds = CloudsData(all = cachedWeather.cloudCoverage),
                    wind = cachedWeather.windSpeed?.let { WindData(speed = it) }
                )
                return@withContext Result.success(weatherResponse)
            }
            
            // Periodically clean up old cache entries
            pinDao.deleteOldWeatherCache(currentTime - cacheExpiryTime)
            
            val apiKey = com.example.BuildConfig.OPENWEATHER_API_KEY
            if (apiKey.isEmpty()) {
                Result.failure(Exception("Weather API key is empty."))
            } else {
                val response = weatherApi.getWeather(
                    lat = lat,
                    lon = lon,
                    apiKey = apiKey
                )
                
                val cacheEntry = WeatherCacheEntity(
                    latitude = roundedLat,
                    longitude = roundedLon,
                    temperature = response.main.temp,
                    weatherStatus = response.weather.firstOrNull()?.main ?: "Clear",
                    cloudCoverage = response.clouds.all,
                    humidity = response.main.humidity,
                    windSpeed = response.wind?.speed,
                    timestamp = currentTime
                )
                pinDao.insertWeatherCache(cacheEntry)
                Log.d("ScenicRepository", "Weather Cache Miss. Fetched from API and cached.")
                Result.success(response)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun insertPin(pin: ScenicPin): Long = withContext(Dispatchers.IO) {
        var enrichedPin = pin.enrichWithCelestialData()
        
        if (!enrichedPin.isWeatherSynced) {
            fetchWeather(enrichedPin.latitude, enrichedPin.longitude)
                .onSuccess { response ->
                    enrichedPin = enrichedPin.enrichWithWeather(response)
                    Log.d("ScenicRepository", "Successfully fetched weather on save for coordinates ${enrichedPin.latitude}, ${enrichedPin.longitude}")
                }
                .onFailure { e ->
                    Log.e("ScenicRepository", "Failed to fetch weather on save: ${e.localizedMessage}")
                }
        }

        val resultId = pinDao.insertPin(ScenicPinEntity.fromDomain(enrichedPin))
        triggerWidgetAndSyncUpdate()
        resultId
    }

    override suspend fun insertPins(pins: List<ScenicPin>) = withContext(Dispatchers.IO) {
        val enrichedPins = pins.map { it.enrichWithCelestialData() }
        pinDao.insertPins(enrichedPins.map { ScenicPinEntity.fromDomain(it) })
        triggerWidgetAndSyncUpdate()
    }

    override suspend fun updatePin(pin: ScenicPin) = withContext(Dispatchers.IO) {
        val updatedPin = pin.enrichWithCelestialData()
        pinDao.updatePin(ScenicPinEntity.fromDomain(updatedPin))
        triggerWidgetAndSyncUpdate()
    }

    override suspend fun deletePin(pin: ScenicPin) = withContext(Dispatchers.IO) {
        pinDao.deletePin(ScenicPinEntity.fromDomain(pin))
        triggerWidgetAndSyncUpdate()
    }

    override suspend fun syncWeatherForPin(pinId: Long) = withContext(Dispatchers.IO) {
        val localEntity = pinDao.getPinById(pinId).firstOrNull() ?: return@withContext
        val pin = localEntity.toDomain()
        
        fetchWeather(pin.latitude, pin.longitude)
            .onSuccess { response ->
                val updatedPin = pin.enrichWithWeather(response)
                pinDao.updatePin(ScenicPinEntity.fromDomain(updatedPin))
                Log.d("ScenicRepository", "Successfully synced weather for pin $pinId")
                triggerWidgetAndSyncUpdate()
            }
            .onFailure { e ->
                Log.e("ScenicRepository", "Weather sync failed for pin $pinId: ${e.localizedMessage}")
            }
    }
}

