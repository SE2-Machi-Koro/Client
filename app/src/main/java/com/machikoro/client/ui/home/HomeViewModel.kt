package com.machikoro.client.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.network.websocket.WebSocketClient

class HomeViewModel(
    private val webSocketClient: WebSocketClient,
) : ViewModel() {

    val lobbyCode = webSocketClient.lobbyCode
    val activeGameId = webSocketClient.activeGameId
    val isLobbyHost = webSocketClient.isLobbyHost

    fun createLobby() {
        if (webSocketClient.connectionStatus.value != ConnectionStatus.CONNECTED) {
            webSocketClient.connect()
            return
        }

        webSocketClient.sendCreateLobby()
    }

    fun startGame() {
        webSocketClient.sendGameStart()
    }

    fun clearLobbyCode() {
        webSocketClient.clearLobbyCode()
    }

    class Factory(
        private val webSocketClient: WebSocketClient,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return HomeViewModel(webSocketClient) as T
        }
    }
}
