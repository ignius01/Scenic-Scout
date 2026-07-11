package com.example.domain

import kotlinx.coroutines.flow.Flow

interface ScenicRepository {
    fun getAllPins(): Flow<List<ScenicPin>>
    fun getPinById(id: Long): Flow<ScenicPin?>
    suspend fun insertPin(pin: ScenicPin): Long
    suspend fun insertPins(pins: List<ScenicPin>)
    suspend fun updatePin(pin: ScenicPin)
    suspend fun deletePin(pin: ScenicPin)
    suspend fun syncWeatherForPin(pinId: Long)
}
