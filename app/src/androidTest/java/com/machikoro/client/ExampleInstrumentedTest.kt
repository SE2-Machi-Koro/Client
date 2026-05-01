package com.machikoro.client

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.RegisterDialogState
import com.machikoro.client.domain.model.state.StartScreenState
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
                    ),
                    registerDialogState = RegisterDialogState(),
                    onRegisterUsernameChange = {},
                    onRegisterPasswordChange = {},
                    onRegisterSubmit = {},
                    onRegisterDialogReset = {},
                )
            }
        }

        composeTestRule.onNodeWithText("MACHI KORO").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connection status: connected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lobby/start: placeholder").assertIsDisplayed()
    }
}
