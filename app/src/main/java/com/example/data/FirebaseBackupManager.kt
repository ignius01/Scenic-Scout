package com.example.data

import android.util.Log
import com.example.domain.ScenicPin
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Self-contained Task await extension to prevent play-services coroutines dependency mismatches
suspend fun <T> Task<T>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: Exception("Firebase task failed"))
            }
        }
    }
}

class FirebaseBackupManager {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        try {
            _currentUserEmail.value = auth.currentUser?.email
            auth.addAuthStateListener { firebaseAuth ->
                _currentUserEmail.value = firebaseAuth.currentUser?.email
            }
        } catch (e: Exception) {
            Log.e("FirebaseBackupManager", "Firebase initialization error: ${e.localizedMessage}")
        }
    }

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    val userId: String?
        get() = auth.currentUser?.uid

    suspend fun signUp(email: String, password: String): Result<Unit> {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            _currentUserEmail.value = auth.currentUser?.email
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            _currentUserEmail.value = auth.currentUser?.email
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            _currentUserEmail.value = auth.currentUser?.email
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
        _currentUserEmail.value = null
    }

    suspend fun backupPins(pins: List<ScenicPin>): Result<Unit> {
        val uid = userId ?: return Result.failure(IllegalStateException("User not logged in"))
        _isSyncing.value = true
        return try {
            val pinsMap = pins.map { pin ->
                mapOf(
                    "name" to pin.name,
                    "latitude" to pin.latitude,
                    "longitude" to pin.longitude,
                    "timestamp" to pin.timestamp,
                    "landscapeType" to pin.landscapeType,
                    "timeOfDayCategory" to pin.timeOfDayCategory,
                    "filmStock" to pin.filmStock,
                    "iso" to pin.iso,
                    "aperture" to pin.aperture,
                    "notes" to pin.notes,
                    "temperature" to pin.temperature,
                    "weatherStatus" to pin.weatherStatus,
                    "cloudCoverage" to pin.cloudCoverage,
                    "isWeatherSynced" to pin.isWeatherSynced
                )
            }
            firestore.collection("users").document(uid).set(mapOf("pins" to pinsMap)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun restorePins(onRestore: suspend (ScenicPin) -> Unit): Result<Int> {
        val uid = userId ?: return Result.failure(IllegalStateException("User not logged in"))
        _isSyncing.value = true
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            var count = 0
            if (document.exists()) {
                val pinsList = document.get("pins") as? List<Map<String, Any?>> ?: emptyList()
                for (pinMap in pinsList) {
                    if (pinMap.isEmpty()) continue
                    val name = pinMap["name"] as? String ?: "Restored Pin"
                    val lat = (pinMap["latitude"] as? Number)?.toDouble() ?: 0.0
                    val lng = (pinMap["longitude"] as? Number)?.toDouble() ?: 0.0
                    val timestamp = (pinMap["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    val landscapeType = pinMap["landscapeType"] as? String ?: "Mountain"
                    val timeOfDayCategory = pinMap["timeOfDayCategory"] as? String ?: "Day"
                    val filmStock = pinMap["filmStock"] as? String ?: ""
                    val iso = (pinMap["iso"] as? Number)?.toInt() ?: 100
                    val aperture = pinMap["aperture"] as? String ?: "f/8"
                    val notes = pinMap["notes"] as? String ?: ""
                    val temperature = (pinMap["temperature"] as? Number)?.toDouble()
                    val weatherStatus = pinMap["weatherStatus"] as? String
                    val cloudCoverage = (pinMap["cloudCoverage"] as? Number)?.toInt()
                    val isWeatherSynced = pinMap["isWeatherSynced"] as? Boolean ?: false

                    val restoredPin = ScenicPin(
                        name = name,
                        latitude = lat,
                        longitude = lng,
                        timestamp = timestamp,
                        landscapeType = landscapeType,
                        timeOfDayCategory = timeOfDayCategory,
                        filmStock = filmStock,
                        iso = iso,
                        aperture = aperture,
                        notes = notes,
                        temperature = temperature,
                        weatherStatus = weatherStatus,
                        cloudCoverage = cloudCoverage,
                        isWeatherSynced = isWeatherSynced
                    )
                    onRestore(restoredPin)
                    count++
                }
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }
}
