package com.example.data

import android.content.Context
import com.example.domain.ScenicRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ScenicDatabase {
        return ScenicDatabase.getDatabase(context)
    }

    @Provides
    fun provideScenicPinDao(database: ScenicDatabase): ScenicPinDao {
        return database.scenicPinDao()
    }

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideWeatherApi(): WeatherApi {
        return Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }

    @Provides
    @Singleton
    fun provideFirebaseBackupManager(): FirebaseBackupManager {
        return FirebaseBackupManager()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideScenicRepository(
        pinDao: ScenicPinDao,
        weatherApi: WeatherApi,
        @ApplicationContext context: Context
    ): ScenicRepository {
        return ScenicRepositoryImpl(
            pinDao = pinDao,
            weatherApi = weatherApi,
            context = context
        )
    }
}
