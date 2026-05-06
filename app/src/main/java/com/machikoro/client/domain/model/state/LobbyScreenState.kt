package com.machikoro.client.domain.model.state

data class LobbyScreenState(
    val connectionStatus: ConnectionStatus,
    val lobbyStatus: LobbyStatus,
    val playerList: List<String> = emptyList(),
    val maxPlayers: Int = 4,
    val isHost: Boolean = false,
    val isReady: Boolean = false,
    val loggedInAs: String? = null,
) {
    companion object {
        fun placeholder() = LobbyScreenState(
            connectionStatus = ConnectionStatus.IDLE,
            lobbyStatus = LobbyStatus.WAITING_FOR_PLAYERS,
            playerList = emptyList(),
            maxPlayers = 4,
            isHost = false,
            isReady = false,
            loggedInAs = null,
        )
    }
}