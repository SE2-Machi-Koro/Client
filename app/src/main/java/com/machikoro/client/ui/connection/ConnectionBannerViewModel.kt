package com.machikoro.client.ui.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.session.SessionStateHolder
import com.machikoro.client.network.websocket.WebSocketClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectionBannerViewModel(
    private val webSocketClient: WebSocketClient,
    private val sessionStateHolder: SessionStateHolder,
    private val reconnectedDisplayMs: Long = DEFAULT_RECONNECTED_DISPLAY_MS,
) : ViewModel() {

    private val mutableState = MutableStateFlow<ConnectionBannerState>(ConnectionBannerState.Hidden)
    val state: StateFlow<ConnectionBannerState> = mutableState.asStateFlow()

    // Tracks whether the user has seen a successful CONNECTED at least once in
    // the current session. Without this guard the initial CONNECTING after
    // login would falsely flash "Connection lost" before the first connect.
    private var hasEverConnectedThisSession = false
    private var recoveryJob: Job? = null

    init {
        viewModelScope.launch {
            webSocketClient.connectionStatus.collect { status -> onStatusChange(status) }
        }
        viewModelScope.launch {
            sessionStateHolder.session.collect { session ->
                if (session == null) {
                    recoveryJob?.cancel()
                    recoveryJob = null
                    hasEverConnectedThisSession = false
                    mutableState.value = ConnectionBannerState.Hidden
                }
            }
        }
    }

    private fun onStatusChange(status: ConnectionStatus) {
        if (status == ConnectionStatus.CONNECTED) {
            if (hasEverConnectedThisSession) {
                recoveryJob?.cancel()
                recoveryJob = viewModelScope.launch {
                    mutableState.value = ConnectionBannerState.Reconnected
                    delay(reconnectedDisplayMs)
                    if (mutableState.value is ConnectionBannerState.Reconnected) {
                        mutableState.value = ConnectionBannerState.Hidden
                    }
                }
            } else {
                mutableState.value = ConnectionBannerState.Hidden
            }
            hasEverConnectedThisSession = true
        } else {
            recoveryJob?.cancel()
            recoveryJob = null
            mutableState.value = if (hasEverConnectedThisSession) {
                ConnectionBannerState.Disconnected
            } else {
                ConnectionBannerState.Hidden
            }
        }
    }

    class Factory(
        private val webSocketClient: WebSocketClient,
        private val sessionStateHolder: SessionStateHolder,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ConnectionBannerViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return ConnectionBannerViewModel(webSocketClient, sessionStateHolder) as T
        }
    }

    companion object {
        private const val DEFAULT_RECONNECTED_DISPLAY_MS = 3_000L
    }
}
