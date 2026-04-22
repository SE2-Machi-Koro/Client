package com.machikoro.client.network.websocket

import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.ConnectionStatus
import kotlinx.coroutines.flow.StateFlow

interface WebSocketClient {
    val connectionStatus: StateFlow<ConnectionStatus>

    val gamePhase: StateFlow<GamePhase>

    fun connect()

    fun disconnect()
}
