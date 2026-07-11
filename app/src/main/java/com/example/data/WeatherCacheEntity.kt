package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity representing cached weather metrics at specific coordinates.
 * 
 * Rounding latitude/longitude allows caching weather bounds within ~1.1km areas to save API requests.
 */
@Entity(
    tableName = "weather_cache",
    indices = [androidx.room.Index(value = ["latitude", "longitude"])]
)
data class WeatherCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val temperature: Double,
    val weatherStatus: String,
    val cloudCoverage: Int,
    val humidity: Int?,
    val windSpeed: Double?,
    val timestamp: Long
)
