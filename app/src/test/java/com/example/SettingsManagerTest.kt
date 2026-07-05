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
        assertEquals("Portra 400", settingsManager.defaultFilmStock.value)
        assertEquals(400, settingsManager.defaultIso.value)
        assertEquals("f/8", settingsManager.defaultAperture.value)
        assertTrue(settingsManager.enableHaptic.value)
    }

    @Test
    fun testUpdateSettingsValues() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settingsManager = SettingsManager(context)

        settingsManager.setUseFahrenheit(true)
        assertTrue(settingsManager.useFahrenheit.value)

        settingsManager.setUseDmsCoordinates(true)
        assertTrue(settingsManager.useDmsCoordinates.value)

        settingsManager.setDefaultFilmStock("Ektar 100")
        assertEquals("Ektar 100", settingsManager.defaultFilmStock.value)

        settingsManager.setDefaultIso(100)
        assertEquals(100, settingsManager.defaultIso.value)

        settingsManager.setDefaultAperture("f/2.8")
        assertEquals("f/2.8", settingsManager.defaultAperture.value)

        settingsManager.setEnableHaptic(false)
        assertFalse(settingsManager.enableHaptic.value)
    }
}
