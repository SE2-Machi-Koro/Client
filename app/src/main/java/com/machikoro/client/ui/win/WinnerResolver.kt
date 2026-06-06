package com.machikoro.client.ui.win

import com.machikoro.client.domain.model.state.GameScreenState

fun resolveWinnerName(state: GameScreenState): String {
    val winnerId = state.winnerId ?: return "Winner"

    return state.players
        .find { it.id == winnerId.toString() }
        ?.displayName
        ?: "Winner"
}

/**
 * Returns all players ranked for the end-of-game screen.
 * Rank 1 = winner (via winnerId).
 * Remaining players are sorted by number of built landmarks (descending).
 * Returns a list of Pair(displayName, builtLandmarkCount).
 */
fun resolveRankedPlayers(state: GameScreenState): List<Pair<String, Int>> {
    val winnerId = state.winnerId?.toString()

    val winner = state.players.find { it.id == winnerId }
    val others = state.players
        .filter { it.id != winnerId }
        .sortedByDescending { player ->
            state.playerLandmarks[player.id.toIntOrNull()]
                ?.count { it.isBuilt }
                ?: 0
        }

    val ranked = listOfNotNull(winner) + others

    return ranked.map { player ->
        val builtCount = state.playerLandmarks[player.id.toIntOrNull()]
            ?.count { it.isBuilt }
            ?: 0
        player.displayName to builtCount
    }
}