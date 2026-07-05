package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.domain.ScenicPin
import com.example.ui.CompactPinItem
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun compact_pin_item_screenshot() {
    val mockPin = ScenicPin(
      id = 1,
      name = "Yosemite Meadow View",
      latitude = 37.7425,
      longitude = -119.5375,
      timestamp = System.currentTimeMillis(),
      landscapeType = "Mountain",
      timeOfDayCategory = "GoldenHour",
      filmStock = "Portra 400",
      iso = 400,
      aperture = "f/5.6",
      notes = "Crisp autumn shadows stretching across the valley floor.",
      temperature = 18.5,
      weatherStatus = "Clear"
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        CompactPinItem(
          pin = mockPin,
          useFahrenheit = false,
          useDmsCoordinates = false,
          onClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/compact_pin.png")
  }
}
