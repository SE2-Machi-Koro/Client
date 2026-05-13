package com.machikoro.client.network.websocket

import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.PlayerCoinState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface WebSocketClient {
    val connectionStatus: StateFlow<ConnectionStatus>

    val gamePhase: StateFlow<GamePhase>
    val players: StateFlow<List<PlayerCoinState>>
    val lobbyCode: StateFlow<String?>

    fun connect()
    fun disconnect()

    fun sendCreateLobby()
    fun clearLobbyCode()
    fun sendGameStart()

    // Sends a ROLL_DICE STOMP frame. diceCount is 1 by default,
    // 2 once the player has built the Bahnhof and chooses two dice.
    fun rollDice(diceCount: Int = 1)
}