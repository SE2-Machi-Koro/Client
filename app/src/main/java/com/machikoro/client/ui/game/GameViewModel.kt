package com.machikoro.client.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.machikoro.client.model.state.ConnectionStatus
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
        wsClient.disconnect()
    }
}