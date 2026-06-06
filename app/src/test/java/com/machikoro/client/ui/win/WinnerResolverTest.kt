package com.machikoro.client.ui.win

import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PlayerCoinState
import junit.framework.TestCase.assertEquals
import org.junit.Test

class WinnerResolverTest {

    @Test
    fun resolveWinnerNameReturnsMatchingPlayerName() {
        val state = GameScreenState.initial().copy(
            winnerId = 11,
            players = listOf(
                PlayerCoinState(
                    id = "11",
                    displayName = "Alice",
                    coins = 5
                )
            )
        )

        assertEquals(
            "Alice",
            resolveWinnerName(state)
        )
    }

    @Test
    fun resolveWinnerNameReturnsDefaultWhenNoWinnerIdSet() {
        val state = GameScreenState.initial().copy(
            winnerId = null,
            players = listOf(
                PlayerCoinState(id = "1", displayName = "Alice", coins = 5)
            )
        )

        assertEquals("Winner", resolveWinnerName(state))
    }

    @Test
    fun resolveWinnerNameReturnsDefaultWhenWinnerNotInPlayerList() {
        val state = GameScreenState.initial().copy(
            winnerId = 99,
            players = listOf(
                PlayerCoinState(id = "1", displayName = "Alice", coins = 5)
            )
        )

        assertEquals("Winner", resolveWinnerName(state))
    }

    @Test
    fun resolveWinnerNameReturnsCorrectPlayerFromMultiplePlayers() {
        val state = GameScreenState.initial().copy(
            winnerId = 2,
            players = listOf(
                PlayerCoinState(id = "1", displayName = "Alice", coins = 5),
                PlayerCoinState(id = "2", displayName = "Bob", coins = 10),
                PlayerCoinState(id = "3", displayName = "Charlie", coins = 3),
            )
        )

        assertEquals("Bob", resolveWinnerName(state))
    }
}