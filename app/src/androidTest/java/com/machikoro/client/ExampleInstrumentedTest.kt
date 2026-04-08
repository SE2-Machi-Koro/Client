package com.machikoro.client

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun minimalStartScreenIsShown() {
        composeTestRule.onNodeWithText("Machi Koro Client").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connection status: waiting for WebSocket integration").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lobby/start: placeholder").assertIsDisplayed()
    }
}
