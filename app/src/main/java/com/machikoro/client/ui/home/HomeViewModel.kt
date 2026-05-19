package com.machikoro.client.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.network.websocket.WebSocketClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel(
    private val webSocketClient: WebSocketClient,
) : ViewModel() {

    private var createLobbyJob: Job? = null
    private var joinLobbyJob: Job? = null
    private val mutableJoinLobbyCode = MutableStateFlow("")
    val joinLobbyCode: StateFlow<String> = mutableJoinLobbyCode
    private val mutableJoinLobbyError = MutableStateFlow(false)
    val joinLobbyError: StateFlow<Boolean> = mutableJoinLobbyError
    val lobbyCode = webSocketClient.lobbyCode
    val activeGameId = webSocketClient.activeGameId
    val isLobbyHost = webSocketClient.isLobbyHost

    // FIX: Track whether a createLobby request is already in flight so rapid
    // taps don't send multiple LOBBY_CREATE frames to the server.
    private val mutableIsCreatingLobby = MutableStateFlow(false)

    fun createLobby() {
        // FIX: Ignore tap if a lobby creation is already pending or a lobby
        // code has already been assigned by the server.
        if (mutableIsCreatingLobby.value) return
        if (webSocketClient.lobbyCode.value != null) return

        mutableIsCreatingLobby.value = true

        if (webSocketClient.connectionStatus.value == ConnectionStatus.CONNECTED) {
            webSocketClient.sendCreateLobby()
            // Reset flag once the server confirms via lobbyCode flow
            viewModelScope.launch {
                webSocketClient.lobbyCode.first { it != null }
                mutableIsCreatingLobby.value = false
            }
            return
        }

        if (createLobbyJob?.isActive == true) {
            mutableIsCreatingLobby.value = false
            return
        }

        webSocketClient.connect()

        createLobbyJob = viewModelScope.launch {
            val status = webSocketClient.connectionStatus.first { status ->
                status == ConnectionStatus.CONNECTED ||
                        status == ConnectionStatus.ERROR ||
                        status == ConnectionStatus.DISCONNECTED
            }

            if (status == ConnectionStatus.CONNECTED) {
                webSocketClient.sendCreateLobby()
                webSocketClient.lobbyCode.first { it != null }
            }
            mutableIsCreatingLobby.value = false
        }
    }

    fun onJoinLobbyCodeChange(code: String) {
        mutableJoinLobbyError.value = false
        mutableJoinLobbyCode.value = code.trim().uppercase()
    }

    /**
     * Sends the entered lobby code once the WebSocket connection is ready.
     * If the socket is not connected yet, the first click starts the connection
     * and continues automatically after CONNECTED.
     */
    fun joinLobby() {
        val code = mutableJoinLobbyCode.value.trim()
        if (code.isBlank()) return

        if (webSocketClient.connectionStatus.value == ConnectionStatus.CONNECTED) {
            webSocketClient.sendJoinLobby(code)
            return
        }

        if (joinLobbyJob?.isActive == true) return

        webSocketClient.connect()

        joinLobbyJob = viewModelScope.launch {
            val status = webSocketClient.connectionStatus.first { status ->
                status == ConnectionStatus.CONNECTED ||
                        status == ConnectionStatus.ERROR ||
                        status == ConnectionStatus.DISCONNECTED
            }

            if (status == ConnectionStatus.CONNECTED) {
                webSocketClient.sendJoinLobby(code)
            }
        }
    }

    fun setJoinLobbyError(message: String) {
        mutableJoinLobbyError.value = true
    }

    fun startGame() {
        webSocketClient.sendGameStart()
    }

    fun clearLobbyCode() {
        webSocketClient.clearLobbyCode()
        mutableIsCreatingLobby.value = false
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