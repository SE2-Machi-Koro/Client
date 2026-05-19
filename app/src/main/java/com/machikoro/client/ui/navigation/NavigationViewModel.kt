package com.machikoro.client.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.StateFlow
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
 * [userHasLoggedIn] is set explicitly when the user completes a login action,
 * and is never inferred from session hydration alone. This prevents the app
 * from auto-navigating away from the start screen on cold start when a
 * persisted session is restored in the background.
 */
data class NavigationUiState(
    val showLobbyScreen: Boolean = false,
    // FIX (Bug 1 & 2): Tracks whether the user has explicitly logged in during
    // this app session. False on cold start even if a session is hydrated from
    // storage, so the start screen is never auto-skipped.
    val userHasLoggedIn: Boolean = false,
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

    fun showLobby() {
        mutableUiState.update { it.copy(showLobbyScreen = true) }
    }

    fun leaveLobby() {
        mutableUiState.update { it.copy(showLobbyScreen = false) }
    }

    /**
     * FIX (Bug 1 & 2): Called only from the explicit login success callback,
     * never from a session-hydration observer. This ensures the start screen
     * stays visible until the user actively logs in.
     */
    fun onUserLoggedIn() {
        mutableUiState.update { it.copy(userHasLoggedIn = true) }
    }

    /**
     * Resets the login flag when the user logs out so the next cold start
     * correctly returns to the start screen.
     */
    fun onUserLoggedOut() {
        mutableUiState.update { it.copy(userHasLoggedIn = false, showLobbyScreen = false) }
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
     * Route priority is Winner > Game > Lobby > Home > Main, matching the
     * current game flow documented in docs/navigation.md.
     *
     * FIX (Bug 1 & 2): Navigation to Home now requires [NavigationUiState.userHasLoggedIn]
     * to be true. This flag is only set by an explicit user login action
     * ([onUserLoggedIn]), not by session hydration, so the start screen is
     * never auto-skipped on app startup.
     *
     * Navigation to Game now additionally requires [NavigationUiState.userHasLoggedIn]
     * so that a stale gamePhase/gameId in the WebSocket state cannot trigger an
     * automatic jump into the game screen before the user has done anything.
     */
    fun updateNavigationBasedOnState(
        gameScreenState: GameScreenState,
        startScreenState: StartScreenState,
        lobbyCode: String?,
    ) {
        viewModelScope.launch {
            val ui = uiState.value

            val targetRoute = when {
                // Game-over: always navigate to winner screen regardless of login state
                // (user is already in an active session when this fires).
                gameScreenState.gameStatus == GameStatus.FINISHED -> AppRoute.Winner

                // FIX: Only navigate to Game when the user has explicitly logged in
                // during this session. Prevents stale gamePhase in the WebSocket
                // state from auto-navigating on app startup.
                ui.userHasLoggedIn && gameScreenState.gamePhase != GamePhase.NONE -> AppRoute.Game

                // FIX: Only navigate to Lobby when the user has logged in.
                ui.userHasLoggedIn && ui.showLobbyScreen -> AppRoute.Lobby

                // FIX: Only navigate to Home when the user has explicitly logged in.
                // Previously `startScreenState.loggedInAs != null` was enough, which
                // caused auto-navigation whenever a persisted session was hydrated.
                ui.userHasLoggedIn && startScreenState.loggedInAs != null -> AppRoute.Home

                // Default: stay on (or navigate back to) the start screen.
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