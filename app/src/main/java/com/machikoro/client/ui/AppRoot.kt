package com.machikoro.client.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
    isLobbyHost: Boolean,
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
    onPurchaseClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    showLobbyScreen: Boolean = false,
    onGoToLobbyClick: () -> Unit = {},
) {
    if (gameScreenState.gamePhase != GamePhase.NONE) {
     // Game actions stay owned by GameScreenViewModel and are passed into the UI.
        GameScreen(
            state = gameScreenState,
            onRollDice = onRollDice,
            onPurchaseClick = onPurchaseClick,
            modifier = modifier
        )
    } else if (lobbyCode != null) {
        LobbyScreen(
            state = lobbyScreenState,
            onReadyToggle = onReadyToggle,
            onStartGame = onStartGame,
            onLeaveLobby = onLeaveLobby,
            modifier = modifier
        )
    } else if (loggedInAs != null) {
        HomeScreen(
            lobbyCode = lobbyCode,
            isLobbyHost = isLobbyHost,
            canStartGame = isLobbyHost &&
                    startScreenState.connectionStatus == ConnectionStatus.CONNECTED,
            onCreateLobbyClick = onCreateLobbyClick,
            onStartGame = onStartGame,
            showLobbyScreen = showLobbyScreen,
            onGoToLobbyClick = onGoToLobbyClick,
            modifier = modifier
        )
    } else {
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
            modifier = modifier
        )
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