package com.machikoro.client.domain.model.state

import com.machikoro.client.domain.enums.GamePhase

fun GamePhase.toDisplayText(): String = when (this) {
    GamePhase.NONE -> ""
    GamePhase.ROLL_DICE -> "Roll Dice"
    GamePhase.RESOLVE_EFFECTS -> "Resolve Effects"
    GamePhase.BUY_OR_BUILD -> "Buy or Build"
    GamePhase.END_TURN -> "End Turn"
}
