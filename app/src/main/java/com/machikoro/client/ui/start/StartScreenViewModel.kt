package com.machikoro.client.ui.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.machikoro.client.domain.model.state.StartScreenState
import com.machikoro.client.network.websocket.WebSocketClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StartScreenViewModel(
    private val webSocketClient: WebSocketClient
) : ViewModel() {
    val state: StateFlow<StartScreenState>
        get() = mutableState.asStateFlow()

    private val mutableState = MutableStateFlow(StartScreenState.placeholder())

    init {
        viewModelScope.launch {
            webSocketClient.connectionStatus.collect { connectionStatus ->
                mutableState.update { current ->
                    current.copy(connectionStatus = connectionStatus)
                }
            }
        }
        viewModelScope.launch {
            webSocketClient.diceResult.collect { diceResult ->
                diceResult?.let {
                    mutableState.update { current ->
                        current.copy(lastDiceRoll = it.total)
                    }
                }
            }
        }
    }

    fun rollDice(playerId: String, diceCount: Int = 1) {
        webSocketClient.rollDice(playerId, diceCount)
    }

    class Factory(
        private val webSocketClient: WebSocketClient
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(StartScreenViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return StartScreenViewModel(webSocketClient) as T
        }
    }
}