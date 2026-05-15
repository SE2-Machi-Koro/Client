package com.machikoro.client.network.websocket

import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface WebSocketClient {
    val connectionStatus: StateFlow<ConnectionStatus>

    val gamePhase: StateFlow<GamePhase>
    val players: StateFlow<List<PlayerCoinState>>
    val lobbyCode: StateFlow<String?>
    val activeGameId: StateFlow<Int?>
    val isLobbyHost: StateFlow<Boolean>

    // Holds the latest dice result received from the server.
    // Null if no dice have been rolled yet in the current turn.
    val diceResult: StateFlow<List<Int>?>
    val activePlayerId: StateFlow<Int?>

    // Reconnect snapshot fields, populated from the /app/game.sync response.
    // gameStatus is null until the first snapshot arrives.
    val gameStatus: StateFlow<GameStatus?>
    val roundNumber: StateFlow<Int?>
    // playerId -> that player's landmarks (built / unbuilt).
    val playerLandmarks: StateFlow<Map<Int, List<PlayerLandmarkState>>>
    // Remaining marketplace supply per card type.
    val marketplace: StateFlow<Map<CardType, Int>>

    // Fires when the server rejects the STOMP CONNECT for auth reasons (token
    // missing / invalid / server-side cleared). The UI layer is responsible for
    // calling SessionManager.signOut() and surfacing a "session expired"
    // message — kept out of the network client so transport and policy stay
    // separated.
    val authRejections: SharedFlow<Unit>

    fun connect()
    fun disconnect()

    fun sendCreateLobby()
    fun clearLobbyCode()
    fun sendGameStart()
    fun rollDice(diceCount: Int = 1)
}