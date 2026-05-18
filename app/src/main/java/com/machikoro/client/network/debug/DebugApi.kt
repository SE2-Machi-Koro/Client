package com.machikoro.client.network.debug

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface DebugApi {
    // Response body ignored — the server broadcasts LOBBY_JOINED via WebSocket instead
    @POST("/debug/fill-lobby")
    suspend fun fillLobby(@Body body: FillLobbyRequest): Response<Unit>

    // Removes all non-host players from the lobby
    @POST("/debug/reset-lobby")
    suspend fun resetLobby(@Body body: ResetLobbyRequest): Response<Unit>
}