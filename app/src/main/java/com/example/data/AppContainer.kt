package com.example.data

import android.content.Context
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.example.domain.ScenicRepository

class AppContainer(private val context: Context) {

    private val database: ScenicDatabase by lazy {
        ScenicDatabase.getDatabase(context)
    }

    val weatherApi: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }

    val settingsManager: SettingsManager by lazy {
        SettingsManager(context)
    }

    val firebaseBackupManager: FirebaseBackupManager by lazy {
        FirebaseBackupManager()
    }

    val scenicRepository: ScenicRepository by lazy {
        ScenicRepositoryImpl(
            pinDao = database.scenicPinDao(),
            weatherApi = weatherApi,
            context = context
        )
    }
}
