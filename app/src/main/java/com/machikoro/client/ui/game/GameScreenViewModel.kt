package com.machikoro.client.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.machikoro.client.domain.cheat.recommendBestBuy
import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.model.shop.ShopCatalog
import com.machikoro.client.domain.model.shop.ShopItem
import com.machikoro.client.domain.model.shop.PurchaseEvent
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PurchaseState
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.session.SessionStateHolder
import com.machikoro.client.network.debug.DebugApi
import com.machikoro.client.network.debug.EndGameRequest
import com.machikoro.client.network.websocket.WebSocketClient
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameScreenViewModel(
    private val webSocketClient: WebSocketClient,
    private val sessionStateHolder: SessionStateHolder,
    private val debugApi: DebugApi,
) : ViewModel() {
    val state: StateFlow<GameScreenState>
        get() = mutableState.asStateFlow()

    private val mutableState = MutableStateFlow(GameScreenState.initial())

    /** Insider Trading cheat (#203): the recommended card for this turn, or null when inactive. */
    val cheatRecommendation: StateFlow<CardType?>
        get() = mutableCheatRecommendation.asStateFlow()
    private val mutableCheatRecommendation = MutableStateFlow<CardType?>(null)

    /** One-shot signal each time a shake produces a recommendation (drives the toast). */
    val cheatActivations: SharedFlow<CardType?>
        get() = mutableCheatActivations.asSharedFlow()
    private val mutableCheatActivations = MutableSharedFlow<CardType?>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Debug End-game (#191): one-shot failure message, drives a snackbar. */
    val debugEndGameErrors: SharedFlow<String>
        get() = mutableDebugEndGameErrors.asSharedFlow()
    private val mutableDebugEndGameErrors = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

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
                // The Insider Trading cheat is valid for one turn only — drop it when the turn rotates.
                if (mutableState.value.activePlayerId != activePlayerId) {
                    mutableCheatRecommendation.value = null
                }
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
            webSocketClient.winnerId.collect { winnerId ->
                mutableState.update { it.copy(winnerId = winnerId) }
            }
        }
        viewModelScope.launch {
            webSocketClient.roundNumber.collect { roundNumber ->
                if (mutableState.value.roundNumber != roundNumber) {
                    mutableCheatRecommendation.value = null
                }
                mutableState.update { state ->
                    state.copy(roundNumber = roundNumber)
                        .resetPurchaseFeedbackIf(state.roundNumber != roundNumber)
                }
            }
        }
        viewModelScope.launch {
            webSocketClient.playerCards.collect { playerCards ->
                mutableState.update { it.copy(playerCards = playerCards) }
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
        if (mutableState.value.gameStatus != GameStatus.IN_PROGRESS) return
        if (mutableState.value.gamePhase != GamePhase.ROLL_DICE) return
        if (!mutableState.value.isActivePlayer) return
        mutableState.update { it.copy(isRolling = true) }
        webSocketClient.rollDice(diceCount)
    }

    /**
     * Insider Trading cheat (#203). On a shake during the local player's turn,
     * computes a local best-buy recommendation and emits a one-shot activation
     * signal for the toast. No-op off-turn or before the game is running; never
     * contacts the server.
     */
    fun onShake() {
        val current = mutableState.value
        if (current.gameStatus != GameStatus.IN_PROGRESS || !current.isActivePlayer) return
        val me = current.players.firstOrNull { it.isCurrentPlayer } ?: return
        val recommendation = recommendBestBuy(current, me)
        mutableCheatRecommendation.value = recommendation
        mutableCheatActivations.tryEmit(recommendation)
    }

    fun performTurnFlowAction() {
        val current = mutableState.value
        val gameId = current.gameId ?: return
        if (current.gameStatus != GameStatus.IN_PROGRESS) return
        if (!current.isActivePlayer) return

        when (current.gamePhase) {
            GamePhase.RESOLVE_EFFECTS -> webSocketClient.resolveEffects(gameId)
            GamePhase.BUY_OR_BUILD -> webSocketClient.endTurn(gameId)
            GamePhase.NONE,
            GamePhase.ROLL_DICE,
            GamePhase.END_TURN -> Unit
        }
    }

    fun clearGameState() {
        webSocketClient.clearGameState()
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
        gameStatus == GameStatus.IN_PROGRESS &&
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

    /**
     * Debug-only (#191): force-ends the current game with the local player as
     * winner via the admin debug endpoint. On success the server's GAME_END
     * broadcast drives the winner screen, so there is nothing else to do here;
     * failures surface through [debugEndGameErrors] as a snackbar.
     */
    // Guards against rapid double-taps queuing concurrent end-game calls (#191 review).
    private var endGameInFlight = false

    fun endGame() {
        val gameId = mutableState.value.gameId ?: return
        if (endGameInFlight) return
        endGameInFlight = true
        viewModelScope.launch {
            try {
                val response = debugApi.endGame(EndGameRequest(gameId))
                if (!response.isSuccessful) {
                    mutableDebugEndGameErrors.tryEmit("End game failed (${response.code()})")
                }
            } catch (e: Exception) {
                mutableDebugEndGameErrors.tryEmit("End game error: ${e.message ?: "unknown error"}")
            } finally {
                endGameInFlight = false
            }
        }
    }

    private fun String.toDisplayName(): String =
        lowercase()
            .split("_")
            .joinToString(" ") { part -> part.replaceFirstChar { it.titlecase() } }

    class Factory(
        private val webSocketClient: WebSocketClient,
        private val sessionStateHolder: SessionStateHolder,
        private val debugApi: DebugApi,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(GameScreenViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return GameScreenViewModel(webSocketClient, sessionStateHolder, debugApi) as T
        }
    }
}
