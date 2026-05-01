package com.machikoro.client.domain.model.state

data class StartScreenState(
    val title: String,
    val connectionStatus: ConnectionStatus,
    val lobbyStatus: LobbyStatus,
    val playerList: List<String> = emptyList(),
    val maxPlayers: Int = 4,
    val isHost: Boolean = false,
    val loggedInAs: String? = null,
) {
    companion object {
        fun placeholder() = StartScreenState(
            title = "Machi Koro Client",
            connectionStatus = ConnectionStatus.IDLE,
            lobbyStatus = LobbyStatus.PLACEHOLDER,
            playerList = emptyList(),
            maxPlayers = 4,
            isHost = false,
            loggedInAs = null,
        )
    }
}
