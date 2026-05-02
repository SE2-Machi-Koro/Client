package com.machikoro.client.network.websocket

import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.PlayerCoinState
import kotlinx.coroutines.flow.StateFlow

interface WebSocketClient {
    val connectionStatus: StateFlow<ConnectionStatus>

    val gamePhase: StateFlow<GamePhase>

    // Backend coin payload is still pending; expose the UI-ready state now for #37.
    val players: StateFlow<List<PlayerCoinState>>

    fun connect()

    fun disconnect()

    fun sendGameStart()
}
