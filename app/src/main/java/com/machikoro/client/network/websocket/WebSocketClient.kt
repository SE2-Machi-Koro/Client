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

    // Holds the latest created lobby code received from the server.
    // Null if no lobby has been created yet.
    val lobbyCode: StateFlow<String?>

    // Holds the latest dice result received from the server.
    // Null if no dice have been rolled yet in the current turn.
    val diceResult: StateFlow<List<Int>?>

    fun connect()

    fun disconnect()

    fun sendCreateLobby()

    fun clearLobbyCode()

    fun sendGameStart()

    // Sends a ROLL_DICE STOMP frame. diceCount is 1 by default,
    // 2 once the player has built the Bahnhof and chooses two dice.
    fun rollDice(diceCount: Int = 1)
}