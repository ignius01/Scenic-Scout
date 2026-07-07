package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SettingsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsManagerTest {

    @Test
    fun testSettingsDefaultValues() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settingsManager = SettingsManager(context)

        assertFalse(settingsManager.useFahrenheit.value)
        assertFalse(settingsManager.useDmsCoordinates.value)
        assertFalse(settingsManager.use24HourFormat.value)
        assertTrue(settingsManager.enableHaptic.value)
        assertEquals(0L, settingsManager.lastSyncTime.value)
        assertEquals(0L, settingsManager.lastLocalChangeTime.value)
    }

    @Test
    fun testUpdateSettingsValues() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settingsManager = SettingsManager(context)

        settingsManager.setUseFahrenheit(true)
        assertTrue(settingsManager.useFahrenheit.value)

        settingsManager.setUseDmsCoordinates(true)
        assertTrue(settingsManager.useDmsCoordinates.value)

        settingsManager.setUse24HourFormat(true)
        assertTrue(settingsManager.use24HourFormat.value)

        settingsManager.setEnableHaptic(false)
        assertFalse(settingsManager.enableHaptic.value)

        settingsManager.setLastSyncTime(123456L)
        assertEquals(123456L, settingsManager.lastSyncTime.value)

        settingsManager.setLastLocalChangeTime(789012L)
        assertEquals(789012L, settingsManager.lastLocalChangeTime.value)
    }
}
