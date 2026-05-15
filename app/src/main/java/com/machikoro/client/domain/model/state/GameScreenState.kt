package com.machikoro.client.domain.model.state

import com.machikoro.client.domain.enums.GamePhase

data class GameScreenState(
    val connectionStatus: ConnectionStatus,
    val gamePhase: GamePhase,
    val players: List<PlayerCoinState>,
    val diceResult: List<Int>? = null,
    val activePlayerId: Int? = null,
    val myUserId: Int? = null,
    val isRolling: Boolean = false,
) {
    val isActivePlayer: Boolean
        get() = myUserId != null && myUserId == activePlayerId

    companion object {
        fun initial() = GameScreenState(
            connectionStatus = ConnectionStatus.IDLE,
            gamePhase = GamePhase.NONE,
            players = emptyList(),
            diceResult = null,
            activePlayerId = null,
            myUserId = null,
            isRolling = false,
        )
    }
}