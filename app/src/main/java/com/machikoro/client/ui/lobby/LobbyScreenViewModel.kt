package com.machikoro.client.ui.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.machikoro.client.domain.model.state.LobbyStatus
import com.machikoro.client.domain.model.state.LobbyScreenState
import com.machikoro.client.domain.session.SessionStateHolder
import com.machikoro.client.network.websocket.WebSocketClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LobbyScreenViewModel(
    private val webSocketClient: WebSocketClient,
    private val sessionStateHolder: SessionStateHolder,
) : ViewModel() {

    val state: StateFlow<LobbyScreenState>
        get() = mutableState.asStateFlow()

    private val mutableState = MutableStateFlow(
        LobbyScreenState.placeholder().copy(
            lobbyStatus = LobbyStatus.WAITING_FOR_PLAYERS
        )
    )

    private val mutableIsReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean>
        get() = mutableIsReady.asStateFlow()

    init {
        observeSession()
        observePlayers()
        observeConnectionStatus()
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionStateHolder.session.collect { session ->
                mutableState.update { current ->
                    current.copy(
                        loggedInAs = session?.username
                    )
                }
            }
        }
    }

    private fun observePlayers() {
        viewModelScope.launch {
            webSocketClient.players.collect { players ->
                mutableState.update { current ->
                    current.copy(
                        playerList = players.map { it.displayName },
                        lobbyStatus = if (players.size >= 2) {
                            LobbyStatus.READY
                        } else {
                            LobbyStatus.WAITING_FOR_PLAYERS
                        }
                    )
                }
            }
        }
    }

    private fun observeConnectionStatus() {
        viewModelScope.launch {
            webSocketClient.connectionStatus.collect { connectionStatus ->
                mutableState.update { current ->
                    current.copy(
                        connectionStatus = connectionStatus
                    )
                }
            }
        }
    }

    fun onStartGame() {
        val currentState = mutableState.value

        if (!currentState.isHost) return
        if (currentState.playerList.size < 2) return

        webSocketClient.sendGameStart()
    }

    fun onReadyToggle() {
        mutableIsReady.update { current -> !current }

        // Later:
        // webSocketClient.sendReadyToggle()
    }

    fun onLeaveLobby() {
        // Later:
        // webSocketClient.sendLeaveLobby()
    }

    class Factory(
        private val webSocketClient: WebSocketClient,
        private val sessionStateHolder: SessionStateHolder,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(LobbyScreenViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return LobbyScreenViewModel(
                webSocketClient = webSocketClient,
                sessionStateHolder = sessionStateHolder
            ) as T
        }
    }
}