package com.machikoro.client.network.websocket

import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerCoinState
import kotlinx.coroutines.flow.StateFlow

interface WebSocketClient {
    val connectionStatus: StateFlow<ConnectionStatus>
    val diceResult: StateFlow<DiceRollResult?>

    val gamePhase: StateFlow<GamePhase>

    // Backend coin payload is still pending; expose the UI-ready state now for #37.
    val players: StateFlow<List<PlayerCoinState>>

    // Holds the latest created lobby code received from the server.
    // Null if no lobby has been created yet.
    val lobbyCode: StateFlow<String?>

    fun connect()

    fun disconnect()

    fun rollDice(playerId: String, diceCount: Int)

    fun sendCreateLobby()
    fun sendGameStart()
}
