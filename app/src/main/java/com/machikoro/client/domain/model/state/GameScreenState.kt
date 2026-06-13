package com.machikoro.client.domain.model.state

import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.enums.ShopItemColor
import com.machikoro.client.domain.model.shop.ShopItem

data class GameScreenState(
    val gameId: Int?,
    val connectionStatus: ConnectionStatus,
    val gamePhase: GamePhase,
    val customDisplayText: String? = null,
    val players: List<PlayerCoinState>,
    val diceResult: List<Int>? = null,
    val activePlayerId: Int? = null,
    val winnerId: Int? = null,
    val myUserId: Int? = null,
    val purchaseState: PurchaseState,
    val selectedPurchaseItemType: String? = null,
    val pendingPurchaseItemType: String? = null,
    val purchaseFeedbackItemType: String? = null,
    val purchaseMessage: String? = null,
    val isRolling: Boolean = false,
    val rollingStartTime: Long? = null,
    val requestedDiceCount: Int = 1,
    val gameStatus: GameStatus? = null,
    val roundNumber: Int? = null,
    val playerCards: Map<Int, List<PlayerCardState>> = emptyMap(),
    val playerLandmarks: Map<Int, List<PlayerLandmarkState>> = emptyMap(),
    val marketplace: Map<CardType, Int> = emptyMap(),
    val shopItems: List<ShopItem> = emptyList(),
) {
    val isActivePlayer: Boolean
        get() = myUserId != null && myUserId == activePlayerId

    val isBuyingPhase: Boolean
        get() = gamePhase == GamePhase.BUY_OR_BUILD

    val hasTrainStation: Boolean
        get() = activePlayerHasBuiltLandmark(LandmarkType.TRAIN_STATION)

    // Radio Tower (#326) unlocks the once-per-turn reroll during RESOLVE_EFFECTS.
    val hasRadioTower: Boolean
        get() = activePlayerHasBuiltLandmark(LandmarkType.RADIO_TOWER)

    /**
     * Radio Tower reroll gate (#326): the active player may reroll the dice
     * during RESOLVE_EFFECTS once they have built a Radio Tower and a roll
     * already exists this turn. The server stays authoritative for the
     * once-per-turn rule; this only drives the client action and its button.
     */
    val canReroll: Boolean
        get() = gameStatus == GameStatus.IN_PROGRESS &&
            gamePhase == GamePhase.RESOLVE_EFFECTS &&
            isActivePlayer &&
            hasRadioTower &&
            diceResult != null

    private fun activePlayerHasBuiltLandmark(landmarkType: LandmarkType): Boolean {
        val activePlayerDatabaseId = players.firstOrNull { it.isActivePlayer }?.id?.toIntOrNull()
            ?: return false
        return playerLandmarks[activePlayerDatabaseId].orEmpty().any {
            it.landmarkType == landmarkType && it.isBuilt
        }
    }

    val hasRadioTower: Boolean
        get() {
            val activePlayerDatabaseId = players.firstOrNull { it.isActivePlayer }?.id?.toIntOrNull()
                ?: return false
            return playerLandmarks[activePlayerDatabaseId].orEmpty().any {
                it.landmarkType == LandmarkType.RADIO_TOWER && it.isBuilt
            }
        }

    companion object {
        fun initial() = GameScreenState(
            gameId = null,
            connectionStatus = ConnectionStatus.IDLE,
            gamePhase = GamePhase.NONE,
            customDisplayText = null,
            players = emptyList(),
            diceResult = null,
            activePlayerId = null,
            myUserId = null,
            purchaseState = PurchaseState.IDLE,
            selectedPurchaseItemType = null,
            pendingPurchaseItemType = null,
            purchaseFeedbackItemType = null,
            purchaseMessage = null,
            isRolling = false,
            rollingStartTime = null,
            requestedDiceCount = 1,
            gameStatus = null,
            roundNumber = null,
            playerCards = emptyMap(),
            playerLandmarks = emptyMap(),
            marketplace = emptyMap(),
            shopItems = emptyList(),
        )
    }
}

fun GameScreenState.remainingMarketplaceQuantityFor(item: ShopItem): Int? {
    if (item.purchaseType != PurchaseType.ESTABLISHMENT || marketplace.isEmpty()) return null
    val cardType = runCatching { CardType.valueOf(item.type) }.getOrNull() ?: return null
    return marketplace[cardType] ?: 0
}

fun GameScreenState.isShopItemAvailableFromMarketplace(item: ShopItem): Boolean =
    remainingMarketplaceQuantityFor(item)?.let { it > 0 } ?: item.isAvailable

fun GameScreenState.isAlreadyOwnedPurpleEstablishment(item: ShopItem): Boolean {
    if (item.purchaseType != PurchaseType.ESTABLISHMENT || item.color != ShopItemColor.PURPLE) {
        return false
    }
    val activePlayerId = players.firstOrNull { it.isActivePlayer }?.id?.toIntOrNull() ?: return false
    val cardType = runCatching { CardType.valueOf(item.type) }.getOrNull() ?: return false
    return playerCards[activePlayerId].orEmpty().any { it.cardType == cardType && it.quantity > 0 }
}
