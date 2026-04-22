package com.machikoro.client.network.websocket

import com.machikoro.client.domain.model.state.ConnectionStatus
import kotlinx.coroutines.flow.StateFlow

interface WebSocketClient {
    val connectionStatus: StateFlow<ConnectionStatus>

    fun connect()

    fun disconnect()
}
