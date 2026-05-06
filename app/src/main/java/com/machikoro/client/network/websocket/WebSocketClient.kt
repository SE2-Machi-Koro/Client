package com.machikoro.client.network.websocket

import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.PlayerCoinState
import kotlinx.coroutines.flow.StateFlow

interface WebSocketClient {
    val connectionStatus: StateFlow<ConnectionStatus>
    val diceResult: StateFlow<DiceRollResult?>
    val gamePhase: StateFlow<GamePhase>
    val players: StateFlow<List<PlayerCoinState>>
    val lobbyCode: StateFlow<String?>

    fun connect()
    fun disconnect()
    fun rollDice(playerId: String, diceCount: Int)
    fun sendCreateLobby()
    fun sendGameStart()
}
