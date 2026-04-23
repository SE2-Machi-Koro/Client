package com.machikoro.client.domain.enums

import org.junit.Assert.assertEquals
import org.junit.Test

class GamePhaseTest {
    @Test
    fun enumContainsAllServerTurnPhaseValuesPlusNone() {
        val names = GamePhase.entries.map { it.name }.toSet()
        assertEquals(
            setOf("NONE", "ROLL_DICE", "RESOLVE_EFFECTS", "BUY_OR_BUILD", "END_TURN"),
            names
        )
    }
}
