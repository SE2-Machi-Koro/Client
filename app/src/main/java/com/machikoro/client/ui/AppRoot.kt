package com.machikoro.client.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.LoginDialogState
import com.machikoro.client.domain.model.state.LobbyScreenState
import com.machikoro.client.domain.model.state.LogoutState
import com.machikoro.client.domain.model.state.RegisterDialogState
import com.machikoro.client.domain.model.state.StartScreenState
import com.machikoro.client.ui.game.GameScreen
import com.machikoro.client.ui.home.HomeScreen
import com.machikoro.client.ui.lobby.LobbyScreen
import com.machikoro.client.ui.navigation.AppRoute
import com.machikoro.client.ui.start.StartScreen
import com.machikoro.client.ui.theme.ClientTheme

@Composable
fun AppRoot(
    gameScreenState: GameScreenState,
    startScreenState: StartScreenState,
    lobbyScreenState: LobbyScreenState,
    registerDialogState: RegisterDialogState,
    loginDialogState: LoginDialogState,
    logoutState: LogoutState,
    lobbyCode: String?,
    loggedInAs: String?,
    onRegisterUsernameChange: (String) -> Unit,
    onRegisterPasswordChange: (String) -> Unit,
    onRegisterSubmit: () -> Unit,
    onRegisterDialogReset: () -> Unit,
    onLoginUsernameChange: (String) -> Unit,
    onLoginPasswordChange: (String) -> Unit,
    onLoginSubmit: () -> Unit,
    onCreateLobbyClick: () -> Unit,
    onLoginDialogReset: () -> Unit,
    onLogoutSubmit: () -> Unit,
    onReadyToggle: () -> Unit = {},
    onStartGame: () -> Unit = {},
    onLeaveLobby: () -> Unit = {},
    onRollDice: () -> Unit = {},
    modifier: Modifier = Modifier,
    isLobbyHost: Boolean = false,
    showLobbyScreen: Boolean = false,
    onGoToLobbyClick: () -> Unit = {},
) {
    val navController = rememberNavController()

    // Keep the current state-based screen priority while hosting screens in one NavHost.
    // TODO(#68,#69): Move route decisions into ViewModel navigation state/events.
    val targetRoute = when {
        gameScreenState.gamePhase != GamePhase.NONE -> AppRoute.Game
        showLobbyScreen -> AppRoute.Lobby
        loggedInAs != null -> AppRoute.Home
        else -> AppRoute.Main
    }

    LaunchedEffect(targetRoute) {
        if (navController.currentDestination?.route != targetRoute.route) {
            navController.navigate(
                targetRoute.route,
                navOptions {
                    launchSingleTop = true
                    popUpTo(AppRoute.Main.route)
                }
            )
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoute.Main.route,
        modifier = modifier,
    ) {
        composable(AppRoute.Main.route) {
            StartScreen(
                state = startScreenState,
                registerDialogState = registerDialogState,
                loginDialogState = loginDialogState,
                logoutState = logoutState,
                onRegisterUsernameChange = onRegisterUsernameChange,
                onRegisterPasswordChange = onRegisterPasswordChange,
                onRegisterSubmit = onRegisterSubmit,
                onRegisterDialogReset = onRegisterDialogReset,
                onLoginUsernameChange = onLoginUsernameChange,
                onLoginPasswordChange = onLoginPasswordChange,
                onLoginSubmit = onLoginSubmit,
                onLoginDialogReset = onLoginDialogReset,
                onLogoutSubmit = onLogoutSubmit,
            )
        }
        composable(AppRoute.Home.route) {
            HomeScreen(
                lobbyCode = lobbyCode,
                isLobbyHost = isLobbyHost,
                canStartGame = isLobbyHost &&
                    startScreenState.connectionStatus == ConnectionStatus.CONNECTED,
                onCreateLobbyClick = onCreateLobbyClick,
                onStartGame = onStartGame,
                showLobbyScreen = showLobbyScreen,
                onGoToLobbyClick = onGoToLobbyClick,
            )
        }
        composable(AppRoute.Lobby.route) {
            LobbyScreen(
                state = lobbyScreenState,
                onReadyToggle = onReadyToggle,
                onStartGame = onStartGame,
                onLeaveLobby = onLeaveLobby,
            )
        }
        composable(AppRoute.Game.route) {
            GameScreen(
                state = gameScreenState,
                onRollDice = onRollDice,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 917, heightDp = 412)
@Composable
private fun AppRootStartScreenPreview() {
    ClientTheme {
        AppRoot(
            gameScreenState = GameScreenState.initial(),
            startScreenState = StartScreenState.placeholder(),
            lobbyScreenState = LobbyScreenState.placeholder(),
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
            isLobbyHost = false,
            loggedInAs = null,
            onCreateLobbyClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 917, heightDp = 412)
@Composable
private fun AppRootGameScreenPreview() {
    ClientTheme {
        AppRoot(
            gameScreenState = GameScreenState.initial().copy(gamePhase = GamePhase.ROLL_DICE),
            startScreenState = StartScreenState.placeholder(),
            lobbyScreenState = LobbyScreenState.placeholder(),
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
            isLobbyHost = false,
            loggedInAs = null,
            onCreateLobbyClick = {},
        )
    }
}