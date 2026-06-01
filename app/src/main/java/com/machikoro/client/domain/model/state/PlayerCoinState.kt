package com.machikoro.client.domain.model.state

data class PlayerCoinState(
    val id: String,
    val displayName: String,
    val coins: Int,
    val isCurrentPlayer: Boolean = false,
    val isActivePlayer: Boolean = false,
    // Populated once the backend includes isReady in LobbyRosterPlayerDto
    val isReady: Boolean = false,
)
