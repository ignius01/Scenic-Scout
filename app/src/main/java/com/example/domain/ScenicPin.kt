package com.example.domain

import com.example.data.WeatherResponse

data class ScenicPin(
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    
    // Landscape & Customization
    val landscapeType: String, // Mountain, Beach, Forest, Desert, Urban, Water
    val timeOfDayCategory: String, // Sunrise, Day, GoldenHour, Sunset, BlueHour, Night
    val photoUri: String? = null,
    
    // Analog Field Notes
    val filmStock: String = "",
    val iso: Int = 100,
    val aperture: String = "f/8",
    val notes: String = "",

    // Weather Data (can be updated asynchronously)
    val temperature: Double? = null,
    val weatherStatus: String? = null,
    val cloudCoverage: Int? = null,
    val humidity: Int? = null,
    val windSpeed: Double? = null,
    val isWeatherSynced: Boolean = false,

    // Celestial Calculations (Computed offline or cached)
    val sunAltitude: Double = 0.0,
    val sunAzimuth: Double = 0.0,
    val moonAltitude: Double = 0.0,
    val moonAzimuth: Double = 0.0,
    val moonPhase: Double = 0.0, // 0.0 to 1.0 (New Moon to Full Moon)
    val goldenHourStart: String = "",
    val goldenHourEnd: String = "",
    val twilightStart: String = "",
    val twilightEnd: String = "",
    val shutterSpeed: String = "1/125s"
) {

    /**
     * Enriches this pin with calculated celestial positions and golden hour / twilight brackets.
     * 
     * Centralizing this computation ensures we do not repeat manual mapping logic in repository methods.
     */
    fun enrichWithCelestialData(): ScenicPin {
        val celestialResult = CelestialCalculator.calculate(latitude, longitude, timestamp)
        return this.copy(
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

    /**
     * Maps a Retrofit [WeatherResponse] to enrich this pin's weather metrics.
     * 
     * Unifies mapping coordinates to local domain properties across active saves and background updates.
     */
    fun enrichWithWeather(response: WeatherResponse): ScenicPin {
        return this.copy(
            temperature = response.main.temp,
            weatherStatus = response.weather.firstOrNull()?.main ?: "Clear",
            cloudCoverage = response.clouds.all,
            humidity = response.main.humidity,
            windSpeed = response.wind?.speed,
            isWeatherSynced = true
        )
    }

    /**
     * Serializes all properties of this pin into a Map for Firestore backups.
     * 
     * Centralized to ensure that no metadata (such as celestial bounds or field details) is silently lost.
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to timestamp,
            "landscapeType" to landscapeType,
            "timeOfDayCategory" to timeOfDayCategory,
            "photoUri" to photoUri,
            "filmStock" to filmStock,
            "iso" to iso,
            "aperture" to aperture,
            "notes" to notes,
            "temperature" to temperature,
            "weatherStatus" to weatherStatus,
            "cloudCoverage" to cloudCoverage,
            "humidity" to humidity,
            "windSpeed" to windSpeed,
            "isWeatherSynced" to isWeatherSynced,
            "sunAltitude" to sunAltitude,
            "sunAzimuth" to sunAzimuth,
            "moonAltitude" to moonAltitude,
            "moonAzimuth" to moonAzimuth,
            "moonPhase" to moonPhase,
            "goldenHourStart" to goldenHourStart,
            "goldenHourEnd" to goldenHourEnd,
            "twilightStart" to twilightStart,
            "twilightEnd" to twilightEnd,
            "shutterSpeed" to shutterSpeed
        )
    }

    companion object {
        /**
         * Reconstructs a [ScenicPin] from a Firestore document map.
         * 
         * Safely decodes properties, applying appropriate type-safety casts and fallback defaults.
         */
        fun fromMap(map: Map<String, Any?>): ScenicPin {
            val name = map["name"] as? String ?: "Restored Pin"
            val lat = (map["latitude"] as? Number)?.toDouble() ?: 0.0
            val lng = (map["longitude"] as? Number)?.toDouble() ?: 0.0
            val timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
            val landscapeType = map["landscapeType"] as? String ?: "Mountain"
            val timeOfDayCategory = map["timeOfDayCategory"] as? String ?: "Day"
            val photoUri = map["photoUri"] as? String
            val filmStock = map["filmStock"] as? String ?: ""
            val iso = (map["iso"] as? Number)?.toInt() ?: 100
            val aperture = map["aperture"] as? String ?: "f/8"
            val notes = map["notes"] as? String ?: ""
            val temperature = (map["temperature"] as? Number)?.toDouble()
            val weatherStatus = map["weatherStatus"] as? String
            val cloudCoverage = (map["cloudCoverage"] as? Number)?.toInt()
            val humidity = (map["humidity"] as? Number)?.toInt()
            val windSpeed = (map["windSpeed"] as? Number)?.toDouble()
            val isWeatherSynced = map["isWeatherSynced"] as? Boolean ?: false
            val sunAltitude = (map["sunAltitude"] as? Number)?.toDouble() ?: 0.0
            val sunAzimuth = (map["sunAzimuth"] as? Number)?.toDouble() ?: 0.0
            val moonAltitude = (map["moonAltitude"] as? Number)?.toDouble() ?: 0.0
            val moonAzimuth = (map["moonAzimuth"] as? Number)?.toDouble() ?: 0.0
            val moonPhase = (map["moonPhase"] as? Number)?.toDouble() ?: 0.0
            val goldenHourStart = map["goldenHourStart"] as? String ?: ""
            val goldenHourEnd = map["goldenHourEnd"] as? String ?: ""
            val twilightStart = map["twilightStart"] as? String ?: ""
            val twilightEnd = map["twilightEnd"] as? String ?: ""
            val shutterSpeed = map["shutterSpeed"] as? String ?: "1/125s"

            return ScenicPin(
                name = name,
                latitude = lat,
                longitude = lng,
                timestamp = timestamp,
                landscapeType = landscapeType,
                timeOfDayCategory = timeOfDayCategory,
                photoUri = photoUri,
                filmStock = filmStock,
                iso = iso,
                aperture = aperture,
                notes = notes,
                temperature = temperature,
                weatherStatus = weatherStatus,
                cloudCoverage = cloudCoverage,
                humidity = humidity,
                windSpeed = windSpeed,
                isWeatherSynced = isWeatherSynced,
                sunAltitude = sunAltitude,
                sunAzimuth = sunAzimuth,
                moonAltitude = moonAltitude,
                moonAzimuth = moonAzimuth,
                moonPhase = moonPhase,
                goldenHourStart = goldenHourStart,
                goldenHourEnd = goldenHourEnd,
                twilightStart = twilightStart,
                twilightEnd = twilightEnd,
                shutterSpeed = shutterSpeed
            )
        }
    }
}

