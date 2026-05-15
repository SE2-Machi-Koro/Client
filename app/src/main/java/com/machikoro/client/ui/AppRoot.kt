package com.machikoro.client.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
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
import com.machikoro.client.ui.win.GameOverOneWinner

@Composable
fun AppRoot(
    gameScreenState: GameScreenState,
    startScreenState: StartScreenState,
    lobbyScreenState: LobbyScreenState,
    registerDialogState: RegisterDialogState,
    loginDialogState: LoginDialogState,
    logoutState: LogoutState,
    lobbyCode: String?,
    joinLobbyCode: String = "",
    showJoinLobbyInput: Boolean = false,
    loggedInAs: String?,
    onRegisterUsernameChange: (String) -> Unit,
    onRegisterPasswordChange: (String) -> Unit,
    onRegisterSubmit: () -> Unit,
    onRegisterDialogReset: () -> Unit,
    onLoginUsernameChange: (String) -> Unit,
    onLoginPasswordChange: (String) -> Unit,
    onLoginSubmit: () -> Unit,
    onCreateLobbyClick: () -> Unit,
    onJoinLobbyClick: () -> Unit = {},
    onJoinLobbyCodeChange: (String) -> Unit = {},
    onJoinLobbySubmit: () -> Unit = {},
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
    if (gameScreenState.gameStatus == GameStatus.FINISHED) {
        // A finished game outranks every other screen: even mid-phase, once the
        // snapshot reports FINISHED the player should see the end screen.
        GameOverOneWinner(
            winnerName = resolveWinnerName(gameScreenState),
            roundsNumber = gameScreenState.roundNumber ?: 0,
        )
    } else if (gameScreenState.gamePhase != GamePhase.NONE) {
        GameScreen(
            state = gameScreenState,
            onRollDice = onRollDice,
            onPurchaseClick = onPurchaseClick,
            modifier = modifier
        )
    } else if (showLobbyScreen) {
        LobbyScreen(
            state = lobbyScreenState,
            lobbyCode = lobbyCode,
            onReadyToggle = onReadyToggle,
            onStartGame = onStartGame,
            onLeaveLobby = onLeaveLobby,
            modifier = modifier
        )
    } else if (loggedInAs != null) {
        HomeScreen(
            lobbyCode = lobbyCode,
            joinLobbyCode = joinLobbyCode,
            showJoinLobbyInput = showJoinLobbyInput && lobbyCode == null,
            onJoinLobbyClick = onJoinLobbyClick,
            onJoinLobbyCodeChange = onJoinLobbyCodeChange,
            onJoinLobbySubmit = onJoinLobbySubmit,
            onCreateLobbyClick = onCreateLobbyClick,
            onGoToLobbyClick = onGoToLobbyClick,
            onLogoutClick = onLogoutSubmit,
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

/**
 * Derives the winner from the reconnect snapshot: in Machi Koro the game ends
 * when a player has built all four landmarks, so the winner is the player whose
 * landmark list is non-empty and fully built. Falls back to a generic label if
 * the snapshot doesn't pin down a single winner.
 */
private fun resolveWinnerName(state: GameScreenState): String {
    val winnerPlayerId = state.playerLandmarks.entries
        .firstOrNull { (_, landmarks) ->
            landmarks.isNotEmpty() && landmarks.all { it.isBuilt }
        }
        ?.key
    return state.players
        .firstOrNull { it.id == winnerPlayerId?.toString() }
        ?.displayName
        ?: "the winner"
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
            loggedInAs = null,
            onCreateLobbyClick = {},
            joinLobbyCode = "",
            showJoinLobbyInput = false,
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
            loggedInAs = null,
            onCreateLobbyClick = {},
            joinLobbyCode = "",
            showJoinLobbyInput = false,
        )
    }
}