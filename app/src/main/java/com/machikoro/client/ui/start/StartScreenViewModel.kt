package com.machikoro.client.ui.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.machikoro.client.model.state.StartScreenState
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
