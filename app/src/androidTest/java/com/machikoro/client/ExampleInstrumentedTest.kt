package com.machikoro.client

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.machikoro.client.model.state.ConnectionStatus
import com.machikoro.client.model.state.StartScreenState
import com.machikoro.client.ui.start.StartScreen
import com.machikoro.client.ui.theme.ClientTheme
import org.junit.Test
import org.junit.Rule

class ExampleInstrumentedTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun startScreenRendersProvidedConnectionStatus() {
        composeTestRule.setContent {
            ClientTheme {
                StartScreen(
                    state = StartScreenState.placeholder().copy(
                        connectionStatus = ConnectionStatus.CONNECTED
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Machi Koro Client").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connection status: connected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lobby/start: placeholder").assertIsDisplayed()
    }
}
