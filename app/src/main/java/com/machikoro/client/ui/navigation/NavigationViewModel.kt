package com.machikoro.client.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.StartScreenState

class NavigationViewModel : ViewModel() {

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>(extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    fun navigateTo(
        route: AppRoute,
        arguments: AppRoute.AppRouteArguments = AppRoute.AppRouteArguments(),
    ) {
        _navigationEvent.tryEmit(NavigationEvent.NavigateTo(route, arguments))
    }

    /**
     * Updates navigation based on app state changes (game status, login state, lobby state).
     * This centralizes all state-based routing logic that was previously in AppRoot.
     */
    fun updateNavigationBasedOnState(
        gameScreenState: GameScreenState,
        startScreenState: StartScreenState,
        lobbyCode: String?,
        showLobbyScreen: Boolean,
        loggedInAs: String?
    ) {
        viewModelScope.launch {
            // Determine target route based on current app state
            val targetRoute = when {
                gameScreenState.gameStatus == GameStatus.FINISHED -> AppRoute.Winner
                gameScreenState.gamePhase != GamePhase.NONE -> AppRoute.Game
                showLobbyScreen -> AppRoute.Lobby
                loggedInAs != null -> AppRoute.Home
                startScreenState.loggedInAs != null -> AppRoute.Home
                else -> AppRoute.Main
            }

            val routeArguments = AppRoute.AppRouteArguments(
                lobbyCode = lobbyCode,
                gameId = gameScreenState.gameId,
            )

            navigateTo(targetRoute, routeArguments)
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NavigationViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return NavigationViewModel() as T
        }
    }
}

sealed class NavigationEvent {
    data class NavigateTo(
        val route: AppRoute,
        val arguments: AppRoute.AppRouteArguments = AppRoute.AppRouteArguments(),
    ) : NavigationEvent()
}