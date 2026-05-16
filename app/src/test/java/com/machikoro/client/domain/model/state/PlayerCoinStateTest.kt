package com.machikoro.client.domain.model.state

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerCoinStateTest {
    @Test
    fun testDefaultValues() {
        val state = PlayerCoinState(id = "1", displayName = "Player", coins = 5)
        assertEquals("1", state.id)
        assertEquals("Player", state.displayName)
        assertEquals(5, state.coins)
        assertEquals(false, state.isCurrentPlayer)
        assertEquals(false, state.isActivePlayer)
    }

    @Test
    fun testCustomValues() {
        val state = PlayerCoinState(id = "2", displayName = "P2", coins = 10, isCurrentPlayer = true, isActivePlayer = true)
        assertEquals("2", state.id)
        assertEquals("P2", state.displayName)
        assertEquals(10, state.coins)
        assertEquals(true, state.isCurrentPlayer)
        assertEquals(true, state.isActivePlayer)
    }
}
