package com.machikoro.client.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.model.shop.ShopCatalog
import com.machikoro.client.domain.model.shop.ShopItem
import com.machikoro.client.domain.model.shop.PurchaseEvent
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PurchaseState
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.session.SessionStateHolder
import com.machikoro.client.network.websocket.WebSocketClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameScreenViewModel(
    private val webSocketClient: WebSocketClient,
    private val sessionStateHolder: SessionStateHolder,
) : ViewModel() {
    val state: StateFlow<GameScreenState>
        get() = mutableState.asStateFlow()

    private val mutableState = MutableStateFlow(GameScreenState.initial())

    init {
        viewModelScope.launch {
            webSocketClient.activeGameId.collect { gameId ->
                mutableState.update { current ->
                    current.copy(gameId = gameId)
                }
            }
        }
        viewModelScope.launch {
            webSocketClient.connectionStatus.collect { connectionStatus ->
                mutableState.update { it.copy(connectionStatus = connectionStatus) }
            }
        }
        viewModelScope.launch {
            webSocketClient.gamePhase.collect { gamePhase ->
                mutableState.update { state ->
                    state.copy(gamePhase = gamePhase)
                        .resetPurchaseFeedbackIf(gamePhase != GamePhase.BUY_OR_BUILD)
                }
            }
        }
        viewModelScope.launch {
            webSocketClient.players.collect { players ->
                mutableState.update { it.copy(players = players) }
            }
        }
        viewModelScope.launch {
            webSocketClient.diceResult.collect { diceResult ->
                mutableState.update { it.copy(diceResult = diceResult, isRolling = false) }
            }
        }
        viewModelScope.launch {
            webSocketClient.activePlayerId.collect { activePlayerId ->
                mutableState.update { state ->
                    state.copy(activePlayerId = activePlayerId)
                        .resetPurchaseFeedbackIf(state.activePlayerId != activePlayerId)
                }
            }
        }
        viewModelScope.launch {
            webSocketClient.gameStatus.collect { gameStatus ->
                mutableState.update { it.copy(gameStatus = gameStatus) }
            }
        }
        viewModelScope.launch {
            webSocketClient.roundNumber.collect { roundNumber ->
                mutableState.update { state ->
                    state.copy(roundNumber = roundNumber)
                        .resetPurchaseFeedbackIf(state.roundNumber != roundNumber)
                }
            }
        }
        viewModelScope.launch {
            webSocketClient.playerLandmarks.collect { playerLandmarks ->
                mutableState.update { it.copy(playerLandmarks = playerLandmarks) }
            }
        }
        viewModelScope.launch {
            webSocketClient.marketplace.collect { marketplace ->
                mutableState.update { it.copy(marketplace = marketplace) }
            }
        }
        viewModelScope.launch {
            webSocketClient.shopItems.collect { shopItems ->
                mutableState.update { it.copy(shopItems = shopItems) }
            }
        }
        viewModelScope.launch {
            webSocketClient.purchaseEvents.collect { event ->
                mutableState.update { state -> state.applyPurchaseEvent(event) }
            }
        }
        viewModelScope.launch {
            sessionStateHolder.session.collect { session ->
                mutableState.update { it.copy(myUserId = session?.userId) }
            }
        }
    }

    fun rollDice(diceCount: Int = 1) {
        if (mutableState.value.gamePhase != GamePhase.ROLL_DICE) return
        if (!mutableState.value.isActivePlayer) return
        mutableState.update { it.copy(isRolling = true) }
        webSocketClient.rollDice(diceCount)
    }

    fun purchase(itemType: String) {
        val current = mutableState.value
        val gameId = current.gameId ?: return
        val availableItems = current.shopItems.ifEmpty { ShopCatalog.defaultItems }
        val item = availableItems.firstOrNull { it.type == itemType && it.isAvailable } ?: return
        if (!current.canStartPurchase(item)) return

        mutableState.update { state ->
            state.copy(
                purchaseState = PurchaseState.PENDING,
                pendingPurchaseItemType = item.type,
                purchaseFeedbackItemType = item.type,
                purchaseMessage = "Buying ${item.displayName}..."
            )
        }
        webSocketClient.sendPurchase(
            gameId = gameId,
            purchaseType = item.purchaseType,
            cardType = item.type.takeIf { item.purchaseType == PurchaseType.ESTABLISHMENT },
            landmarkType = item.type.takeIf { item.purchaseType == PurchaseType.LANDMARK }
        )
    }

    private fun GameScreenState.canStartPurchase(item: ShopItem): Boolean =
        isBuyingPhase &&
            isActivePlayer &&
            purchaseState != PurchaseState.PENDING &&
            purchaseState != PurchaseState.SUCCESS &&
            item.isAvailable &&
            hasEnoughKnownCoinsFor(item) &&
            !isKnownBuiltLandmark(item)

    private fun GameScreenState.hasEnoughKnownCoinsFor(item: ShopItem): Boolean {
        val activePlayerCoins = players.firstOrNull { it.isActivePlayer }?.coins
        return activePlayerCoins == null || activePlayerCoins >= item.cost
    }

    private fun GameScreenState.isKnownBuiltLandmark(item: ShopItem): Boolean {
        if (item.purchaseType != PurchaseType.LANDMARK) return false
        val activePlayerId = players.firstOrNull { it.isActivePlayer }?.id?.toIntOrNull() ?: return false
        val landmarkType = runCatching { LandmarkType.valueOf(item.type) }.getOrNull() ?: return false
        return playerLandmarks[activePlayerId].orEmpty().any {
            it.landmarkType == landmarkType && it.isBuilt
        }
    }

    private fun GameScreenState.applyPurchaseEvent(event: PurchaseEvent): GameScreenState =
        when (event) {
            is PurchaseEvent.Success -> {
                // Only finish the local pending action when the server confirms the same target.
                val matchesPending = pendingPurchaseItemType == event.itemType
                if (!matchesPending) {
                    this
                } else {
                    copy(
                        purchaseState = PurchaseState.SUCCESS,
                        pendingPurchaseItemType = null,
                        purchaseFeedbackItemType = event.itemType,
                        purchaseMessage = "${event.itemType.toDisplayName()} bought"
                    )
                }
            }
            is PurchaseEvent.Failure -> {
                // Failed purchases are retryable; the backend stays authoritative for the reason.
                if (purchaseState != PurchaseState.PENDING) {
                    this
                } else {
                    copy(
                        purchaseState = PurchaseState.ERROR,
                        pendingPurchaseItemType = null,
                        purchaseFeedbackItemType = purchaseFeedbackItemType,
                        purchaseMessage = event.message.ifBlank { "Purchase failed" }
                    )
                }
            }
        }

    private fun GameScreenState.resetPurchaseFeedbackIf(shouldReset: Boolean): GameScreenState =
        if (!shouldReset) {
            this
        } else {
            copy(
                purchaseState = PurchaseState.IDLE,
                pendingPurchaseItemType = null,
                purchaseFeedbackItemType = null,
                purchaseMessage = null
            )
        }

    private fun String.toDisplayName(): String =
        lowercase()
            .split("_")
            .joinToString(" ") { part -> part.replaceFirstChar { it.titlecase() } }

    class Factory(
        private val webSocketClient: WebSocketClient,
        private val sessionStateHolder: SessionStateHolder,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(GameScreenViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return GameScreenViewModel(webSocketClient, sessionStateHolder) as T
        }
    }
}
