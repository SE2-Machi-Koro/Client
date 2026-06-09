package com.machikoro.client.ui

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.LoginDialogState
import com.machikoro.client.domain.model.state.LobbyScreenState
import com.machikoro.client.domain.model.state.LogoutState
import com.machikoro.client.domain.model.state.RegisterDialogState
import com.machikoro.client.domain.model.state.StartScreenState
import com.machikoro.client.ui.connection.ConnectionBannerState
import com.machikoro.client.ui.connection.ConnectionStatusBanner
import com.machikoro.client.ui.game.GameScreen
import com.machikoro.client.ui.home.HomeScreen
import com.machikoro.client.ui.lobby.LobbyScreen
import com.machikoro.client.ui.navigation.AppNavigator
import com.machikoro.client.ui.navigation.AppRoute
import com.machikoro.client.ui.navigation.NavigationEvent
import com.machikoro.client.ui.navigation.NavigationViewModel
import com.machikoro.client.ui.leaderboard.LeaderboardScreen
import com.machikoro.client.ui.leaderboard.LeaderboardState
import com.machikoro.client.ui.start.StartScreen
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.ui.win.GameOverOneWinner
import com.machikoro.client.ui.win.resolveRankedPlayers
import com.machikoro.client.ui.win.resolveWinnerName
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
    connectionBannerState: ConnectionBannerState = ConnectionBannerState.Hidden,
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
    onRollDice: (diceCount: Int) -> Unit = {},
    onTurnFlowAction: () -> Unit = {},
    onPurchaseClick: (String) -> Unit = {},
    onBackHome: () -> Unit = {},
    onLeaveGame: () -> Unit = {},
    onEndGame: () -> Unit = {},
    cheatRecommendation: CardType? = null,
    onShake: () -> Unit = {},
    hasActiveGame: Boolean = false,
    onResumeGameClick: () -> Unit = {},
    onPurgeClick: () -> Unit = {},
    leaderboardState: LeaderboardState = LeaderboardState.Loading,
    onLeaderboardRetry: () -> Unit = {},
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val appNavigator = remember(navController) { AppNavigator(navController) }
    val navigationUiState by navigationViewModel.uiState.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val showConnectionBanner = currentRoute != null && currentRoute != AppRoute.Main.route

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, _, _ ->
            navigationViewModel.clearLastNavigation()
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    LaunchedEffect(navigationViewModel) {
        navigationViewModel.navigationEvent.collectLatest { event ->
            when (event) {
                is NavigationEvent.NavigateTo ->
                    appNavigator.navigateTo(event.route, event.arguments)
            }
        }
    }

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

    Column(modifier = modifier) {
        AnimatedVisibility(
            visible = showConnectionBanner && connectionBannerState !is ConnectionBannerState.Hidden,
        ) {
            ConnectionStatusBanner(state = connectionBannerState)
        }
        NavHost(
            navController = navController,
            startDestination = AppRoute.Main.route,
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
                    username = startScreenState.loggedInAs,
                    joinLobbyCode = joinLobbyCode,
                    showJoinLobbyInput = showJoinLobbyInput && lobbyCode == null,
                    onJoinLobbyClick = onJoinLobbyClick,
                    onJoinLobbyCodeChange = onJoinLobbyCodeChange,
                    onJoinLobbySubmit = onJoinLobbySubmit,
                    joinLobbyError = joinLobbyError,
                    onCreateLobbyClick = onCreateLobbyClick,
                    hasActiveGame = hasActiveGame,
                    onResumeGameClick = onResumeGameClick,
                    onPurgeClick = onPurgeClick,
                    onLogoutClick = onLogoutSubmit,
                    // Navigate to leaderboard keeping Home in the back stack
                    onRankingClick = {
                        navController.navigate(AppRoute.Leaderboard.route) {
                            launchSingleTop = true
                        }
                    },
                    modifier = modifier,
                )
            }

            composable(AppRoute.Leaderboard.route) {
                LeaderboardScreen(
                    state = leaderboardState,
                    onBackClick = { navController.popBackStack() },
                    onRetry = onLeaderboardRetry,
                )
            }

            composable(
                route = AppRoute.Lobby.route,
                arguments = listOf(
                    navArgument(AppRoute.Lobby.LOBBY_CODE_ARGUMENT) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
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
                    },
                ),
            ) { backStackEntry ->
                val routedGameId = backStackEntry.arguments
                    ?.getInt(AppRoute.Game.GAME_ID_ARGUMENT)
                    ?.takeIf { it != AppRoute.Game.MISSING_GAME_ID }
                GameScreen(
                    state = gameScreenState.copy(gameId = routedGameId ?: gameScreenState.gameId),
                    onRollDice = onRollDice,
                    onTurnFlowAction = onTurnFlowAction,
                    onPurchaseClick = onPurchaseClick,
                    onLeaveGame = onLeaveGame,
                    onEndGame = onEndGame,
                    cheatRecommendation = cheatRecommendation,
                    onShake = onShake,
                )
            }

            composable(AppRoute.Winner.route) {
                GameOverOneWinner(
                    winnerName = resolveWinnerName(gameScreenState),
                    roundsNumber = gameScreenState.roundNumber ?: 0,
                    rankedPlayers = resolveRankedPlayers(gameScreenState),
                    onBackHome = onBackHome,
                    onViewLeaderboard = {
                        // Pop winner screen so back from leaderboard goes to home, not winner
                        navController.navigate(AppRoute.Leaderboard.route) {
                            launchSingleTop = true
                            popUpTo(AppRoute.Winner.route) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
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
            navigationViewModel = NavigationViewModel(),
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
            connectionBannerState = ConnectionBannerState.Disconnected,
            navigationViewModel = NavigationViewModel(),
        )
    }
}