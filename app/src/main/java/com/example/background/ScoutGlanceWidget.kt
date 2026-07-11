package com.example.background

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.DpSize
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.R
import com.example.data.AppContainer
import com.example.domain.CelestialCalculator
import com.example.domain.ScenicPin
import kotlinx.coroutines.flow.first

class ScoutGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(100.dp, 100.dp),  // Compact
            DpSize(220.dp, 110.dp),  // Medium
            DpSize(220.dp, 220.dp)   // Large/Tall
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = AppContainer(context.applicationContext)
        val pins = try {
            container.scenicRepository.getAllPins().first()
        } catch (e: Exception) {
            emptyList()
        }

        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                when {
                    size.width < 150.dp || size.height < 120.dp -> {
                        CompactWidgetLayout(context, pins.firstOrNull())
                    }
                    size.height < 180.dp -> {
                        MediumWidgetLayout(context, pins)
                    }
                    else -> {
                        LargeWidgetLayout(context, pins)
                    }
                }
            }
        }
    }
}

@Composable
fun CompactWidgetLayout(context: Context, lastPin: ScenicPin?) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // GPS chip
        Row(
            modifier = GlanceModifier
                .background(GlanceTheme.colors.surfaceVariant)
                .cornerRadius(12.dp)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(6.dp)
                    .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF82C49C)))
                    .cornerRadius(3.dp)
            ) {}
            Spacer(modifier = GlanceModifier.width(4.dp))
            Text(
                text = "GPS ACTIVE",
                style = TextStyle(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Center card
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
                .background(GlanceTheme.colors.primaryContainer)
                .cornerRadius(16.dp)
                .clickable(actionRunCallback<QuickScoutActionCallback>()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_scout_pin),
                contentDescription = "Scout Icon",
                modifier = GlanceModifier.size(24.dp),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "SCOUT",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun MediumWidgetLayout(context: Context, pins: List<ScenicPin>) {
    val lastPin = pins.firstOrNull()
    val temp = lastPin?.temperature ?: 68.0
    val wind = lastPin?.windSpeed ?: 12.0
    val sunsetTime = lastPin?.goldenHourEnd ?: "17:42"

    val timeDiffMs = System.currentTimeMillis() - (lastPin?.timestamp ?: 0L)
    val lastPinTimeText = when {
        lastPin == null -> "No pins yet"
        timeDiffMs < 60000L -> "Just now"
        timeDiffMs < 3600000L -> "${timeDiffMs / 60000L}m ago"
        timeDiffMs < 86400000L -> "${timeDiffMs / 3600000L}h ago"
        else -> "${timeDiffMs / 86400000L}d ago"
    }

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Celestial/Weather Dial Card
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .background(GlanceTheme.colors.surfaceVariant)
                .cornerRadius(16.dp)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val sunsetBitmap = createSunsetDialBitmap(context, sunsetTime)
            Image(
                provider = ImageProvider(sunsetBitmap),
                contentDescription = "Sunset Dial",
                modifier = GlanceModifier.size(72.dp)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_temp),
                    contentDescription = "Temp",
                    modifier = GlanceModifier.size(12.dp),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                )
                Spacer(modifier = GlanceModifier.width(2.dp))
                Text(
                    text = "${temp.toInt()}°F",
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurfaceVariant)
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Image(
                    provider = ImageProvider(R.drawable.ic_wind),
                    contentDescription = "Wind",
                    modifier = GlanceModifier.size(12.dp),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                )
                Spacer(modifier = GlanceModifier.width(2.dp))
                Text(
                    text = "${wind.toInt()}mph",
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurfaceVariant)
                )
            }
        }

        Spacer(modifier = GlanceModifier.width(12.dp))

        // Right: Quick Action & Status Card
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App brand & GPS status
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_compass),
                        contentDescription = "Compass",
                        modifier = GlanceModifier.size(14.dp),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "Scenic Scout",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onBackground)
                    )
                }
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "• GPS ACTIVE",
                    style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF82C49C)))
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Quick Scout Action Button
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(GlanceTheme.colors.primaryContainer)
                    .cornerRadius(12.dp)
                    .clickable(actionRunCallback<QuickScoutActionCallback>())
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_scout_pin),
                    contentDescription = "Scout Icon",
                    modifier = GlanceModifier.size(16.dp),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = "QUICK SCOUT",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onPrimaryContainer)
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Sync & Last Pin status footer
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SYNC STATUS",
                        style = TextStyle(fontSize = 8.sp, color = GlanceTheme.colors.onSurfaceVariant)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_cloud_check),
                            contentDescription = "Synced",
                            modifier = GlanceModifier.size(10.dp),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                        )
                        Spacer(modifier = GlanceModifier.width(2.dp))
                        Text(
                            text = "Ready",
                            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onBackground)
                        )
                    }
                }
                Spacer(modifier = GlanceModifier.defaultWeight())
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "LAST PIN",
                        style = TextStyle(fontSize = 8.sp, color = GlanceTheme.colors.onSurfaceVariant)
                    )
                    Text(
                        text = lastPinTimeText,
                        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onBackground)
                    )
                }
            }
        }
    }
}

