package com.machikoro.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.machikoro.client.config.AppConfig
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.network.websocket.OkHttpWebSocketClient
import com.machikoro.client.ui.game.GameScreen
import com.machikoro.client.ui.game.GameScreenViewModel
import com.machikoro.client.ui.start.StartScreen
import com.machikoro.client.ui.start.StartScreenViewModel
import com.machikoro.client.ui.theme.ClientTheme

class MainActivity : ComponentActivity() {
    private val webSocketClient by lazy {
        OkHttpWebSocketClient(websocketUrl = AppConfig.websocketUrl)
    }
    private val startScreenViewModel by viewModels<StartScreenViewModel> {
        StartScreenViewModel.Factory(webSocketClient)
    }
    private val gameScreenViewModel by viewModels<GameScreenViewModel> {
        GameScreenViewModel.Factory(webSocketClient)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val startState by startScreenViewModel.state.collectAsState()
            val gameState by gameScreenViewModel.state.collectAsState()

            ClientTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (startState.gamePhase != GamePhase.NONE) {
                        GameScreen(
                            state = gameState,
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        StartScreen(
                            state = startState,
                            onStartGame = { startScreenViewModel.startGame() },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
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