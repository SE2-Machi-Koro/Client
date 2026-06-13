package com.machikoro.client.ui.game

import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.enums.ShopItemColor
import com.machikoro.client.domain.model.shop.ShopItem
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PurchaseState
import com.machikoro.client.domain.model.state.isShopItemAvailableFromMarketplace
import com.machikoro.client.domain.model.state.remainingMarketplaceQuantityFor
import com.machikoro.client.ui.game.ui.shouldShowBuyingPhaseShop
import org.junit.Assert.assertEquals
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

    @Test
    fun marketplaceQuantityOverridesStaleShopAvailability() {
        val item = shopItem("BAKERY", isAvailable = true)
        val state = buyingPhaseState(activePlayerId = 42, myUserId = 42).copy(
            marketplace = mapOf(CardType.BAKERY to 0)
        )

        assertEquals(0, state.remainingMarketplaceQuantityFor(item))
        assertFalse(state.isShopItemAvailableFromMarketplace(item))
    }

    @Test
    fun marketplaceQuantityIsShownForMatchingShopItem() {
        val item = shopItem("BAKERY", isAvailable = false)
        val state = buyingPhaseState(activePlayerId = 42, myUserId = 42).copy(
            marketplace = mapOf(CardType.BAKERY to 3)
        )

        assertEquals(3, state.remainingMarketplaceQuantityFor(item))
        assertTrue(state.isShopItemAvailableFromMarketplace(item))
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

    private fun shopItem(
        type: String,
        isAvailable: Boolean,
    ) = ShopItem(
        type = type,
        displayName = "Bakery",
        cost = 1,
        purchaseType = PurchaseType.ESTABLISHMENT,
        color = ShopItemColor.GREEN,
        establishmentType = "BREAD",
        activationNumbers = listOf(2, 3),
        effectText = "Get 1 coin from the bank on your turn.",
        imageKey = "card_bakery",
        isAvailable = isAvailable
    )
}
