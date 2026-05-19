package com.machikoro.client.ui

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.LoginDialogState
import com.machikoro.client.domain.model.state.LobbyScreenState
import com.machikoro.client.domain.model.state.LogoutState
import com.machikoro.client.domain.model.state.RegisterDialogState
import com.machikoro.client.domain.model.state.StartScreenState
import com.machikoro.client.ui.game.GameScreen
import com.machikoro.client.ui.home.HomeScreen
import com.machikoro.client.ui.lobby.LobbyScreen
import com.machikoro.client.ui.navigation.AppNavigator
import com.machikoro.client.ui.navigation.AppRoute
import com.machikoro.client.ui.navigation.NavigationEvent
import com.machikoro.client.ui.navigation.NavigationViewModel
import com.machikoro.client.ui.start.StartScreen
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.ui.win.GameOverOneWinner
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AppRoot(
    navigationViewModel: NavigationViewModel,
    gameScreenState: GameScreenState,
    startScreenState: StartScreenState,
    lobbyScreenState: LobbyScreenState,
    registerDialogState: RegisterDialogState,
    loginDialogState: LoginDialogState,
    logoutState: LogoutState,
    lobbyCode: String?,
    joinLobbyCode: String = "",
    showJoinLobbyInput: Boolean = false,
    joinLobbyError: Boolean = false,
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
    onFillWithDummies: () -> Unit = {},
    onResetLobby: () -> Unit = {},
    onRollDice: () -> Unit = {},
    onPurchaseClick: (String) -> Unit = {},
    onPurgeClick: () -> Unit = {},
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val appNavigator = remember(navController) { AppNavigator(navController) }
    val navigationUiState by navigationViewModel.uiState.collectAsState()

    // AppRoot owns the NavHost lifecycle, but route decisions are delegated to
    // NavigationViewModel so navigation state has one source of truth.

    // Reset NavigationViewModel idempotency cache when the NavController actually
    // changes destination, so the same navigation can be re-emitted later if
    // needed.
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, _, _ ->
            navigationViewModel.clearLastNavigation()
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    // Listen to navigation events from ViewModel and apply them
    LaunchedEffect(navigationViewModel) {
        navigationViewModel.navigationEvent.collectLatest { event ->
            when (event) {
                is NavigationEvent.NavigateTo ->
                    appNavigator.navigateTo(event.route, event.arguments)
            }
        }
    }

    // Delegate state-based route decisions to NavigationViewModel
    LaunchedEffect(
        gameScreenState,
        startScreenState,
        lobbyCode,
        navigationUiState.showLobbyScreen
    ) {
        navigationViewModel.updateNavigationBasedOnState(
            gameScreenState = gameScreenState,
            startScreenState = startScreenState,
            lobbyCode = lobbyCode,
        )
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
                joinLobbyCode = joinLobbyCode,
                showJoinLobbyInput = showJoinLobbyInput && lobbyCode == null,
                onJoinLobbyClick = onJoinLobbyClick,
                onJoinLobbyCodeChange = onJoinLobbyCodeChange,
                onJoinLobbySubmit = onJoinLobbySubmit,
                joinLobbyError = joinLobbyError,
                onCreateLobbyClick = onCreateLobbyClick,
                onPurgeClick = onPurgeClick,
                onLogoutClick = onLogoutSubmit,
                modifier = modifier
            )
        }

        composable(
            route = AppRoute.Lobby.route,
            arguments = listOf(
                navArgument(AppRoute.Lobby.LOBBY_CODE_ARGUMENT) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
        ) { backStackEntry ->
            val routedLobbyCode = backStackEntry.arguments
                ?.getString(AppRoute.Lobby.LOBBY_CODE_ARGUMENT)
                ?.takeIf { it.isNotBlank() }
            LobbyScreen(
                state = lobbyScreenState,
                lobbyCode = routedLobbyCode ?: lobbyCode,
                onReadyToggle = onReadyToggle,
                onStartGame = onStartGame,
                onLeaveLobby = onLeaveLobby,
                onFillWithDummies = onFillWithDummies,
                onResetLobby = onResetLobby,
            )
        }

        composable(
            route = AppRoute.Game.route,
            arguments = listOf(
                navArgument(AppRoute.Game.GAME_ID_ARGUMENT) {
                    type = NavType.IntType
                    defaultValue = AppRoute.Game.MISSING_GAME_ID
                }
            ),
        ) { backStackEntry ->
            val routedGameId = backStackEntry.arguments
                ?.getInt(AppRoute.Game.GAME_ID_ARGUMENT)
                ?.takeIf { it != AppRoute.Game.MISSING_GAME_ID }
            GameScreen(
                state = gameScreenState.copy(gameId = routedGameId ?: gameScreenState.gameId),
                onRollDice = onRollDice,
                onPurchaseClick = onPurchaseClick,
            )
        }

        composable(AppRoute.Winner.route) {
            GameOverOneWinner(
                winnerName = resolveWinnerName(gameScreenState),
                roundsNumber = gameScreenState.roundNumber ?: 0,
            )
        }
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

@SuppressLint("ViewModelConstructorInComposable")
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
            onCreateLobbyClick = {},
            navigationViewModel = NavigationViewModel()
        )
    }
}

@SuppressLint("ViewModelConstructorInComposable")
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
            onCreateLobbyClick = {},
            navigationViewModel = NavigationViewModel()
        )
    }
}
