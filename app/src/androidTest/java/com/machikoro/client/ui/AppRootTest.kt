package com.machikoro.client.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.LobbyScreenState
import com.machikoro.client.domain.model.state.LoginDialogState
import com.machikoro.client.domain.model.state.LogoutState
import com.machikoro.client.domain.model.state.RegisterDialogState
import com.machikoro.client.domain.model.state.StartScreenState
import com.machikoro.client.domain.model.state.toDisplayText
import com.machikoro.client.ui.theme.ClientTheme
import org.junit.Rule
import org.junit.Test

private const val START_SCREEN_TITLE = "MACHI KORO"
// HomeScreen-specific labels — distinguish HomeScreen from StartScreen, which
// shares the "MACHI KORO" title.
private const val HOME_SCREEN_LOBBY_CARD = "Lobby beitreten"
private const val HOME_SCREEN_LOGOUT = "Abmelden"
private const val START_SCREEN_LOGIN = "Login"
private const val START_SCREEN_REGISTER = "Register"
// LobbyScreen-specific label — only rendered on LobbyScreen (it's the
// "Spielerliste" heading above the player list), so it cleanly distinguishes
// LobbyScreen from HomeScreen for routing assertions.
private const val LOBBY_SCREEN_PLAYER_LIST = "Spielerliste"

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
                    lobbyScreenState = LobbyScreenState.placeholder(),
                    lobbyCode = null,
                    isLobbyHost = false,
                    loggedInAs = null,
                    onCreateLobbyClick = {},
                    onGoToLobbyClick = {},
                    showLobbyScreen = false,
                    onReadyToggle = {},
                    onStartGame = {},
                    onLeaveLobby = {},
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
                    lobbyScreenState = LobbyScreenState.placeholder(),
                    lobbyCode = null,
                    isLobbyHost = false,
                    loggedInAs = null,
                    onCreateLobbyClick = {},
                    onGoToLobbyClick = {},
                    showLobbyScreen = false,
                    onReadyToggle = {},
                    onStartGame = {},
                    onLeaveLobby = {},
                )
            }
        }

        composeTestRule.onNodeWithText(GamePhase.ROLL_DICE.toDisplayText()).assertIsDisplayed()
        composeTestRule.onNodeWithText(START_SCREEN_TITLE).assertDoesNotExist()
    }

    @Test
    fun showsStartScreenWhenUnauthenticated() {
        setAppRoot(loggedInAs = null)

        composeTestRule.onNodeWithText(START_SCREEN_REGISTER).assertIsDisplayed()
        composeTestRule.onNodeWithText(START_SCREEN_LOGIN).assertIsDisplayed()
        composeTestRule.onNodeWithText(HOME_SCREEN_LOGOUT).assertDoesNotExist()
        composeTestRule.onNodeWithText(HOME_SCREEN_LOBBY_CARD).assertDoesNotExist()
    }

    @Test
    fun showsHomeScreenWhenAuthenticated() {
        setAppRoot(loggedInAs = "alice")

        composeTestRule.onNodeWithText(HOME_SCREEN_LOBBY_CARD).assertIsDisplayed()
        composeTestRule.onNodeWithText(HOME_SCREEN_LOGOUT).assertIsDisplayed()
        composeTestRule.onNodeWithText(START_SCREEN_REGISTER).assertDoesNotExist()
        composeTestRule.onNodeWithText(START_SCREEN_LOGIN).assertDoesNotExist()
    }

    @Test
    fun routesAuthenticatedUserToLobbyWhenShowLobbyScreenFlipsTrue() {
        // Pins the contract for #51: while authenticated, the user stays on
        // HomeScreen until the confirm/check icon flips `showLobbyScreen` to
        // true — only then do they navigate into LobbyScreen. The reverse
        // direction is also exercised so a future regression that pins the
        // routing one-way (e.g. via an absorbing state) is caught.
        var showLobbyScreen by mutableStateOf(false)
        composeTestRule.setContent {
            ClientTheme {
                AppRoot(
                    gameScreenState = GameScreenState.initial(),
                    startScreenState = StartScreenState.placeholder().copy(loggedInAs = "alice"),
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
                    lobbyScreenState = LobbyScreenState.placeholder(),
                    lobbyCode = "ABC1234",
                    loggedInAs = "alice",
                    onCreateLobbyClick = {},
                    onGoToLobbyClick = {},
                    showLobbyScreen = showLobbyScreen,
                    onReadyToggle = {},
                    onStartGame = {},
                    onLeaveLobby = {},
                )
            }
        }

        composeTestRule.onNodeWithText(HOME_SCREEN_LOBBY_CARD).assertIsDisplayed()
        composeTestRule.onNodeWithText(LOBBY_SCREEN_PLAYER_LIST).assertDoesNotExist()

        showLobbyScreen = true
        composeTestRule.onNodeWithText(LOBBY_SCREEN_PLAYER_LIST).assertIsDisplayed()
        composeTestRule.onNodeWithText(HOME_SCREEN_LOBBY_CARD).assertDoesNotExist()

        showLobbyScreen = false
        composeTestRule.onNodeWithText(HOME_SCREEN_LOBBY_CARD).assertIsDisplayed()
        composeTestRule.onNodeWithText(LOBBY_SCREEN_PLAYER_LIST).assertDoesNotExist()
    }

    private fun setAppRoot(loggedInAs: String?) {
        composeTestRule.setContent {
            ClientTheme {
                AppRoot(
                    gameScreenState = GameScreenState.initial(),
                    startScreenState = StartScreenState.placeholder().copy(loggedInAs = loggedInAs),
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
                    lobbyScreenState = LobbyScreenState.placeholder(),
                    lobbyCode = null,
                    loggedInAs = loggedInAs,
                    onCreateLobbyClick = {},
                    onGoToLobbyClick = {},
                    showLobbyScreen = false,
                    onReadyToggle = {},
                    onStartGame = {},
                    onLeaveLobby = {},
                )
            }
        }
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
                    lobbyScreenState = LobbyScreenState.placeholder(),
                    lobbyCode = null,
                    isLobbyHost = false,
                    loggedInAs = null,
                    onCreateLobbyClick = {},
                    onGoToLobbyClick = {},
                    showLobbyScreen = false,
                    onReadyToggle = {},
                    onStartGame = {},
                    onLeaveLobby = {},
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
    fun showsHomeScreenWithStartGameWhenLoggedInHostHasActiveGame() {
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
                    lobbyScreenState = LobbyScreenState.placeholder(),
                    lobbyCode = null,
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
