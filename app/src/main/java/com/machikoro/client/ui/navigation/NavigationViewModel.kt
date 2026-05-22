package com.machikoro.client.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
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
class NavigationViewModel(
    private val _navigationChannel: Channel<NavigationEvent> = Channel(Channel.BUFFERED)
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = mutableUiState.asStateFlow()

    // Expose the channel as a Flow for collectors (AppRoot).
    val navigationEvent = _navigationChannel.receiveAsFlow()

    // Track last emitted navigation to avoid emitting duplicate navigation events
    // which can cause unnecessary navigation attempts and UI churn.
    internal var lastNavigation: Pair<AppRoute, AppRoute.AppRouteArguments>? = null

    // True only after the user explicitly enters a lobby this session.
    // Prevents reconnect snapshots from auto-navigating to Game right after login.
    private var hasBeenInLobby = false

    fun showLobby() {
        hasBeenInLobby = true
        mutableUiState.update { it.copy(showLobbyScreen = true) }
    }

    fun leaveLobby() {
        hasBeenInLobby = false
        mutableUiState.update { it.copy(showLobbyScreen = false) }
        navigateTo(AppRoute.Home)
    }

    // Navigates directly to Game without going through the lobby flow.
    fun resumeGame(gameId: Int?) {
        hasBeenInLobby = true
        navigateTo(AppRoute.Game, AppRoute.AppRouteArguments(gameId = gameId))
    }

    fun navigateTo(
        route: AppRoute,
        arguments: AppRoute.AppRouteArguments = AppRoute.AppRouteArguments(),
    ) {
        val next = route to arguments
        if (lastNavigation == next) return

        // Reserve the destination to avoid races from concurrent callers. We
        // send the navigation command through the channel; if sending fails
        // (closed/cancelled) we clear the reservation so the route can be
        // retried later.
        lastNavigation = next

        viewModelScope.launch {
            try {
                _navigationChannel.send(NavigationEvent.NavigateTo(route, arguments))
            } catch (t: Throwable) {
                if (lastNavigation == next) lastNavigation = null
                // Swallow the error after clearing the reservation so a failed
                // send doesn't poison navigation. Upstream logs will still
                // surface via the coroutine exception handler if configured.
            }
        }
    }

    /**
     * Updates navigation based on app state changes.
     *
     * Unauthenticated users always return to Main. For authenticated users,
     * route priority is Winner > Game > Lobby > Home, matching the current
     * game flow documented in docs/navigation.md.
     */
    fun updateNavigationBasedOnState(
        gameScreenState: GameScreenState,
        startScreenState: StartScreenState,
        lobbyCode: String?,
    ) {
        viewModelScope.launch {
            val loggedIn = startScreenState.loggedInAs != null
            val targetRoute = when {
                !loggedIn -> {
                    hasBeenInLobby = false
                    AppRoute.Main
                }
                gameScreenState.gameStatus == GameStatus.FINISHED -> AppRoute.Winner
                // Gate on hasBeenInLobby: reconnect snapshots alone must not skip HomeScreen.
                hasBeenInLobby && gameScreenState.gamePhase != GamePhase.NONE -> AppRoute.Game
                uiState.value.showLobbyScreen -> AppRoute.Lobby
                else -> AppRoute.Home
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
