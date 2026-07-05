package com.example.domain

import kotlinx.coroutines.flow.Flow

interface ScenicRepository {
    fun getAllPins(): Flow<List<ScenicPin>>
    fun getPinById(id: Long): Flow<ScenicPin?>
    suspend fun insertPin(pin: ScenicPin): Long
    suspend fun updatePin(pin: ScenicPin)
    suspend fun deletePin(pin: ScenicPin)
    suspend fun syncWeatherForPin(pinId: Long)
}
