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
        assertEquals(emptyList<PlayerCoinState>(), state.players)
        assertEquals(null, state.gameId)
        assertEquals(null, state.diceResult)
        assertEquals(null, state.activePlayerId)
        assertEquals(null, state.myUserId)
        assertEquals(false, state.isActivePlayer)
        assertEquals(PurchaseState.IDLE, state.purchaseState)
        assertEquals(null, state.pendingPurchaseItemType)
        assertEquals(null, state.purchaseMessage)
        assertEquals(false, state.isBuyingPhase)
    }

    @Test
    fun buyOrBuildPhaseIsBuyingPhase() {
        val state = GameScreenState.initial().copy(gamePhase = GamePhase.BUY_OR_BUILD)

        assertEquals(true, state.isBuyingPhase)
    }
}
