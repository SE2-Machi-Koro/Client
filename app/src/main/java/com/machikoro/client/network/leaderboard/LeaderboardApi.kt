package com.machikoro.client.network.leaderboard

import retrofit2.http.GET
import retrofit2.http.Query

interface LeaderboardApi {
    // Top-N players ordered by wins; server caps at 100
    @GET("/leaderboard")
    suspend fun getLeaderboard(@Query("limit") limit: Int = 50): List<LeaderboardEntry>
}