package com.example.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

object CelestialCalculator {

    data class CelestialResult(
        val sunAltitude: Double,
        val sunAzimuth: Double,
        val moonAltitude: Double,
        val moonAzimuth: Double,
        val moonPhase: Double, // 0.0 to 1.0 (0.0=New Moon, 0.25=First Quarter, 0.5=Full Moon, 0.75=Third Quarter)
        val goldenHourStart: String,
        val goldenHourEnd: String,
        val twilightStart: String,
        val twilightEnd: String
    )

    fun calculate(lat: Double, lng: Double, timestamp: Long): CelestialResult {
        val date = Date(timestamp)
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            time = date
        }

        // 1. Sun Calculations
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val hour = calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60.0
        
        // Approximate Sun Declination
        val declination = 23.45 * sin(Math.toRadians(360.0 / 365.0 * (284 + dayOfYear)))
        val declRad = Math.toRadians(declination)
        val latRad = Math.toRadians(lat)
        
        // Hour angle (approximate)
        val localSolarTime = hour + (lng / 15.0) // 15 degrees per hour
        val hourAngle = 15.0 * (localSolarTime - 12.0)
        val haRad = Math.toRadians(hourAngle)
        
        // Altitude
        val sinAltitude = sin(latRad) * sin(declRad) + cos(latRad) * cos(declRad) * cos(haRad)
        val sunAltitudeRad = asin(sinAltitude.coerceIn(-1.0, 1.0))
        val sunAltitudeDeg = Math.toDegrees(sunAltitudeRad)
        
        // Azimuth
        val cosAzimuth = (sin(declRad) - sin(latRad) * sinAltitude) / (cos(latRad) * cos(sunAltitudeRad))
        var sunAzimuthDeg = Math.toDegrees(acos(cosAzimuth.coerceIn(-1.0, 1.0)))
        if (haRad > 0) {
            sunAzimuthDeg = 360.0 - sunAzimuthDeg
        }

        // 2. Moon Phase & Position (Approximate)
        // Reference New Moon: Jan 6, 2000 (Julian Date approx 2451549.5)
        val msPerDay = 24.0 * 60.0 * 60.0 * 1000.0
        val baseDateMs = 947116800000L // Jan 6, 2000 in UTC
        val daysSinceBase = (timestamp - baseDateMs) / msPerDay
        val synodicMonth = 29.530588853
        val ageInDays = (daysSinceBase % synodicMonth + synodicMonth) % synodicMonth
        val moonPhase = ageInDays / synodicMonth // 0.0 to 1.0

        // Approximate Moon coordinates (relative offset from Sun)
        val moonAgeRad = Math.toRadians(moonPhase * 360.0)
        val moonAltitudeDeg = sunAltitudeDeg + 8.0 * sin(moonAgeRad)
        val moonAzimuthDeg = (sunAzimuthDeg + 180.0 + 15.0 * sin(moonAgeRad)) % 360.0

        // 3. Golden Hour and Twilight time brackets formatting
        // Standard sunrise/sunset offset
        val localCalendar = Calendar.getInstance().apply {
            time = date
        }
        val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        // Let's calculate a robust approximate time of day for these brackets
        // Golden hour usually occurs around sunrise (sun is -4 to 6 deg) and sunset.
        // Let's create beautiful formatted time bounds relative to local solar time:
        val noonMs = timestamp - (calendar.get(Calendar.HOUR_OF_DAY) * 3600 + calendar.get(Calendar.MINUTE) * 60 + calendar.get(Calendar.SECOND)) * 1000L + (12 * 3600 - lng * 240).toLong() * 1000L
        
        // Approximate sunset/sunrise local solar times
        // cos(ha) = -tan(lat)*tan(decl)
        val cosH = -tan(latRad) * tan(declRad)
        val hDeg = if (cosH in -1.0..1.0) Math.toDegrees(acos(cosH)) else 90.0
        val halfDayMs = (hDeg / 15.0 * 3600.0 * 1000.0).toLong()
        
        val sunriseTime = Date(noonMs - halfDayMs)
        val sunsetTime = Date(noonMs + halfDayMs)
        
        // Brackets
        val sunriseStr = hourFormat.format(sunriseTime)
        val sunsetStr = hourFormat.format(sunsetTime)
        
        val goldenHourStartCal = Calendar.getInstance().apply {
            time = sunsetTime
            add(Calendar.MINUTE, -40)
        }
        val goldenHourEndCal = Calendar.getInstance().apply {
            time = sunsetTime
            add(Calendar.MINUTE, 20)
        }
        
        val twilightStartCal = Calendar.getInstance().apply {
            time = sunriseTime
            add(Calendar.MINUTE, -45)
        }
        val twilightEndCal = Calendar.getInstance().apply {
            time = sunriseTime
            add(Calendar.MINUTE, -15)
        }

        val goldenHourStart = hourFormat.format(goldenHourStartCal.time)
        val goldenHourEnd = hourFormat.format(goldenHourEndCal.time)
        val twilightStart = hourFormat.format(twilightStartCal.time)
        val twilightEnd = hourFormat.format(twilightEndCal.time)

        return CelestialResult(
            sunAltitude = sunAltitudeDeg,
            sunAzimuth = sunAzimuthDeg,
            moonAltitude = moonAltitudeDeg,
            moonAzimuth = moonAzimuthDeg,
            moonPhase = moonPhase,
            goldenHourStart = "$twilightEnd to $goldenHourEnd", // Golden Hour bracket (morning and evening)
            goldenHourEnd = goldenHourEnd,
            twilightStart = twilightStart,
            twilightEnd = "$twilightStart to $sunriseStr"
        )
    }
}
