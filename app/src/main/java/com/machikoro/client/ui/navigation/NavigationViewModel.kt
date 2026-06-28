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
    // Issue #175: this is set only by an explicit login action, not by session
    // hydration, so a restored session cannot skip the start screen on launch.
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

    // Route currently shown by the NavController. Kept in sync via
    // onDestinationChanged so state-driven routing can tell when an overlay
    // route (see overlayRoutes) is in front.
    internal var currentRoute: String? = null
        private set

    // True only after the user explicitly enters a lobby this session.
    // Prevents reconnect snapshots from auto-navigating to Game right after login.
    private var hasBeenInLobby = false

    // Overlay routes are opened by an explicit user action (e.g. tapping
    // "View Leaderboard") and layer on top of the state-driven flow. While one of
    // these is the current destination, background state updates must not navigate
    // away from it, otherwise a competing state-driven NavigateTo can bounce the
    // user off the overlay (issue #373).
    private val overlayRoutes = setOf(AppRoute.Leaderboard.route)

    fun showLobby() {
        hasBeenInLobby = true
        mutableUiState.update { it.copy(showLobbyScreen = true) }
    }

    fun leaveLobby() {
        hasBeenInLobby = false
        mutableUiState.update { it.copy(showLobbyScreen = false) }
        navigateTo(AppRoute.Home)
    }

    fun onUserLoggedIn() {
        mutableUiState.update { it.copy(userHasLoggedIn = true) }
    }

    fun onUserLoggedOut() {
        hasBeenInLobby = false
        mutableUiState.update {
            it.copy(
                showLobbyScreen = false,
                userHasLoggedIn = false,
            )
        }
    }

    // Navigates directly to Game without going through the lobby flow.
    fun resumeGame(gameId: Int?) {
        hasBeenInLobby = true
        navigateTo(AppRoute.Game, AppRoute.AppRouteArguments(gameId = gameId))
    }

    // Relies on clearGameState() before calling this so state-based routing
    // resolves back to Home instead of Winner/Game.
    fun returnHome() {
        mutableUiState.update { it.copy(showLobbyScreen = false) }
        navigateTo(AppRoute.Home)
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
            val ui = uiState.value
            val loggedIn = startScreenState.loggedInAs != null
            val targetRoute = when {
                !loggedIn -> {
                    onUserLoggedOut()
                    AppRoute.Main
                }
                !ui.userHasLoggedIn -> AppRoute.Main
                gameScreenState.gameStatus == GameStatus.FINISHED -> AppRoute.Winner
                // Gate on hasBeenInLobby: reconnect snapshots alone must not skip HomeScreen.
                hasBeenInLobby && gameScreenState.gameStatus == GameStatus.IN_PROGRESS -> AppRoute.Game
                ui.showLobbyScreen && (gameScreenState.gameStatus == GameStatus.WAITING || gameScreenState.gameStatus == null)-> AppRoute.Lobby
                else -> AppRoute.Home
            }

            // Don't let a background state update pull the user off an overlay
            // route they explicitly opened (issue #373). A forced logout still
            // wins because unauthenticated users must always return to Main.
            if (currentRoute in overlayRoutes && targetRoute != AppRoute.Main) {
                return@launch
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

    /**
     * Records the NavController's current destination and clears the
     * lastNavigation cache. Called whenever the NavController changes
     * destination so state-driven routing knows which route is in front and can
     * leave overlay routes (e.g. Leaderboard) untouched.
     */
    fun onDestinationChanged(route: String?) {
        currentRoute = route
        clearLastNavigation()
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
