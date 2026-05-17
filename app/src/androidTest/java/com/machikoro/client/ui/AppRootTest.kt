package com.machikoro.client.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.LobbyScreenState
import com.machikoro.client.domain.model.state.LoginDialogState
import com.machikoro.client.domain.model.state.LogoutState
import com.machikoro.client.domain.model.state.RegisterDialogState
import com.machikoro.client.domain.model.state.StartScreenState
import com.machikoro.client.domain.model.state.toDisplayText
import com.machikoro.client.ui.navigation.NavigationViewModel
import com.machikoro.client.ui.theme.ClientTheme
import org.junit.Rule
import org.junit.Test

private const val START_SCREEN_TITLE = "MACHI KORO"
// HomeScreen-specific labels — distinguish HomeScreen from StartScreen, which
// shares the "MACHI KORO" title.
private const val HOME_SCREEN_LOBBY_CARD = "Join Lobby"
private const val HOME_SCREEN_LOGOUT = "Logout"
private const val START_SCREEN_LOGIN = "Login"
private const val START_SCREEN_REGISTER = "Register"
// LobbyScreen-specific label — only rendered on LobbyScreen (it's the
// "Spielerliste" heading above the player list), so it cleanly distinguishes
// LobbyScreen from HomeScreen for routing assertions.
private const val LOBBY_SCREEN_PLAYER_LIST = "Players"

class AppRootTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsStartScreenWhenGamePhaseIsNone() {
        val navigationViewModel = NavigationViewModel()

        composeTestRule.setContent {
            ClientTheme {
                AppRoot(
                    navigationViewModel = navigationViewModel,
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
                    onCreateLobbyClick = {},
                    onGoToLobbyClick = {},
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
        val navigationViewModel = NavigationViewModel()

        composeTestRule.setContent {
            ClientTheme {
                AppRoot(
                    navigationViewModel = navigationViewModel,
                    gameScreenState = GameScreenState.initial()
                        .copy(gamePhase = GamePhase.ROLL_DICE),
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
                    onCreateLobbyClick = {},
                    onGoToLobbyClick = {},
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
        val navigationViewModel = NavigationViewModel()
        composeTestRule.setContent {
            ClientTheme {
                AppRoot(
                    navigationViewModel = navigationViewModel,
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
                    onCreateLobbyClick = {},
                    onGoToLobbyClick = {},
                    onReadyToggle = {},
                    onStartGame = {},
                    onLeaveLobby = {},
                )
            }
        }

        composeTestRule.onNodeWithText(HOME_SCREEN_LOBBY_CARD).assertIsDisplayed()
        composeTestRule.onNodeWithText(LOBBY_SCREEN_PLAYER_LIST).assertDoesNotExist()

        navigationViewModel.showLobby()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(LOBBY_SCREEN_PLAYER_LIST).assertIsDisplayed()
        composeTestRule.onNodeWithText(HOME_SCREEN_LOBBY_CARD).assertDoesNotExist()

        navigationViewModel.leaveLobby()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(HOME_SCREEN_LOBBY_CARD).assertIsDisplayed()
        composeTestRule.onNodeWithText(LOBBY_SCREEN_PLAYER_LIST).assertDoesNotExist()
    }

    private fun setAppRoot(loggedInAs: String?) {
        val navigationViewModel = NavigationViewModel()

        composeTestRule.setContent {
            ClientTheme {
                AppRoot(
                    navigationViewModel = navigationViewModel,
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
                    onCreateLobbyClick = {},
                    onGoToLobbyClick = {},
                    onReadyToggle = {},
                    onStartGame = {},
                    onLeaveLobby = {},
                )
            }
        }
    }

    @Test
    fun swapsScreenWhenGamePhaseTransitions() {
        val navigationViewModel = NavigationViewModel()
        var phase by mutableStateOf(GamePhase.NONE)
        composeTestRule.setContent {
            ClientTheme {
                AppRoot(
                    navigationViewModel = navigationViewModel,
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
                    onCreateLobbyClick = {},
                    onGoToLobbyClick = {},
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
    fun showsHomeScreenWhenLoggedInUserHasLobbyActions() {
        val navigationViewModel = NavigationViewModel()

        composeTestRule.setContent {
            ClientTheme {
                AppRoot(
                    navigationViewModel = navigationViewModel,
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
                    lobbyCode = null,
                    onCreateLobbyClick = {},
                    onGoToLobbyClick = {},
                    onReadyToggle = {},
                    onStartGame = {},
                    onLeaveLobby = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Create Lobby").assertIsDisplayed()
        composeTestRule.onNodeWithText("Join Lobby").assertIsDisplayed()
    }

    @Test
    fun showsWinnerScreenWhenGameStatusIsFinished() {
        val navigationViewModel = NavigationViewModel()

        composeTestRule.setContent {
            ClientTheme {
                AppRoot(
                    navigationViewModel = navigationViewModel,
                    gameScreenState = GameScreenState.initial().copy(
                        gameStatus = GameStatus.FINISHED,
                        roundNumber = 5,
                    ),
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
                    lobbyCode = null,
                    onCreateLobbyClick = {},
                    onGoToLobbyClick = {},
                    onReadyToggle = {},
                    onStartGame = {},
                    onLeaveLobby = {},
                )
            }
        }

        // FINISHED outranks every other screen — the winner screen header shows
        // and neither the start/home title nor the lobby list is rendered.
        composeTestRule.onNodeWithText("Congratulations to...").assertIsDisplayed()
        composeTestRule.onNodeWithText(START_SCREEN_TITLE).assertDoesNotExist()
        composeTestRule.onNodeWithText(LOBBY_SCREEN_PLAYER_LIST).assertDoesNotExist()
    }
}
