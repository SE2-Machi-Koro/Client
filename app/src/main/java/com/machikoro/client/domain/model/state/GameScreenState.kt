package com.machikoro.client.domain.model.state

import com.machikoro.client.domain.enums.GamePhase

data class GameScreenState(
    val gamePhase: GamePhase,
    val connectionStatus: ConnectionStatus,
    val players: List<PlayerCoinState>
) {
    companion object {
        fun initial() = GameScreenState(
            gamePhase = GamePhase.NONE,
            connectionStatus = ConnectionStatus.IDLE,
            players = emptyList()
        )
    }
}
