package com.machikoro.client.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.StartScreenState

data class NavigationUiState(
    val showLobbyScreen: Boolean = false,
)

class NavigationViewModel : ViewModel() {

    private val mutableUiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = mutableUiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>(extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()
    // Track last emitted navigation to avoid emitting duplicate navigation events
    // which can cause unnecessary navigation attempts and UI churn.
    private var lastNavigation: Pair<AppRoute, AppRoute.AppRouteArguments>? = null

    fun showLobby() {
        mutableUiState.update { it.copy(showLobbyScreen = true) }
    }

    fun leaveLobby() {
        mutableUiState.update { it.copy(showLobbyScreen = false) }
    }

    fun navigateTo(
        route: AppRoute,
        arguments: AppRoute.AppRouteArguments = AppRoute.AppRouteArguments(),
    ) {
        val next = route to arguments
        if (lastNavigation == next) return
        lastNavigation = next
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
    ) {
        viewModelScope.launch {
            // Determine target route based on current app state
            val targetRoute = when {
                gameScreenState.gameStatus == GameStatus.FINISHED -> AppRoute.Winner
                gameScreenState.gamePhase != GamePhase.NONE -> AppRoute.Game
                uiState.value.showLobbyScreen -> AppRoute.Lobby
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

    /**
     * Clears the lastNavigation cache. Should be called when the NavController
     * actually changes destination (to allow re-emitting the same navigation
     * later if needed).
     */
    fun clearLastNavigation() {
        lastNavigation = null
    }
}

sealed class NavigationEvent {
    data class NavigateTo(
        val route: AppRoute,
        val arguments: AppRoute.AppRouteArguments = AppRoute.AppRouteArguments(),
    ) : NavigationEvent()
}