@Composable
fun LargeWidgetLayout(context: Context, pins: List<ScenicPin>) {
    val lastPin = pins.firstOrNull()
    val lat = lastPin?.latitude ?: 37.7749
    val lng = lastPin?.longitude ?: -122.4194
    val celResult = CelestialCalculator.calculate(lat, lng, System.currentTimeMillis())

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(R.drawable.ic_compass),
                    contentDescription = "Compass",
                    modifier = GlanceModifier.size(16.dp),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = "Recent Scouts",
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onBackground)
                )
            }
            Spacer(modifier = GlanceModifier.defaultWeight())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(R.drawable.ic_cloud_check),
                    contentDescription = "Sync Badge",
                    modifier = GlanceModifier.size(14.dp),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = "CLOUD-READY",
                    style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.primary)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Split Content: Left (List) and Right (Celestial dial)
        Row(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Pane (List of pins or empty state) - fills remaining space
            Column(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pins.isEmpty()) {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(GlanceTheme.colors.surfaceVariant)
                            .cornerRadius(12.dp)
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_scout_pin),
                            contentDescription = "Empty",
                            modifier = GlanceModifier.size(24.dp),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant)
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = "No pins logged yet",
                            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurfaceVariant)
                        )
                        Text(
                            text = "Tap Quick Scout below",
                            style = TextStyle(fontSize = 8.sp, color = GlanceTheme.colors.onSurfaceVariant)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = GlanceModifier.fillMaxSize()
                    ) {
                        items(pins.take(2)) { pin ->
                            val latDir = if (pin.latitude >= 0) "N" else "S"
                            val lngDir = if (pin.longitude >= 0) "E" else "W"
                            val latVal = String.format(java.util.Locale.US, "%.4f", Math.abs(pin.latitude))
                            val lngVal = String.format(java.util.Locale.US, "%.4f", Math.abs(pin.longitude))
                            val coordsText = "$latVal°$latDir, $lngVal°$lngDir"

                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(GlanceTheme.colors.surfaceVariant)
                                    .cornerRadius(8.dp)
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = GlanceModifier
                                        .size(24.dp)
                                        .background(GlanceTheme.colors.primaryContainer)
                                        .cornerRadius(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        provider = ImageProvider(R.drawable.ic_scout_pin),
                                        contentDescription = "Pin",
                                        modifier = GlanceModifier.size(12.dp),
                                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer)
                                    )
                                }
                                Spacer(modifier = GlanceModifier.width(6.dp))
                                Column(modifier = GlanceModifier.defaultWeight()) {
                                    Text(
                                        text = if (pin.name.length > 24) pin.name.substring(0, 22) + "..." else pin.name,
                                        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurfaceVariant)
                                    )
                                    Text(
                                        text = coordsText,
                                        style = TextStyle(fontSize = 8.sp, color = GlanceTheme.colors.onSurfaceVariant)
                                    )
                                }
                                Spacer(modifier = GlanceModifier.width(4.dp))
                                Box(
                                    modifier = GlanceModifier
                                        .background(GlanceTheme.colors.secondaryContainer)
                                        .cornerRadius(6.dp)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = pin.timeOfDayCategory,
                                        style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSecondaryContainer)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            // Right Pane (Celestial Orbit Dial) - fixed-width to align dial beautifully
            Column(
                modifier = GlanceModifier.width(120.dp).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val orbitBitmap = createCelestialOrbitBitmap(
                    context,
                    label = "Golden Hour",
                    countdown = "Approaching",
                    alt = celResult.sunAltitude,
                    az = celResult.sunAzimuth
                )
                Image(
                    provider = ImageProvider(orbitBitmap),
                    contentDescription = "Celestial Dial",
                    modifier = GlanceModifier.size(110.dp)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Bottom Row: QUICK SCOUT PIN button
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.primaryContainer)
                .cornerRadius(12.dp)
                .clickable(actionRunCallback<QuickScoutActionCallback>())
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_scout_pin),
                contentDescription = "Scout Icon",
                modifier = GlanceModifier.size(16.dp),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer)
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = "QUICK SCOUT PIN",
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onPrimaryContainer)
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        // Coordinates status line at the very bottom
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.surfaceVariant)
                .cornerRadius(8.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val latStr = String.format(java.util.Locale.US, "%.4f", lat)
            val lngStr = String.format(java.util.Locale.US, "%.4f", lng)
            Text(
                text = "LAT $latStr | LNG $lngStr",
                style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurfaceVariant)
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Image(
                provider = ImageProvider(R.drawable.ic_compass),
                contentDescription = "Compass Bottom",
                modifier = GlanceModifier.size(10.dp),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
            )
        }
    }
}

