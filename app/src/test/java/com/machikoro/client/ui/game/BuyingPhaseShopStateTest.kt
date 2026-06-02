package com.machikoro.client.ui.game

import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PurchaseState
import com.machikoro.client.ui.game.ui.shouldShowBuyingPhaseShop
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuyingPhaseShopStateTest {
    @Test
    fun activePlayerCanSeeShopDuringBuyOrBuildPhase() {
        val state = buyingPhaseState(activePlayerId = 42, myUserId = 42)

        assertTrue(state.shouldShowBuyingPhaseShop())
    }

    @Test
    fun waitingPlayerDoesNotSeeShopDuringBuyOrBuildPhase() {
        val state = buyingPhaseState(activePlayerId = 7, myUserId = 42)

        assertFalse(state.shouldShowBuyingPhaseShop())
    }

    @Test
    fun activePlayerDoesNotSeeShopWithoutGameId() {
        val state = buyingPhaseState(activePlayerId = 42, myUserId = 42).copy(gameId = null)

        assertFalse(state.shouldShowBuyingPhaseShop())
    }

    private fun buyingPhaseState(
        activePlayerId: Int,
        myUserId: Int,
    ) = GameScreenState(
        gameId = 1,
        connectionStatus = ConnectionStatus.CONNECTED,
        gamePhase = GamePhase.BUY_OR_BUILD,
        players = listOf(
            PlayerCoinState(
                id = activePlayerId.toString(),
                displayName = "Active player",
                coins = 6,
                isActivePlayer = true,
                isCurrentPlayer = activePlayerId == myUserId
            )
        ),
        activePlayerId = activePlayerId,
        myUserId = myUserId,
        purchaseState = PurchaseState.IDLE,
        gameStatus = GameStatus.IN_PROGRESS
    )
}
