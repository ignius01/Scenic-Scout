package com.example.background

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.background
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ScenicPin
import com.example.data.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScoutGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .background(androidx.glance.GlanceTheme.colors.background),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scenic Scout",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.glance.GlanceTheme.colors.onBackground
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = "Tap to Quick Scout",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = androidx.glance.GlanceTheme.colors.onBackground
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        Box(
                            modifier = GlanceModifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .background(androidx.glance.GlanceTheme.colors.primary)
                                .clickable(actionRunCallback<QuickScoutActionCallback>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "CAPTURE",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.glance.GlanceTheme.colors.onPrimary
                                ),
                                modifier = GlanceModifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

class QuickScoutActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val timestamp = System.currentTimeMillis()
        val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        val container = AppContainer(context.applicationContext)

        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    val lat = location?.latitude ?: 37.7749
                    val lng = location?.longitude ?: -122.4194
                    saveWidgetPin(container, lat, lng, timeString, timestamp)
                }.addOnFailureListener {
                    saveWidgetPin(container, 37.7749, -122.4194, timeString, timestamp)
                }
            } catch (e: SecurityException) {
                saveWidgetPin(container, 37.7749, -122.4194, timeString, timestamp)
            }
        } else {
            saveWidgetPin(container, 37.7749, -122.4194, timeString, timestamp)
        }
    }

    private fun saveWidgetPin(container: AppContainer, lat: Double, lng: Double, timeString: String, timestamp: Long) {
        val pin = ScenicPin(
            name = "Widget Quick Scout @ $timeString",
            latitude = lat,
            longitude = lng,
            timestamp = timestamp,
            landscapeType = "Forest",
            timeOfDayCategory = "Widget Capture",
            notes = "Auto-captured via Glance Widget"
        )
        // CoroutineScope to launch save on background thread
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            container.scenicRepository.insertPin(pin)
        }
    }
}

class ScoutWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScoutGlanceWidget()
}
