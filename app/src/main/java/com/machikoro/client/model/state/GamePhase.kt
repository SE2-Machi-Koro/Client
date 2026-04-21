package com.machikoro.client.model.state

enum class GamePhase {
    NONE,
    ROLL_DICE,
    RESOLVE_EFFECTS,
    BUY_OR_BUILD,
    END_TURN
}

fun GamePhase.toDisplayText(): String = when (this) {
    GamePhase.NONE -> ""
    GamePhase.ROLL_DICE -> "Roll Dice"
    GamePhase.RESOLVE_EFFECTS -> "Resolve Effects"
    GamePhase.BUY_OR_BUILD -> "Buy or Build"
    GamePhase.END_TURN -> "End Turn"
}