private fun getPrimaryColor(context: Context): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            context.getColor(android.R.color.system_accent1_200)
        } catch (e: Exception) {
            0xFF82C49C.toInt() // Beautiful sage green fallback
        }
    } else {
        0xFF82C49C.toInt()
    }
}

private fun getOnSurfaceColor(context: Context): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            context.getColor(android.R.color.system_neutral1_100)
        } catch (e: Exception) {
            Color.WHITE
        }
    } else {
        Color.WHITE
    }
}

private fun createSunsetDialBitmap(context: Context, sunsetTime: String): Bitmap {
    val size = 300
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val primaryColor = getPrimaryColor(context)
    val onSurfaceColor = getOnSurfaceColor(context)

    val arcPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 10f
        color = 0x33FFFFFF
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }
    val rect = RectF(30f, 40f, size - 30f, size * 2 - 40f)
    canvas.drawArc(rect, 180f, 180f, false, arcPaint)

    val activePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 12f
        color = primaryColor
    }
    canvas.drawArc(rect, 180f, 130f, false, activePaint)

    val sunPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = primaryColor
    }
    canvas.drawCircle(size / 2f, size / 2f - 30f, 20f, sunPaint)

    val linePaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 6f
        color = primaryColor
    }
    canvas.drawLine(size / 2f - 30f, size / 2f - 10f, size / 2f + 30f, size / 2f - 10f, linePaint)
    canvas.drawLine(size / 2f, size / 2f - 70f, size / 2f, size / 2f - 56f, linePaint)
    canvas.drawLine(size / 2f - 24f, size / 2f - 54f, size / 2f - 14f, size / 2f - 44f, linePaint)
    canvas.drawLine(size / 2f + 24f, size / 2f - 54f, size / 2f + 14f, size / 2f - 44f, linePaint)

    val textPaint = Paint().apply {
        isAntiAlias = true
        color = onSurfaceColor
        textSize = 36f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    canvas.drawText(sunsetTime, size / 2f, size / 2f + 50f, textPaint)

    textPaint.apply {
        textSize = 20f
        color = 0xAAFFFFFF.toInt()
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    }
    canvas.drawText("SUNSET", size / 2f, size / 2f + 80f, textPaint)

    return bitmap
}

