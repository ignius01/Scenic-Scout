package com.example.domain

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
)
