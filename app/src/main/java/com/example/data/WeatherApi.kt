package com.example.data

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("data/2.5/weather")
    suspend fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse
}

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    val main: MainData,
    val weather: List<WeatherData>,
    val clouds: CloudsData,
    val wind: WindData? = null
)

@JsonClass(generateAdapter = true)
data class MainData(
    val temp: Double,
    val humidity: Int? = null
)

@JsonClass(generateAdapter = true)
data class WeatherData(
    val description: String,
    val main: String
)

@JsonClass(generateAdapter = true)
data class CloudsData(
    val all: Int
)

@JsonClass(generateAdapter = true)
data class WindData(
    val speed: Double? = null
)

