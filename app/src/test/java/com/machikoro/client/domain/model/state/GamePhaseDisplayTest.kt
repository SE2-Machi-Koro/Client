package com.machikoro.client.domain.model.state

import com.machikoro.client.domain.enums.GamePhase
import org.junit.Assert.assertEquals
import org.junit.Test

class GamePhaseDisplayTest {
    @Test
    fun displayTextMatchesExpectedLabels() {
        assertEquals("", GamePhase.NONE.toDisplayText())
        assertEquals("Roll Dice", GamePhase.ROLL_DICE.toDisplayText())
        assertEquals("Resolve Effects", GamePhase.RESOLVE_EFFECTS.toDisplayText())
        assertEquals("Buy or Build", GamePhase.BUY_OR_BUILD.toDisplayText())
        assertEquals("End Turn", GamePhase.END_TURN.toDisplayText())
    }
}
