package com.machikoro.client.domain.model.state

import com.machikoro.client.domain.enums.GamePhase
import org.junit.Assert.assertEquals
import org.junit.Test

class GameScreenStateTest {
    @Test
    fun initialUsesNoneGamePhaseAndIdleConnectionStatus() {
        val state = GameScreenState.initial()

        assertEquals(GamePhase.NONE, state.gamePhase)
        assertEquals(ConnectionStatus.IDLE, state.connectionStatus)
    }
}
