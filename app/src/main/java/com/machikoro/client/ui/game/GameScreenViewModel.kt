package com.machikoro.client.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.model.shop.ShopCatalog
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
                mutableState.update { it.copy(gamePhase = gamePhase) }
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
                mutableState.update { it.copy(activePlayerId = activePlayerId) }
            }
        }
        viewModelScope.launch {
            webSocketClient.gameStatus.collect { gameStatus ->
                mutableState.update { it.copy(gameStatus = gameStatus) }
            }
        }
        viewModelScope.launch {
            webSocketClient.roundNumber.collect { roundNumber ->
                mutableState.update { it.copy(roundNumber = roundNumber) }
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
        if (!current.isBuyingPhase || current.purchaseState != PurchaseState.IDLE) return
        if (!current.isActivePlayer) return

        mutableState.update { state ->
            state.copy(purchaseState = PurchaseState.PENDING)
        }
        // TODO(#39): wait for backend success/error response before showing final feedback.
        webSocketClient.sendPurchase(
            gameId = gameId,
            purchaseType = item.purchaseType,
            cardType = item.type.takeIf { item.purchaseType == PurchaseType.ESTABLISHMENT },
            landmarkType = item.type.takeIf { item.purchaseType == PurchaseType.LANDMARK }
        )
        mutableState.update { state ->
            state.copy(purchaseState = PurchaseState.SUCCESS)
        }
    }

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
