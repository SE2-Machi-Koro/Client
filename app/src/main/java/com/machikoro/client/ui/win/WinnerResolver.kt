package com.machikoro.client.ui.win

import com.machikoro.client.domain.model.state.GameScreenState

fun resolveWinnerName(state: GameScreenState): String {
    val winnerId = state.winnerId ?: return "Winner"

    return state.players
        .find { it.id == winnerId.toString() }
        ?.displayName
        ?: "Winner"
}