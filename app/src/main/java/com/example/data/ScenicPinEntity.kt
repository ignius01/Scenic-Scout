package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.ScenicPin

@Entity(
    tableName = "scenic_pins",
    indices = [androidx.room.Index(value = ["timestamp"])]
)
data class ScenicPinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    
    // Landscape & Customization
    val landscapeType: String,
    val timeOfDayCategory: String,
    val photoUri: String?,
    
    // Analog Field Notes
    val filmStock: String,
    val iso: Int,
    val aperture: String,
    val notes: String,

    // Weather Data
    val temperature: Double?,
    val weatherStatus: String?,
    val cloudCoverage: Int?,
    val humidity: Int?,
    val windSpeed: Double?,
    val isWeatherSynced: Boolean,

    // Celestial Calculations
    val sunAltitude: Double,
    val sunAzimuth: Double,
    val moonAltitude: Double,
    val moonAzimuth: Double,
    val moonPhase: Double,
    val goldenHourStart: String,
    val goldenHourEnd: String,
    val twilightStart: String,
    val twilightEnd: String,
    val shutterSpeed: String = "1/125s"
) {
    fun toDomain(): ScenicPin = ScenicPin(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
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

    companion object {
        fun fromDomain(pin: ScenicPin): ScenicPinEntity = ScenicPinEntity(
            id = pin.id,
            name = pin.name,
            latitude = pin.latitude,
            longitude = pin.longitude,
            timestamp = pin.timestamp,
            landscapeType = pin.landscapeType,
            timeOfDayCategory = pin.timeOfDayCategory,
            photoUri = pin.photoUri,
            filmStock = pin.filmStock,
            iso = pin.iso,
            aperture = pin.aperture,
            notes = pin.notes,
            temperature = pin.temperature,
            weatherStatus = pin.weatherStatus,
            cloudCoverage = pin.cloudCoverage,
            humidity = pin.humidity,
            windSpeed = pin.windSpeed,
            isWeatherSynced = pin.isWeatherSynced,
            sunAltitude = pin.sunAltitude,
            sunAzimuth = pin.sunAzimuth,
            moonAltitude = pin.moonAltitude,
            moonAzimuth = pin.moonAzimuth,
            moonPhase = pin.moonPhase,
            goldenHourStart = pin.goldenHourStart,
            goldenHourEnd = pin.goldenHourEnd,
            twilightStart = pin.twilightStart,
            twilightEnd = pin.twilightEnd,
            shutterSpeed = pin.shutterSpeed
        )
    }
}
