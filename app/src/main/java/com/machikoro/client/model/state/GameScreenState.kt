package com.machikoro.client.model.state

data class GameScreenState(
    val gamePhase: GamePhase,
    val connectionStatus: ConnectionStatus
) {
    companion object {
        fun initial() = GameScreenState(
            gamePhase = GamePhase.NONE,
            connectionStatus = ConnectionStatus.IDLE
        )
    }
}
