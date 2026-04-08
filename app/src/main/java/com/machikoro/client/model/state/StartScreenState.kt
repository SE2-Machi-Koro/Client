package com.machikoro.client.model.state

data class StartScreenState(
    val title: String,
    val connectionStatus: ConnectionStatus,
    val lobbyStatus: LobbyStatus
) {
    companion object {
        fun placeholder() = StartScreenState(
            title = "Machi Koro Client",
            connectionStatus = ConnectionStatus.IDLE,
            lobbyStatus = LobbyStatus.PLACEHOLDER
        )
    }
}
