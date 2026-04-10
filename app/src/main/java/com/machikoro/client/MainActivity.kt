package com.machikoro.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.machikoro.client.config.AppConfig
import com.machikoro.client.network.websocket.OkHttpWebSocketClient
import com.machikoro.client.ui.start.StartScreen
import com.machikoro.client.ui.theme.ClientTheme

class MainActivity : ComponentActivity() {
    private val webSocketClient by lazy {
        OkHttpWebSocketClient(websocketUrl = AppConfig.websocketUrl)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClientTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    StartScreen(modifier = Modifier.padding(innerPadding))
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
