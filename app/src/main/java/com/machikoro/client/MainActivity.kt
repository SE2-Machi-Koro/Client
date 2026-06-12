package com.machikoro.client

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.machikoro.client.domain.enums.GamePhase
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.machikoro.client.config.AppConfig
import com.machikoro.client.domain.session.DataStoreSessionStorage
import com.machikoro.client.domain.session.SessionManager
import com.machikoro.client.network.auth.AuthApiFactory
import com.machikoro.client.network.debug.DebugApiFactory
import com.machikoro.client.network.health.BackendHealthRepository
import com.machikoro.client.network.health.HealthApiFactory
import com.machikoro.client.network.leaderboard.LeaderboardApiFactory
import com.machikoro.client.network.websocket.OkHttpWebSocketClient
import com.machikoro.client.ui.AppRoot
import com.machikoro.client.ui.connection.ConnectionBannerViewModel
import com.machikoro.client.ui.game.GameScreenViewModel
import com.machikoro.client.ui.home.HomeViewModel
import com.machikoro.client.ui.leaderboard.LeaderboardViewModel
import com.machikoro.client.ui.lobby.LobbyScreenViewModel
import com.machikoro.client.ui.navigation.NavigationViewModel
import com.machikoro.client.ui.start.LoginDialogViewModel
import com.machikoro.client.ui.start.LogoutViewModel
import com.machikoro.client.ui.start.RegisterDialogViewModel
import com.machikoro.client.ui.start.StartScreenViewModel
import com.machikoro.client.ui.theme.ClientTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val webSocketClient by lazy {
        OkHttpWebSocketClient(
            websocketUrl = AppConfig.websocketUrl,
            sessionStateHolder = SessionManager,
        )
    }
    private val authApi by lazy {
        AuthApiFactory.create(AppConfig.backendBaseUrl)
    }
    private val debugApi by lazy {
        DebugApiFactory.create(AppConfig.backendBaseUrl) { SessionManager.session.value?.sessionToken }
    }
    private val healthApi by lazy {
        HealthApiFactory.create(AppConfig.backendBaseUrl)
    }
    private val healthRepository by lazy {
        BackendHealthRepository(healthApi)
    }
    private val startScreenViewModel by viewModels<StartScreenViewModel> {
        StartScreenViewModel.Factory(webSocketClient, SessionManager, healthRepository)
    }
    private val gameScreenViewModel by viewModels<GameScreenViewModel> {
        GameScreenViewModel.Factory(webSocketClient, SessionManager, debugApi)
    }
    private val homeViewModel by viewModels<HomeViewModel> {
        HomeViewModel.Factory(webSocketClient)
    }
    private val lobbyScreenViewModel by viewModels<LobbyScreenViewModel> {
        LobbyScreenViewModel.Factory(webSocketClient, SessionManager, debugApi)
    }
    private val registerDialogViewModel by viewModels<RegisterDialogViewModel> {
        RegisterDialogViewModel.Factory(authApi)
    }
    private val loginDialogViewModel by viewModels<LoginDialogViewModel> {
        LoginDialogViewModel.Factory(authApi, SessionManager)
    }
    private val logoutViewModel by viewModels<LogoutViewModel> {
        LogoutViewModel.Factory(authApi, SessionManager)
    }

    private val navigationViewModel by viewModels<NavigationViewModel> {
        NavigationViewModel.Factory()
    }

    private val connectionBannerViewModel by viewModels<ConnectionBannerViewModel> {
        ConnectionBannerViewModel.Factory(webSocketClient, SessionManager)
    }
    private val leaderboardApi by lazy {
        LeaderboardApiFactory.create(AppConfig.backendBaseUrl) { SessionManager.session.value?.sessionToken }
    }
    private val leaderboardViewModel by viewModels<LeaderboardViewModel> {
        LeaderboardViewModel.Factory(leaderboardApi)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.attach(DataStoreSessionStorage(applicationContext))
        // Only sign out on a genuine fresh launch, not rotation or process recreation
        if (savedInstanceState == null) lifecycleScope.launch { SessionManager.signOut() }
        enableEdgeToEdge()
        setContent {
            val startScreenState by startScreenViewModel.state.collectAsState()
            val gameScreenState by gameScreenViewModel.state.collectAsState()
            val cheatRecommendation by gameScreenViewModel.cheatRecommendation.collectAsState()
            val context = LocalContext.current
            val lobbyCode by homeViewModel.lobbyCode.collectAsState()
            val activeGameId by homeViewModel.activeGameId.collectAsState()
            val joinLobbyCode by homeViewModel.joinLobbyCode.collectAsState()
            val joinLobbyError by homeViewModel.joinLobbyError.collectAsState()
            val lobbyScreenState by lobbyScreenViewModel.state.collectAsState()
            val registerDialogState by registerDialogViewModel.state.collectAsState()
            val loginDialogState by loginDialogViewModel.state.collectAsState()
            val logoutState by logoutViewModel.state.collectAsState()
            val connectionBannerState by connectionBannerViewModel.state.collectAsState()
            val leaderboardState by leaderboardViewModel.state.collectAsState()
            var showJoinLobbyInput by remember { mutableStateOf(false) }
            val snackbarHostState = remember { SnackbarHostState() }
            val hasActiveGame = activeGameId != null && gameScreenState.gamePhase != GamePhase.NONE

            LaunchedEffect(Unit) {
                SessionManager.session.collect { session ->
                    if (session != null) {
                        webSocketClient.connect()
                    } else {
                        webSocketClient.disconnect()
                    }
                }
            }

            LaunchedEffect(Unit) {
                webSocketClient.authRejections.collect {
                    SessionManager.signOut()
                    navigationViewModel.leaveLobby()
                    homeViewModel.clearLobbyCode()
                    snackbarHostState.showSnackbar(
                        "Sitzung abgelaufen, bitte erneut anmelden"
                    )
                }
            }

            LaunchedEffect(activeGameId) {
                if (activeGameId != null) {
                    showJoinLobbyInput = false
                }
            }

            LaunchedEffect(Unit) {
                webSocketClient.lobbyEntered.collect {
                    // Navigate to LobbyScreen only on fresh lobby entry, not reconnect snapshots
                    navigationViewModel.showLobby()
                }
            }

            LaunchedEffect(Unit) {
                webSocketClient.hostLeftLobby.collect {
                    navigationViewModel.leaveLobby()
                    homeViewModel.clearLobbyCode()

                    snackbarHostState.showSnackbar(
                        "Lobby closed. Choose another one."
                    )
                }
            }

            LaunchedEffect(Unit) {
                webSocketClient.lobbyJoinErrors.collect { error ->
                    Log.e("MainActivity", "Lobby join error received: ${error.diagnosticMessage}")
                    homeViewModel.setJoinLobbyError(error.userMessage)
                    // Return to HomeScreen so the error is visible
                    navigationViewModel.leaveLobby()
                }
            }

            // Insider Trading cheat (#203): brief toast each time a shake activates it.
            LaunchedEffect(Unit) {
                gameScreenViewModel.cheatActivations.collect {
                    Toast.makeText(context, "Insider Trading active", Toast.LENGTH_SHORT).show()
                }
            }

            // Debug End-game (#191): surface End-game button failures as a snackbar.
            LaunchedEffect(Unit) {
                gameScreenViewModel.debugEndGameErrors.collect { message ->
                    snackbarHostState.showSnackbar(message)
                }
            }

            ClientTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    contentWindowInsets = WindowInsets(0)
                ) { innerPadding ->
                    AppRoot(
                        navigationViewModel = navigationViewModel,
                        gameScreenState = gameScreenState,
                        startScreenState = startScreenState,
                        lobbyScreenState = lobbyScreenState,
                        registerDialogState = registerDialogState,
                        loginDialogState = loginDialogState,
                        logoutState = logoutState,
                        connectionBannerState = connectionBannerState,
                        leaderboardState = leaderboardState,
                        onLeaderboardRetry = leaderboardViewModel::load,
                        onRegisterUsernameChange = registerDialogViewModel::usernameChanged,
                        onRegisterPasswordChange = registerDialogViewModel::passwordChanged,
                        onRegisterSubmit = registerDialogViewModel::submit,
                        onRegisterDialogReset = registerDialogViewModel::reset,
                        onLoginUsernameChange = loginDialogViewModel::usernameChanged,
                        onLoginPasswordChange = loginDialogViewModel::passwordChanged,
                        onLoginSubmit = loginDialogViewModel::submit,
                        onLoginDialogReset = loginDialogViewModel::reset,
                        onLogoutSubmit = logoutViewModel::submit,
                        onReadyToggle = lobbyScreenViewModel::onReadyToggle,
                        onStartGame = homeViewModel::startGame,
                        onFillWithDummies = lobbyScreenViewModel::fillWithDummies,
                        onResetLobby = lobbyScreenViewModel::resetLobby,
                        onLeaveLobby = {
                            navigationViewModel.leaveLobby()
                            lobbyScreenViewModel.onLeaveLobby()
                            homeViewModel.clearLobbyCode()
                        },
                        onRollDice = gameScreenViewModel::rollDice,
                        cheatRecommendation = cheatRecommendation,
                        onShake = gameScreenViewModel::onShake,
                        onTurnFlowAction = gameScreenViewModel::performTurnFlowAction,
                        modifier = Modifier.padding(innerPadding),
                        lobbyCode = lobbyCode,
                        joinLobbyCode = joinLobbyCode,
                        joinLobbyError = joinLobbyError,
                        showJoinLobbyInput = showJoinLobbyInput,
                        onCreateLobbyClick = {
                            showJoinLobbyInput = false
                            homeViewModel.createLobby()
                        },
                        onLeaveGame = {
                            navigationViewModel.leaveLobby()
                        },
                        onEndGame = {
                            gameScreenViewModel.endGame()
                        },
                        hasActiveGame = hasActiveGame,
                        onResumeGameClick = {
                            navigationViewModel.resumeGame(activeGameId)
                        },
                        onPurgeClick = {
                            lifecycleScope.launch {
                                try {
                                    val response = debugApi.purge()
                                    if (response.isSuccessful) {
                                        webSocketClient.clearGameState()
                                        navigationViewModel.leaveLobby()
                                        snackbarHostState.showSnackbar("DB purged")
                                    } else {
                                        snackbarHostState.showSnackbar("Purge failed (${response.code()})")
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Purge error: ${e.message}")
                                }
                            }
                        },
                        onPurchaseClick = gameScreenViewModel::selectPurchaseItem,
                        onBuySelectedClick = gameScreenViewModel::purchaseSelectedItem,
                        onBackHome = {
                            gameScreenViewModel.clearGameState()
                            navigationViewModel.returnHome()
                        },
                        onClearGameState = {
                            gameScreenViewModel.clearGameState()
                        },
                        onJoinLobbyClick = {
                            homeViewModel.clearLobbyCode()
                            showJoinLobbyInput = true
                        },
                        onJoinLobbyCodeChange = homeViewModel::onJoinLobbyCodeChange,
                        onJoinLobbySubmit = homeViewModel::joinLobby,
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        webSocketClient.connect()
    }

    override fun onStop() {
        webSocketClient.disconnect()
        super.onStop()
    }
}