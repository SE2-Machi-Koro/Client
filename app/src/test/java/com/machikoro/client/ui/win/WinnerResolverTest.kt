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

}
