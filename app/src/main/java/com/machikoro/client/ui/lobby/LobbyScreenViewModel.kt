package com.machikoro.client.ui.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.machikoro.client.domain.model.state.LobbyStatus
import com.machikoro.client.domain.model.state.LobbyScreenState
import com.machikoro.client.domain.session.SessionStateHolder
import com.machikoro.client.network.websocket.WebSocketClient
import com.machikoro.client.ui.navigation.AppRoute
import com.machikoro.client.ui.navigation.NavigationViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LobbyScreenViewModel(
    private val webSocketClient: WebSocketClient,
    private val sessionStateHolder: SessionStateHolder,
    private val navigationViewModel: NavigationViewModel,
) : ViewModel() {

    val state: StateFlow<LobbyScreenState>
        get() = mutableState.asStateFlow()

    private val mutableState = MutableStateFlow(
        LobbyScreenState.placeholder().copy(
            lobbyStatus = LobbyStatus.WAITING_FOR_PLAYERS
        )
    )

    init {
        observeSession()
        observePlayers()
        observeConnectionStatus()
        observeIsLobbyHost()
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

        navigationViewModel.navigateTo(AppRoute.Home)
    }

    class Factory(
        private val webSocketClient: WebSocketClient,
        private val sessionStateHolder: SessionStateHolder,
        private val navigationViewModel: NavigationViewModel
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(LobbyScreenViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return LobbyScreenViewModel(
                webSocketClient = webSocketClient,
                sessionStateHolder = sessionStateHolder,
                navigationViewModel = navigationViewModel
            ) as T
        }
    }
}