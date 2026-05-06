package com.machikoro.client.domain.model.state

import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.network.websocket.DiceRollResult

data class GameScreenState(
    val gamePhase: GamePhase,
    val connectionStatus: ConnectionStatus,
    val players: List<PlayerCoinState>,
    val diceResult: DiceRollResult? = null
) {
    companion object {
        fun initial() = GameScreenState(
            gamePhase = GamePhase.NONE,
            connectionStatus = ConnectionStatus.IDLE,
            players = emptyList(),
            diceResult = null
        )
    }
}