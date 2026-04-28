package com.machikoro.client.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.network.websocket.DiceRollResult
import com.machikoro.client.network.websocket.WebSocketClient
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn

class GameViewModel(
    private val wsClient: WebSocketClient
) : ViewModel() {

    val connectionStatus: StateFlow<ConnectionStatus> = wsClient.connectionStatus
        .stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionStatus.IDLE)

    val diceResult: StateFlow<DiceRollResult?> = wsClient.diceResult
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _canRollTwo = MutableStateFlow(false)
    val canRollTwo: StateFlow<Boolean> = _canRollTwo

    fun rollDice(diceCount: Int) {
        wsClient.rollDice(
            playerId = "android-client",
            diceCount = diceCount
        )
    }

    fun unlockTwoDice() {
        _canRollTwo.value = true
    }

    override fun onCleared() {
        super.onCleared()
    }

    class Factory(
        private val webSocketClient: WebSocketClient
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(GameViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return GameViewModel(webSocketClient) as T
        }
    }
}