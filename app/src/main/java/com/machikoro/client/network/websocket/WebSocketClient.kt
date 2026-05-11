package com.machikoro.client.network.websocket

import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.PlayerCoinState
import kotlinx.coroutines.flow.StateFlow

interface WebSocketClient {
    val connectionStatus: StateFlow<ConnectionStatus>
    val gamePhase: StateFlow<GamePhase>
    val players: StateFlow<List<PlayerCoinState>>
    val lobbyCode: StateFlow<String?>
    val diceResult: StateFlow<List<Int>?>
    val activePlayerId: StateFlow<Int?>

    fun connect()
    fun disconnect()
    fun sendCreateLobby()
    fun clearLobbyCode()
    fun sendGameStart()
    fun rollDice(diceCount: Int = 1)
}