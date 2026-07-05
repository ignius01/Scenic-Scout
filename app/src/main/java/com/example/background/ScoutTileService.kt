package com.example.background

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.service.quicksettings.TileService
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.example.data.AppContainer
import com.example.domain.ScenicPin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScoutTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onClick() {
        super.onClick()
        Log.d("ScoutTileService", "Quick Settings Tile tapped")

        val context = applicationContext
        val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                          ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val timestamp = System.currentTimeMillis()
        val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        val defaultName = "Tile Quick Scout @ $timeString"

        if (hasLocation) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    val lat = location?.latitude ?: 37.7749
                    val lng = location?.longitude ?: -122.4194
                    savePinAndTriggerFeedback(lat, lng, defaultName, timestamp)
                }.addOnFailureListener {
                    savePinAndTriggerFeedback(37.7749, -122.4194, defaultName, timestamp)
                }
            } catch (e: SecurityException) {
                savePinAndTriggerFeedback(37.7749, -122.4194, defaultName, timestamp)
            }
        } else {
            savePinAndTriggerFeedback(37.7749, -122.4194, defaultName, timestamp)
        }
    }

    private fun savePinAndTriggerFeedback(lat: Double, lng: Double, name: String, timestamp: Long) {
        serviceScope.launch {
            val container = AppContainer(applicationContext)
            
            val pin = ScenicPin(
                name = name,
                latitude = lat,
                longitude = lng,
                timestamp = timestamp,
                landscapeType = "Mountain",
                timeOfDayCategory = "Tile Capture",
                notes = "Auto-captured via Quick Settings Tile"
            )
            
            val newId = container.scenicRepository.insertPin(pin)
            
            triggerHapticFeedback()
            Log.d("ScoutTileService", "Saved pin $newId from Quick Settings Tile")
        }
    }

    private fun triggerHapticFeedback() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(150)
        }
    }
}
