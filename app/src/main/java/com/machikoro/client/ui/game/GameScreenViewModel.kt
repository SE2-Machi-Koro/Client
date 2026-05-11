package com.machikoro.client.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.machikoro.client.domain.model.shop.PurchaseType
import com.machikoro.client.domain.model.shop.ShopCatalog
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PurchaseState
import com.machikoro.client.network.websocket.WebSocketClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameScreenViewModel(
    private val webSocketClient: WebSocketClient
) : ViewModel() {
    val state: StateFlow<GameScreenState>
        get() = mutableState.asStateFlow()

    private val mutableState = MutableStateFlow(GameScreenState.initial())

    init {
        viewModelScope.launch {
            webSocketClient.gameId.collect { gameId ->
                mutableState.update { current ->
                    current.copy(gameId = gameId)
                }
            }
        }
        viewModelScope.launch {
            webSocketClient.connectionStatus.collect { connectionStatus ->
                mutableState.update { current ->
                    current.copy(connectionStatus = connectionStatus)
                }
            }
        }
        viewModelScope.launch {
            webSocketClient.gamePhase.collect { gamePhase ->
                mutableState.update { current ->
                    current.copy(gamePhase = gamePhase)
                }
            }
        }
        viewModelScope.launch {
            webSocketClient.players.collect { players ->
                mutableState.update { current ->
                    current.copy(players = players)
                }
            }
        }
    }

    fun purchase(itemType: String) {
        val current = mutableState.value
        val gameId = current.gameId ?: return
        // Item type is the server enum name (for example BAKERY or TRAIN_STATION).
        val item = ShopCatalog.defaultItems.firstOrNull { it.type == itemType && it.isAvailable } ?: return
        if (!current.isBuyingPhase || current.purchaseState != PurchaseState.IDLE) return
        if (!current.players.any { it.isCurrentPlayer && it.isActivePlayer }) return

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
        private val webSocketClient: WebSocketClient
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(GameScreenViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return GameScreenViewModel(webSocketClient) as T
        }
    }
}
