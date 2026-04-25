package com.machikoro.client.domain.model.state

data class StartScreenState(
    val title: String,
    val connectionStatus: ConnectionStatus,
    val lobbyStatus: LobbyStatus,
    val lastDiceRoll: Int? = null
    val gamePhase: GamePhase = GamePhase.NONE
) {
    companion object {
        fun placeholder() = StartScreenState(
            title = "Machi Koro Client",
            connectionStatus = ConnectionStatus.IDLE,
            lobbyStatus = LobbyStatus.PLACEHOLDER
        )
    }
}