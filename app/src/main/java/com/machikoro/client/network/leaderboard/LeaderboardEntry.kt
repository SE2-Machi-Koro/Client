package com.machikoro.client.network.leaderboard

import kotlinx.serialization.Serializable

// Matches LeaderboardEntryDto from the server
@Serializable
data class LeaderboardEntry(
    val rank: Int,
    val userId: Int,
    val username: String,
    val totalWins: Int,
    val totalGamesPlayed: Int,
)