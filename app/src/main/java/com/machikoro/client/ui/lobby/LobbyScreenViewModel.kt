package com.machikoro.client.ui.lobby

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.machikoro.client.domain.model.state.LobbyStatus
import com.machikoro.client.domain.model.state.LobbyScreenState
import com.machikoro.client.domain.session.SessionStateHolder
import com.machikoro.client.network.debug.DebugApi
import com.machikoro.client.network.debug.FillLobbyRequest
import com.machikoro.client.network.websocket.WebSocketClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LobbyScreenViewModel(
    private val webSocketClient: WebSocketClient,
    private val sessionStateHolder: SessionStateHolder,
    private val debugApi: DebugApi,
) : ViewModel() {

    val state: StateFlow<LobbyScreenState>
        get() = mutableState.asStateFlow()

    private val mutableState = MutableStateFlow(
        LobbyScreenState.placeholder().copy(
            lobbyStatus = LobbyStatus.WAITING_FOR_PLAYERS
        )
    )

    // Tracks the current lobby code so fillWithDummies() can use it without a parameter
    private var currentLobbyCode: String? = null

    init {
        observeSession()
        observePlayers()
        observeConnectionStatus()
        observeIsLobbyHost()
        observeLobbyCode()
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionStateHolder.session.collect { session ->
                mutableState.update { current ->
                    current.copy(loggedInAs = session?.username)
                }
            }
        }
    }

    private fun observePlayers() {
        viewModelScope.launch {
            webSocketClient.players.collect { players ->
                mutableState.update { current ->
                    val playerNames = players.map { it.displayName }

                    current.copy(
                        playerList = playerNames,
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

    private fun observeIsLobbyHost() {
        viewModelScope.launch {
            webSocketClient.isLobbyHost.collect { isHost ->
                mutableState.update { current ->
                    current.copy(isHost = isHost)
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
        mutableState.update { current ->
            current.copy(isReady = !current.isReady)
        }

        // TODO: send ready status to backend when supported.
        // webSocketClient.sendReadyToggle()
    }

    fun onLeaveLobby() {
        // TODO: notify backend when leave-lobby endpoint/event exists.
    }

    // Calls the debug endpoint to fill remaining lobby slots with dummy players
    fun fillWithDummies() {
        val code = currentLobbyCode ?: return
        viewModelScope.launch {
            try {
                debugApi.fillLobby(FillLobbyRequest(lobbyCode = code))
                Log.d("LobbyScreenViewModel", "fillWithDummies succeeded for lobby $code")
            } catch (e: Exception) {
                Log.e("LobbyScreenViewModel", "fillWithDummies failed: ${e.message}")
            }
        }
    }

    private fun observeLobbyCode() {
        viewModelScope.launch {
            webSocketClient.lobbyCode.collect { code ->
                currentLobbyCode = code
            }
        }
    }

    class Factory(
        private val webSocketClient: WebSocketClient,
        private val sessionStateHolder: SessionStateHolder,
        private val debugApi: DebugApi,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(LobbyScreenViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return LobbyScreenViewModel(
                webSocketClient = webSocketClient,
                sessionStateHolder = sessionStateHolder,
                debugApi = debugApi,
            ) as T
        }
    }
}