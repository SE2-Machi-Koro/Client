package com.machikoro.client.network.websocket

import com.machikoro.client.model.state.ConnectionStatus
import kotlinx.coroutines.flow.StateFlow

interface WebSocketClient {
    val connectionStatus: StateFlow<ConnectionStatus>
    val diceResult: StateFlow<DiceRollResult?>

    fun connect()

    fun disconnect()

    fun rollDice(playerId: String, diceCount: Int)
}
