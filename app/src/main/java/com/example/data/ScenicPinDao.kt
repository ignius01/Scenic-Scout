package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScenicPinDao {
    @Query("SELECT * FROM scenic_pins ORDER BY timestamp DESC")
    fun getAllPins(): Flow<List<ScenicPinEntity>>

    @Query("SELECT * FROM scenic_pins WHERE id = :id LIMIT 1")
    fun getPinById(id: Long): Flow<ScenicPinEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPin(pin: ScenicPinEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPins(pins: List<ScenicPinEntity>)

    @Update
    suspend fun updatePin(pin: ScenicPinEntity)

    @Delete
    suspend fun deletePin(pin: ScenicPinEntity)

    @Query("SELECT * FROM weather_cache WHERE latitude = :lat AND longitude = :lon LIMIT 1")
    suspend fun getWeatherCache(lat: Double, lon: Double): WeatherCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherCache(cache: WeatherCacheEntity)

    @Query("DELETE FROM weather_cache WHERE timestamp < :threshold")
    suspend fun deleteOldWeatherCache(threshold: Long)
}
