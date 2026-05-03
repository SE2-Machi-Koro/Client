package com.machikoro.client.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.LoginDialogState
import com.machikoro.client.domain.model.state.LogoutState
import com.machikoro.client.domain.model.state.RegisterDialogState
import com.machikoro.client.domain.model.state.StartScreenState
import com.machikoro.client.ui.game.GameScreen
import com.machikoro.client.ui.home.HomeScreen
import com.machikoro.client.ui.start.StartScreen
import com.machikoro.client.ui.theme.ClientTheme

@Composable
fun AppRoot(
    gameScreenState: GameScreenState,
    startScreenState: StartScreenState,
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
    onStartGame: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (gameScreenState.gamePhase != GamePhase.NONE) {
        GameScreen(state = gameScreenState, modifier = modifier)
    } else if (loggedInAs != null) {
        HomeScreen(
            lobbyCode = lobbyCode,
            onCreateLobbyClick = onCreateLobbyClick,
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
            onStartGame = onStartGame,
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
            loggedInAs = null,
            onCreateLobbyClick = {},
        )
    }
}
