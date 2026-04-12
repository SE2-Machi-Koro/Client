package com.machikoro.client.model.state

import org.junit.Assert.assertEquals
import org.junit.Test

class GamePhaseTest {
    @Test
    fun displayTextMatchesExpectedLabels() {
        assertEquals("", GamePhase.NONE.toDisplayText())
        assertEquals("Roll Dice", GamePhase.ROLL_DICE.toDisplayText())
        assertEquals("Resolve Effects", GamePhase.RESOLVE_EFFECTS.toDisplayText())
        assertEquals("Buy or Build", GamePhase.BUY_OR_BUILD.toDisplayText())
        assertEquals("End Turn", GamePhase.END_TURN.toDisplayText())
    }
}
