package com.machikoro.client.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.network.websocket.WebSocketClient
import com.machikoro.client.network.websocket.WebSocketContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameScreenViewModel(
    private val webSocketClient: WebSocketClient
) : ViewModel() {

    val state: StateFlow<GameScreenState>
        get() = mutableState.asStateFlow()

    private val mutableState = MutableStateFlow(GameScreenState.initial())

    init {
        viewModelScope.launch {
            webSocketClient.connectionStatus.collect { connectionStatus ->
                mutableState.update { it.copy(connectionStatus = connectionStatus) }
            }
        }
        viewModelScope.launch {
            webSocketClient.gamePhase.collect { gamePhase ->
                mutableState.update { it.copy(gamePhase = gamePhase) }
            }
        }
        viewModelScope.launch {
            webSocketClient.players.collect { players ->
                mutableState.update { it.copy(players = players) }
            }
        }
        viewModelScope.launch {
            webSocketClient.diceResult.collect { diceResult ->
                mutableState.update { it.copy(diceResult = diceResult) }
            }
        }
    }

    fun rollDice(diceCount: Int) {
        webSocketClient.rollDice(
            playerId = WebSocketContract.defaultSender,
            diceCount = diceCount
        )
    }

    class Factory(
        private val webSocketClient: WebSocketClient
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(GameScreenViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return GameScreenViewModel(webSocketClient) as T
        }
    }
}