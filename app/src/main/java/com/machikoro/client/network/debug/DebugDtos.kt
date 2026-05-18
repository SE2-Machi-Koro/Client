package com.machikoro.client.network.debug

import kotlinx.serialization.Serializable

@Serializable
data class FillLobbyRequest(val lobbyCode: String)