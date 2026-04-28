package com.machikoro.client.domain.model.state

import com.machikoro.client.domain.enums.GamePhase

data class GameScreenState(
    val gamePhase: GamePhase = GamePhase.NONE,
    val connectionStatus: ConnectionStatus = ConnectionStatus.IDLE
) {
    companion object {
        fun initial() = GameScreenState()
    }
}