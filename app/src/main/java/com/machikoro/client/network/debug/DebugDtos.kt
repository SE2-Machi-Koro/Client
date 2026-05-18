package com.machikoro.client.network.debug

import kotlinx.serialization.Serializable

@Serializable
data class FillLobbyRequest(
    val lobbyCode: String,
    // Null means fill all remaining slots
    val count: Int? = null,
)

@Serializable
data class ResetLobbyRequest(val lobbyCode: String)