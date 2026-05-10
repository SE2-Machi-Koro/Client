package com.machikoro.client.domain.model.state

import com.machikoro.client.domain.enums.GamePhase

data class GameScreenState(
    // Required by Server PurchaseRequest; populated from lobby/game WebSocket messages.
    val gameId: Int?,
    val gamePhase: GamePhase,
    val connectionStatus: ConnectionStatus,
    val players: List<PlayerCoinState>,
    val purchaseState: PurchaseState
) {
    // Keeps UI visibility tied to the existing phase stream from the server.
    val isBuyingPhase: Boolean
        get() = gamePhase == GamePhase.BUY_OR_BUILD

    companion object {
        fun initial() = GameScreenState(
            gameId = null,
            gamePhase = GamePhase.NONE,
            connectionStatus = ConnectionStatus.IDLE,
            players = emptyList(),
            purchaseState = PurchaseState.IDLE
        )
    }
}
