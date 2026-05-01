package com.machikoro.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.machikoro.client.config.AppConfig
import com.machikoro.client.domain.session.SessionManager
import com.machikoro.client.network.auth.AuthApiFactory
import com.machikoro.client.network.websocket.OkHttpWebSocketClient
import com.machikoro.client.ui.AppRoot
import com.machikoro.client.ui.game.GameScreenViewModel
import com.machikoro.client.ui.start.LoginDialogViewModel
import com.machikoro.client.ui.start.LogoutViewModel
import com.machikoro.client.ui.start.RegisterDialogViewModel
import com.machikoro.client.ui.start.StartScreenViewModel
import com.machikoro.client.ui.theme.ClientTheme

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
    private val startScreenViewModel by viewModels<StartScreenViewModel> {
        StartScreenViewModel.Factory(webSocketClient, SessionManager)
    }
    private val gameScreenViewModel by viewModels<GameScreenViewModel> {
        GameScreenViewModel.Factory(webSocketClient)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val startScreenState by startScreenViewModel.state.collectAsState()
            val gameScreenState by gameScreenViewModel.state.collectAsState()
            val registerDialogState by registerDialogViewModel.state.collectAsState()
            val loginDialogState by loginDialogViewModel.state.collectAsState()
            val logoutState by logoutViewModel.state.collectAsState()

            // Drive WebSocket lifecycle from session changes during the foreground.
            // onStart/onStop handle the activity-lifecycle case; this handles the
            // user-logs-in-or-out-while-app-is-open case. connect() and disconnect()
            // are both idempotent so it's safe to call them on every emission.
            LaunchedEffect(Unit) {
                SessionManager.session.collect { session ->
                    if (session != null) {
                        webSocketClient.connect()
                    } else {
                        webSocketClient.disconnect()
                    }
                }
            }

            ClientTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppRoot(
                        gameScreenState = gameScreenState,
                        startScreenState = startScreenState,
                        registerDialogState = registerDialogState,
                        loginDialogState = loginDialogState,
                        logoutState = logoutState,
                        onRegisterUsernameChange = registerDialogViewModel::usernameChanged,
                        onRegisterPasswordChange = registerDialogViewModel::passwordChanged,
                        onRegisterSubmit = registerDialogViewModel::submit,
                        onRegisterDialogReset = registerDialogViewModel::reset,
                        onLoginUsernameChange = loginDialogViewModel::usernameChanged,
                        onLoginPasswordChange = loginDialogViewModel::passwordChanged,
                        onLoginSubmit = loginDialogViewModel::submit,
                        onLoginDialogReset = loginDialogViewModel::reset,
                        onLogoutSubmit = logoutViewModel::submit,
                        modifier = Modifier.padding(innerPadding)
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
