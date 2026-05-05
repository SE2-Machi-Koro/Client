package com.machikoro.client.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.LoginDialogState
import com.machikoro.client.domain.model.state.LogoutState
import com.machikoro.client.domain.model.state.RegisterDialogState
import com.machikoro.client.domain.model.state.StartScreenState
import com.machikoro.client.domain.model.state.toDisplayText
import com.machikoro.client.ui.theme.ClientTheme
import org.junit.Rule
import org.junit.Test

private const val START_SCREEN_TITLE = "MACHI KORO"

class AppRootTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsStartScreenWhenGamePhaseIsNone() {
        composeTestRule.setContent {
            ClientTheme {
                AppRoot(
                    gameScreenState = GameScreenState.initial(),
                    startScreenState = StartScreenState.placeholder(),
                    registerDialogState = RegisterDialogState(),
                    loginDialogState = LoginDialogState(),
                    logoutState = LogoutState(),
                    onRegisterUsernameChange = {},
                    onRegisterPasswordChange = {},
                    onRegisterSubmit = {},
                    onRegisterDialogReset = {},
                    onLoginUsernameChange = {},
                    onLoginPasswordChange = {},
                    onLoginSubmit = {},
                    onLoginDialogReset = {},
                    onLogoutSubmit = {},
                    lobbyCode = null,
                    activeGameId = null,
                    isLobbyHost = false,
                    loggedInAs = null,
                    onCreateLobbyClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText(START_SCREEN_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(GamePhase.ROLL_DICE.toDisplayText()).assertDoesNotExist()
    }

    @Test
    fun showsGameScreenWhenGamePhaseIsNotNone() {
        composeTestRule.setContent {
            ClientTheme {
                AppRoot(
                    gameScreenState = GameScreenState.initial().copy(gamePhase = GamePhase.ROLL_DICE),
                    startScreenState = StartScreenState.placeholder(),
                    registerDialogState = RegisterDialogState(),
                    loginDialogState = LoginDialogState(),
                    logoutState = LogoutState(),
                    onRegisterUsernameChange = {},
                    onRegisterPasswordChange = {},
                    onRegisterSubmit = {},
                    onRegisterDialogReset = {},
                    onLoginUsernameChange = {},
                    onLoginPasswordChange = {},
                    onLoginSubmit = {},
                    onLoginDialogReset = {},
                    onLogoutSubmit = {},
                    lobbyCode = null,
                    activeGameId = null,
                    isLobbyHost = false,
                    loggedInAs = null,
                    onCreateLobbyClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText(GamePhase.ROLL_DICE.toDisplayText()).assertIsDisplayed()
        composeTestRule.onNodeWithText(START_SCREEN_TITLE).assertDoesNotExist()
    }

    @Test
    fun swapsScreenWhenGamePhaseTransitions() {
        var phase by mutableStateOf(GamePhase.NONE)
        composeTestRule.setContent {
            ClientTheme {
                AppRoot(
                    gameScreenState = GameScreenState.initial().copy(gamePhase = phase),
                    startScreenState = StartScreenState.placeholder(),
                    registerDialogState = RegisterDialogState(),
                    loginDialogState = LoginDialogState(),
                    logoutState = LogoutState(),
                    onRegisterUsernameChange = {},
                    onRegisterPasswordChange = {},
                    onRegisterSubmit = {},
                    onRegisterDialogReset = {},
                    onLoginUsernameChange = {},
                    onLoginPasswordChange = {},
                    onLoginSubmit = {},
                    onLoginDialogReset = {},
                    onLogoutSubmit = {},
                    lobbyCode = null,
                    activeGameId = null,
                    isLobbyHost = false,
                    loggedInAs = null,
                    onCreateLobbyClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText(START_SCREEN_TITLE).assertIsDisplayed()

        phase = GamePhase.BUY_OR_BUILD
        composeTestRule.onNodeWithText(GamePhase.BUY_OR_BUILD.toDisplayText()).assertIsDisplayed()
        composeTestRule.onNodeWithText(START_SCREEN_TITLE).assertDoesNotExist()

        phase = GamePhase.NONE
        composeTestRule.onNodeWithText(START_SCREEN_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(GamePhase.BUY_OR_BUILD.toDisplayText()).assertDoesNotExist()
    }

    @Test
    fun showsHomeScreenWithStartGameWhenLoggedInHostHasLobby() {
        composeTestRule.setContent {
            ClientTheme {
                AppRoot(
                    gameScreenState = GameScreenState.initial(),
                    startScreenState = StartScreenState.placeholder().copy(
                        loggedInAs = "alice",
                        connectionStatus = com.machikoro.client.domain.model.state.ConnectionStatus.CONNECTED,
                    ),
                    registerDialogState = RegisterDialogState(),
                    loginDialogState = LoginDialogState(),
                    logoutState = LogoutState(),
                    onRegisterUsernameChange = {},
                    onRegisterPasswordChange = {},
                    onRegisterSubmit = {},
                    onRegisterDialogReset = {},
                    onLoginUsernameChange = {},
                    onLoginPasswordChange = {},
                    onLoginSubmit = {},
                    onLoginDialogReset = {},
                    onLogoutSubmit = {},
                    lobbyCode = "AJ25Z39",
                    activeGameId = 7,
                    isLobbyHost = true,
                    loggedInAs = "alice",
                    onCreateLobbyClick = {},
                    onStartGame = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Lobby erstellen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start Game").assertIsDisplayed()
    }
}
