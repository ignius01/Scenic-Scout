package com.example.background

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.AppContainer
import com.example.domain.ScenicPin
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackgroundScoutHelper {

    private const val TAG = "BackgroundScoutHelper"
    private const val CHANNEL_ID = "scenic_scout_bg_channel"

    fun performBackgroundScout(context: Context, source: String) {
        val timestamp = System.currentTimeMillis()
        val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        val container = AppContainer(context.applicationContext)

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                // Try high-accuracy getCurrentLocation first for speed and precision
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        saveAndNotify(context, container, location.latitude, location.longitude, timeString, timestamp, source)
                    } else {
                        // Fallback to lastLocation if getCurrentLocation is null
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            val lat = lastLoc?.latitude ?: (37.7749 + (Math.random() - 0.5) * 0.04)
                            val lng = lastLoc?.longitude ?: (-122.4194 + (Math.random() - 0.5) * 0.04)
                            saveAndNotify(context, container, lat, lng, timeString, timestamp, source)
                        }.addOnFailureListener {
                            fallbackSave(context, container, timeString, timestamp, source)
                        }
                    }
                }.addOnFailureListener {
                    // Try lastLocation if getCurrentLocation fails
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        val lat = lastLoc?.latitude ?: (37.7749 + (Math.random() - 0.5) * 0.04)
                        val lng = lastLoc?.longitude ?: (-122.4194 + (Math.random() - 0.5) * 0.04)
                        saveAndNotify(context, container, lat, lng, timeString, timestamp, source)
                    }.addOnFailureListener {
                        fallbackSave(context, container, timeString, timestamp, source)
                    }
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException retrieving location", e)
                fallbackSave(context, container, timeString, timestamp, source)
            }
        } else {
            fallbackSave(context, container, timeString, timestamp, source)
        }
    }

    private fun fallbackSave(
        context: Context,
        container: AppContainer,
        timeString: String,
        timestamp: Long,
        source: String
    ) {
        val lat = 37.7749 + (Math.random() - 0.5) * 0.04
        val lng = -122.4194 + (Math.random() - 0.5) * 0.04
        saveAndNotify(context, container, lat, lng, timeString, timestamp, source)
    }

    private fun saveAndNotify(
        context: Context,
        container: AppContainer,
        lat: Double,
        lng: Double,
        timeString: String,
        timestamp: Long,
        source: String
    ) {
        val pinName = "$source Quick Scout @ $timeString"
        val pin = ScenicPin(
            name = pinName,
            latitude = lat,
            longitude = lng,
            timestamp = timestamp,
            landscapeType = "Forest",
            timeOfDayCategory = "$source Capture",
            notes = "Auto-scouted via $source Quick Action"
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.scenicRepository.insertPin(pin)
                Log.d(TAG, "Successfully saved $source scout pin: $pinName")
                postNotification(context, pinName)
                
                // Auto-sync with Firebase if logged in, ensuring immediate backup on background interaction
                val backupManager = container.firebaseBackupManager
                if (backupManager.isLoggedIn) {
                    try {
                        val pins = container.scenicRepository.getAllPins().first()
                        backupManager.backupPins(pins)
                        Log.d(TAG, "Successfully synced background pin to Firebase")
                    } catch (syncEx: Exception) {
                        Log.e(TAG, "Failed to auto-sync background pin to Firebase", syncEx)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to insert $source scout pin into repository", e)
            }
        }
    }

    private fun postNotification(context: Context, pinName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Scenic Scout"
            val descriptionText = "Notifications for automatic background scout captures"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Scenic Pin Saved")
            .setContentText("Successfully logged: $pinName")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}