private fun createCelestialOrbitBitmap(context: Context, label: String, countdown: String, alt: Double, az: Double): Bitmap {
    val size = 360
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val primaryColor = getPrimaryColor(context)
    val onSurfaceColor = getOnSurfaceColor(context)

    val orbitPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = 0x44FFFFFF
        pathEffect = DashPathEffect(floatArrayOf(8f, 12f), 0f)
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 40f, orbitPaint)

    val angleRad = Math.toRadians(az - 90.0)
    val radius = size / 2f - 40f
    val sunX = size / 2f + radius * Math.cos(angleRad).toFloat()
    val sunY = size / 2f + radius * Math.sin(angleRad).toFloat()

    val sunPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = primaryColor
    }
    canvas.drawCircle(sunX, sunY, 16f, sunPaint)

    val rayPaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 4f
        color = primaryColor
    }
    canvas.drawLine(sunX, sunY - 28f, sunX, sunY - 20f, rayPaint)
    canvas.drawLine(sunX, sunY + 20f, sunX, sunY + 28f, rayPaint)
    canvas.drawLine(sunX - 28f, sunY, sunX - 20f, sunY, rayPaint)
    canvas.drawLine(sunX + 20f, sunY, sunX + 28f, sunY, rayPaint)

    val labelPaint = Paint().apply {
        isAntiAlias = true
        color = onSurfaceColor
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    canvas.drawText(label, size / 2f, size / 2f - 20f, labelPaint)

    val countdownBoxPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = 0x22FFFFFF
    }
    val boxRect = RectF(size / 2f - 100f, size / 2f + 4f, size / 2f + 100f, size / 2f + 48f)
    canvas.drawRoundRect(boxRect, 12f, 12f, countdownBoxPaint)

    val countPaint = Paint().apply {
        isAntiAlias = true
        color = primaryColor
        textSize = 24f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
    }
    canvas.drawText(countdown, size / 2f, size / 2f + 36f, countPaint)

    val subPaint = Paint().apply {
        isAntiAlias = true
        color = 0x99FFFFFF.toInt()
        textSize = 18f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("ALT: ${alt.toInt()}°", size / 4f + 10f, size - 24f, subPaint)
    canvas.drawText("AZ: ${az.toInt()}°", size * 3 / 4f - 10f, size - 24f, subPaint)

    return bitmap
}

class QuickScoutActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        BackgroundScoutHelper.performBackgroundScout(context, "Widget")
    }
}

class ScoutWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScoutGlanceWidget()
}

// ==========================================
// NEW WIDGETS MATCHING DESIGNS IN USER IMAGES
// ==========================================

class ScenicDialWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(100.dp, 100.dp),  // Compact 1x1 or 2x1
            DpSize(180.dp, 180.dp),  // Standard 2x2
            DpSize(240.dp, 240.dp)   // Large
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = AppContainer(context.applicationContext)
        val pins = try {
            container.scenicRepository.getAllPins().first()
        } catch (e: Exception) {
            emptyList()
        }
        val use24Hour = container.settingsManager.use24HourFormat.value
        val useFahrenheit = container.settingsManager.useFahrenheit.value

        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                ScenicDialWidgetLayout(context, pins.firstOrNull(), use24Hour, useFahrenheit, size)
            }
        }
    }
}

