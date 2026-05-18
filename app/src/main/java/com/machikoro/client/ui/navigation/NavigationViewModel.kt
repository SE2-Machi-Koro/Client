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

/**
 * Durable navigation UI state.
 *
 * This belongs in a ViewModel instead of MainActivity remember state so route
 * decisions survive recomposition and configuration changes.
 */
data class NavigationUiState(
    val showLobbyScreen: Boolean = false,
)

/**
 * Single source for top-level navigation state and route decisions.
 *
 * The ViewModel converts app state into NavigationEvent commands while keeping
 * persistent navigation UI state separate from one-time navigation events.
 */
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

        // Reserve the destination to prevent duplicate navigations from racing
        // callers. We use a suspending emit below which will not drop events the
        // way tryEmit can; if emission fails (cancellation / error) we clear
        // the cache so we don't permanently poison navigation for this route.
        lastNavigation = next

        viewModelScope.launch {
            try {
                _navigationEvent.emit(NavigationEvent.NavigateTo(route, arguments))
            } catch (t: Throwable) {
                // If emit failed for any reason (including cancellation), clear
                // the reservation so subsequent calls can retry.
                if (lastNavigation == next) lastNavigation = null
                throw t
            }
        }
    }

    /**
     * Updates navigation based on app state changes.
     *
     * Route priority is Winner > Game > Lobby > Home > Main, matching the
     * current game flow documented in docs/navigation.md.
     */
    fun updateNavigationBasedOnState(
        gameScreenState: GameScreenState,
        startScreenState: StartScreenState,
        lobbyCode: String?,
    ) {
        viewModelScope.launch {
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
    /**
     * One-time command consumed by AppRoot and applied through AppNavigator.
     */
    data class NavigateTo(
        val route: AppRoute,
        val arguments: AppRoute.AppRouteArguments = AppRoute.AppRouteArguments(),
    ) : NavigationEvent()
}
