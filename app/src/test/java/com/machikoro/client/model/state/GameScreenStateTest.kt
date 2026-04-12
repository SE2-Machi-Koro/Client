package com.machikoro.client.model.state

import org.junit.Assert.assertEquals
import org.junit.Test

class GameScreenStateTest {
    @Test
    fun initialFactoryUsesDefaultValues() {
        val state = GameScreenState.initial()

        assertEquals(GamePhase.NONE, state.gamePhase)
        assertEquals(ConnectionStatus.IDLE, state.connectionStatus)
    }
}