@Composable
fun ScenicDialWidgetLayout(
    context: Context, 
    lastPin: ScenicPin?, 
    use24Hour: Boolean, 
    useFahrenheit: Boolean,
    size: DpSize
) {
    val tempCelsius = lastPin?.temperature ?: 20.0
    val windMs = lastPin?.windSpeed ?: 5.0
    val windDir = "NW"
    val sunsetTime24 = lastPin?.goldenHourEnd?.ifEmpty { "17:42" } ?: "17:42"
    val sunsetTime = com.example.ui.formatTimeStr(sunsetTime24, use24Hour)

    val displayTemp = if (useFahrenheit) {
        "${((tempCelsius * 9 / 5) + 32).toInt()}°F"
    } else {
        "${tempCelsius.toInt()}°C"
    }

    val displayWind = if (useFahrenheit) {
        "${((windMs * 2.23694).toInt())} mph"
    } else {
        "${windMs.toInt()} m/s"
    }

    val isCompactHeight = size.height < 140.dp
    val isCompactWidth = size.width < 140.dp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(if (isCompactHeight) 6.dp else 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isCompactHeight) {
            // Wind & Temp Header Row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wind Card
                Row(
                    modifier = GlanceModifier
                        .background(GlanceTheme.colors.surfaceVariant)
                        .cornerRadius(8.dp)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_wind),
                        contentDescription = "Wind",
                        modifier = GlanceModifier.size(14.dp),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Column {
                        Text(
                            text = displayWind,
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                        Text(
                            text = "WIND $windDir",
                            style = TextStyle(
                                fontSize = 8.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Temp Card
                Row(
                    modifier = GlanceModifier
                        .background(GlanceTheme.colors.surfaceVariant)
                        .cornerRadius(8.dp)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = displayTemp,
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                        Text(
                            text = if (useFahrenheit) "TEMP F" else "TEMP C",
                            style = TextStyle(
                                fontSize = 8.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Image(
                        provider = ImageProvider(R.drawable.ic_temp),
                        contentDescription = "Temp",
                        modifier = GlanceModifier.size(14.dp),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())
        }

        // Center Dial (adjust size based on container dimensions)
        val dialSize = when {
            isCompactHeight && isCompactWidth -> 80.dp
            isCompactHeight -> 100.dp
            isCompactWidth -> 110.dp
            else -> 155.dp // Much larger sunset dial on standard sizes!
        }

        val dialBitmap = createScenicDialBitmap(context, sunsetTime)
        Image(
            provider = ImageProvider(dialBitmap),
            contentDescription = "Sunset Dial",
            modifier = GlanceModifier.size(dialSize)
        )

        if (!isCompactHeight) {
            Spacer(modifier = GlanceModifier.defaultWeight())

            // Bottom wide pill action button
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(GlanceTheme.colors.primary)
                    .cornerRadius(24.dp)
                    .clickable(actionRunCallback<QuickScoutActionCallback>())
                    .padding(vertical = if (size.height < 200.dp) 6.dp else 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(18.dp)
                        .background(GlanceTheme.colors.onPrimary)
                        .cornerRadius(9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = "SCOUT PIN",
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

class ScenicListWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(100.dp, 100.dp),  // Compact
            DpSize(200.dp, 180.dp),  // Medium
            DpSize(240.dp, 240.dp)   // Large
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = AppContainer(context.applicationContext)
        val pins = try {
            container.scenicRepository.getAllPins().first()
        } catch (e: Exception) {
            emptyList()
        }

        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                ScenicListWidgetLayout(context, pins, size)
            }
        }
    }
}

@Composable
fun ScenicListWidgetLayout(context: Context, pins: List<ScenicPin>, size: DpSize) {
    val isCompactHeight = size.height < 180.dp
    val isCompactWidth = size.width < 180.dp

    val displayPins = if (pins.isEmpty()) {
        listOf(
            ScenicPin(
                id = 101,
                name = "Alpine Ridge #4",
                latitude = 47.6062,
                longitude = -122.3321,
                timestamp = System.currentTimeMillis(),
                landscapeType = "Mountain",
                timeOfDayCategory = "Golden Hr",
                notes = ""
            ),
            ScenicPin(
                id = 102,
                name = "Canyon Overlook",
                latitude = 36.1070,
                longitude = -112.1130,
                timestamp = System.currentTimeMillis() - 3600000L,
                landscapeType = "Desert",
                timeOfDayCategory = "Overcast",
                notes = ""
            )
        )
    } else {
        pins.take(if (isCompactHeight) 1 else 2)
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(if (isCompactHeight) 8.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(R.drawable.ic_compass),
                    contentDescription = "Compass",
                    modifier = GlanceModifier.size(16.dp),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = "Recent Scouts",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onBackground
                    )
                )
            }
            Spacer(modifier = GlanceModifier.defaultWeight())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(R.drawable.ic_cloud_check),
                    contentDescription = "Cloud Badge",
                    modifier = GlanceModifier.size(16.dp),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Box(
                    modifier = GlanceModifier
                        .size(16.dp)
                        .background(GlanceTheme.colors.secondaryContainer)
                        .cornerRadius(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSecondaryContainer
                        )
                    )
                }
            }
        }

        Text(
            text = "SYNC: CLOUD-READY",
            style = TextStyle(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = GlanceTheme.colors.onSurfaceVariant
            ),
            modifier = GlanceModifier.padding(top = 2.dp, bottom = 8.dp)
        )

        // List of Pins
        Column(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            displayPins.forEach { pin ->
                val pLat = String.format(java.util.Locale.US, "%.4f", pin.latitude)
                val pLng = String.format(java.util.Locale.US, "%.4f", Math.abs(pin.longitude))
                val dirSymbol = if (pin.longitude >= 0) "E" else "W"
                val coordsText = "$pLat° N, $pLng° $dirSymbol"

                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(GlanceTheme.colors.surfaceVariant)
                        .cornerRadius(12.dp)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Topography Thumbnail
                    val mapBitmap = createTopographyMapBitmap(context, pin.id)
                    Image(
                        provider = ImageProvider(mapBitmap),
                        contentDescription = "Terrain map",
                        modifier = GlanceModifier.size(40.dp).cornerRadius(8.dp)
                    )

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    // Text Details
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = pin.name,
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                        Text(
                            text = coordsText,
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }

                    // Weather/Category on Right
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isOvercast = pin.timeOfDayCategory.equals("Overcast", ignoreCase = true)
                        val iconRes = if (isOvercast) R.drawable.ic_cloud_check else R.drawable.ic_sunset
                        Image(
                            provider = ImageProvider(iconRes),
                            contentDescription = pin.timeOfDayCategory,
                            modifier = GlanceModifier.size(16.dp),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = pin.timeOfDayCategory,
                            style = TextStyle(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.primary
                            )
                        )
                    }
                }
            }
        }

        if (!isCompactHeight) {
            Spacer(modifier = GlanceModifier.height(8.dp))

            // Large vertical stacked "QUICK SCOUT" button
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(GlanceTheme.colors.primary)
                    .cornerRadius(16.dp)
                    .clickable(actionRunCallback<QuickScoutActionCallback>())
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(20.dp)
                        .background(GlanceTheme.colors.onPrimary)
                        .cornerRadius(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = "QUICK SCOUT",
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // "READY" footer
            Text(
                text = "READY",
                style = TextStyle(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurfaceVariant
                ),
                modifier = GlanceModifier.padding(vertical = 2.dp)
            )
        }
    }
}

class ScenicDialWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScenicDialWidget()
}

class ScenicListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScenicListWidget()
}

private fun createScenicDialBitmap(context: Context, sunsetTime: String): Bitmap {
    val size = 400
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val primaryColor = getPrimaryColor(context)

    // Dashed background circle
    val bgPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 10f
        color = 0x22FFFFFF
        pathEffect = DashPathEffect(floatArrayOf(8f, 12f), 0f)
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 30f, bgPaint)

    // Solid active arc
    val activePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 12f
        color = primaryColor
        strokeCap = Paint.Cap.ROUND
    }
    val rect = RectF(30f, 30f, size - 30f, size - 30f)
    canvas.drawArc(rect, 125f, 290f, false, activePaint)

    // Half sun icon
    val sunPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = primaryColor
    }
    val sunCenterY = size / 2f - 40f
    canvas.drawLine(size / 2f - 32f, sunCenterY + 8f, size / 2f + 32f, sunCenterY + 8f, sunPaint)
    val sunRect = RectF(size / 2f - 20f, sunCenterY - 20f, size / 2f + 20f, sunCenterY + 20f)
    canvas.drawArc(sunRect, 180f, 180f, false, sunPaint)
    canvas.drawLine(size / 2f, sunCenterY - 20f, size / 2f, sunCenterY - 32f, sunPaint)
    canvas.drawLine(size / 2f - 20f, sunCenterY - 10f, size / 2f - 28f, sunCenterY - 18f, sunPaint)
    canvas.drawLine(size / 2f + 20f, sunCenterY - 10f, size / 2f + 28f, sunCenterY - 18f, sunPaint)

    // Time Text
    val timePaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textSize = 52f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    canvas.drawText(sunsetTime, size / 2f, size / 2f + 50f, timePaint)

    // Label Text
    val labelPaint = Paint().apply {
        isAntiAlias = true
        color = 0x88FFFFFF.toInt()
        textSize = 24f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    }
    canvas.drawText("SUNSET", size / 2f, size / 2f + 84f, labelPaint)

    return bitmap
}

private fun createTopographyMapBitmap(context: Context, seed: Long): Bitmap {
    val size = 240
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    val bgPaint = Paint().apply {
        color = 0xFF181C19.toInt()
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)
    
    val contourPaint = Paint().apply {
        color = 0x2282C49C
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    
    val random = java.util.Random(seed)
    val centerX = size / 2f + (random.nextFloat() - 0.5f) * 40f
    val centerY = size / 2f + (random.nextFloat() - 0.5f) * 40f
    
    for (r in 1..4) {
        val radius = r * 30f
        val path = android.graphics.Path()
        val numPoints = 12
        for (i in 0 until numPoints) {
            val angle = (i * 360f / numPoints) * (Math.PI / 180f)
            val wobble = radius + (random.nextFloat() - 0.5f) * 16f
            val x = centerX + Math.cos(angle).toFloat() * wobble
            val y = centerY + Math.sin(angle).toFloat() * wobble
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        canvas.drawPath(path, contourPaint)
    }
    
    val crossPaint = Paint().apply {
        color = 0xFF82C49C.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawCircle(centerX, centerY, 8f, crossPaint)
    
    return bitmap
}
