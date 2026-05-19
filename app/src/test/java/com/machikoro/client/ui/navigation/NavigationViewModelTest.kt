package com.machikoro.client.ui.navigation

import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.StartScreenState
import com.machikoro.client.ui.start.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun unauthenticatedStateNavigatesToMain() = runTest {
        val viewModel = NavigationViewModel()
        val events = collectNavigationEvents(viewModel)

        viewModel.updateNavigationBasedOnState(
            gameScreenState = GameScreenState.initial(),
            startScreenState = StartScreenState.placeholder(),
            lobbyCode = null,
        )
        advanceUntilIdle()

        assertEquals(NavigationEvent.NavigateTo(AppRoute.Main), events.single())
    }

    @Test
    fun loggedInStateNavigatesToHome() = runTest {
        val viewModel = NavigationViewModel()
        val events = collectNavigationEvents(viewModel)

        // FIX: explicit login required before Home is reachable
        viewModel.onUserLoggedIn()
        viewModel.updateNavigationBasedOnState(
            gameScreenState = GameScreenState.initial(),
            startScreenState = StartScreenState.placeholder().copy(loggedInAs = "alice"),
            lobbyCode = null,
        )
        advanceUntilIdle()

        assertEquals(NavigationEvent.NavigateTo(AppRoute.Home), events.single())
    }

    @Test
    fun sessionHydrationAloneDoesNotNavigateAwayFromMain() = runTest {
        // Regression test for Bug 1: persisted session restored on startup must
        // NOT auto-navigate to Home — the user has to explicitly log in first.
        val viewModel = NavigationViewModel()
        val events = collectNavigationEvents(viewModel)

        // Simulate what happens after SessionManager.hydrate(): loggedInAs is
        // populated but onUserLoggedIn() has NOT been called yet.
        viewModel.updateNavigationBasedOnState(
            gameScreenState = GameScreenState.initial(),
            startScreenState = StartScreenState.placeholder().copy(loggedInAs = "alice"),
            lobbyCode = null,
        )
        advanceUntilIdle()

        assertEquals(NavigationEvent.NavigateTo(AppRoute.Main), events.single())
    }

    @Test
    fun lobbyStateNavigatesToLobbyWithLobbyCode() = runTest {
        val viewModel = NavigationViewModel()
        val events = collectNavigationEvents(viewModel)

        // FIX: explicit login required before Lobby is reachable
        viewModel.onUserLoggedIn()
        viewModel.showLobby()
        viewModel.updateNavigationBasedOnState(
            gameScreenState = GameScreenState.initial(),
            startScreenState = StartScreenState.placeholder().copy(loggedInAs = "alice"),
            lobbyCode = "ABC1234",
        )
        advanceUntilIdle()

        assertEquals(
            NavigationEvent.NavigateTo(
                route = AppRoute.Lobby,
                arguments = AppRoute.AppRouteArguments(lobbyCode = "ABC1234"),
            ),
            events.single(),
        )
    }

    @Test
    fun unauthenticatedStateNavigatesToMainEvenWhenLobbyWasShown() = runTest {
        val viewModel = NavigationViewModel()
        val events = collectNavigationEvents(viewModel)

        viewModel.showLobby()
        viewModel.updateNavigationBasedOnState(
            gameScreenState = GameScreenState.initial(),
            startScreenState = StartScreenState.placeholder(),
            lobbyCode = "ABC1234",
        )
        advanceUntilIdle()

        assertEquals(
            NavigationEvent.NavigateTo(
                route = AppRoute.Main,
                arguments = AppRoute.AppRouteArguments(lobbyCode = "ABC1234"),
            ),
            events.single(),
        )
    }

    @Test
    fun unauthenticatedStateNavigatesToMainEvenWhenGameStateIsStale() = runTest {
        // Regression test for Bug 2: stale gamePhase/gameId in WebSocket state
        // must NOT cause auto-navigation to Game on app startup.
        val viewModel = NavigationViewModel()
        val events = collectNavigationEvents(viewModel)

        viewModel.updateNavigationBasedOnState(
            gameScreenState = GameScreenState.initial().copy(
                gameStatus = GameStatus.FINISHED,
                gamePhase = GamePhase.ROLL_DICE,
                gameId = 42,
            ),
            startScreenState = StartScreenState.placeholder(),
            lobbyCode = "ABC1234",
        )
        advanceUntilIdle()

        assertEquals(
            NavigationEvent.NavigateTo(
                route = AppRoute.Main,
                arguments = AppRoute.AppRouteArguments(
                    lobbyCode = "ABC1234",
                    gameId = 42,
                ),
            ),
            events.single(),
        )
    }

    @Test
    fun activeGameNavigatesToGameWithGameId() = runTest {
        val viewModel = NavigationViewModel()
        val events = collectNavigationEvents(viewModel)

        // FIX: explicit login required before Game is reachable
        viewModel.onUserLoggedIn()
        viewModel.showLobby()
        viewModel.updateNavigationBasedOnState(
            gameScreenState = GameScreenState.initial().copy(
                gamePhase = GamePhase.ROLL_DICE,
                gameId = 42,
            ),
            startScreenState = StartScreenState.placeholder().copy(loggedInAs = "alice"),
            lobbyCode = "ABC1234",
        )
        advanceUntilIdle()

        assertEquals(
            NavigationEvent.NavigateTo(
                route = AppRoute.Game,
                arguments = AppRoute.AppRouteArguments(
                    lobbyCode = "ABC1234",
                    gameId = 42,
                ),
            ),
            events.single(),
        )
    }

    @Test
    fun finishedGameNavigatesToWinnerBeforeOtherRoutes() = runTest {
        val viewModel = NavigationViewModel()
        val events = collectNavigationEvents(viewModel)

        // Winner route fires regardless of userHasLoggedIn since the user was
        // already playing when the game ended.
        viewModel.showLobby()
        viewModel.updateNavigationBasedOnState(
            gameScreenState = GameScreenState.initial().copy(
                gameStatus = GameStatus.FINISHED,
                gamePhase = GamePhase.ROLL_DICE,
                gameId = 42,
            ),
            startScreenState = StartScreenState.placeholder().copy(loggedInAs = "alice"),
            lobbyCode = "ABC1234",
        )
        advanceUntilIdle()

        assertEquals(
            NavigationEvent.NavigateTo(
                route = AppRoute.Winner,
                arguments = AppRoute.AppRouteArguments(
                    lobbyCode = "ABC1234",
                    gameId = 42,
                ),
            ),
            events.single(),
        )
    }

    @Test
    fun onUserLoggedOutResetsLoginFlagAndLobbyVisibility() = runTest {
        val viewModel = NavigationViewModel()

        viewModel.onUserLoggedIn()
        viewModel.showLobby()
        assertTrue(viewModel.uiState.value.userHasLoggedIn)
        assertTrue(viewModel.uiState.value.showLobbyScreen)

        viewModel.onUserLoggedOut()

        assertFalse(viewModel.uiState.value.userHasLoggedIn)
        assertFalse(viewModel.uiState.value.showLobbyScreen)
    }

    @Test
    fun afterLogoutUpdateNavigatesToMain() = runTest {
        val viewModel = NavigationViewModel()
        val events = collectNavigationEvents(viewModel)

        viewModel.onUserLoggedIn()
        viewModel.onUserLoggedOut()
        viewModel.updateNavigationBasedOnState(
            gameScreenState = GameScreenState.initial(),
            startScreenState = StartScreenState.placeholder().copy(loggedInAs = null),
            lobbyCode = null,
        )
        advanceUntilIdle()

        assertEquals(NavigationEvent.NavigateTo(AppRoute.Main), events.single())
    }

    @Test
    fun duplicateNavigationEventIsNotEmittedAgain() = runTest {
        val viewModel = NavigationViewModel()
        val events = collectNavigationEvents(viewModel)

        viewModel.navigateTo(AppRoute.Home)
        viewModel.navigateTo(AppRoute.Home)
        advanceUntilIdle()

        assertEquals(listOf(NavigationEvent.NavigateTo(AppRoute.Home)), events)
    }

    @Test
    fun failedEmissionClearsLastNavigation() = runTest {
        val failingChannel = kotlinx.coroutines.channels.Channel<NavigationEvent>(kotlinx.coroutines.channels.Channel.RENDEZVOUS)
        val viewModel = NavigationViewModel(failingChannel)

        failingChannel.close()

        viewModel.navigateTo(AppRoute.Home)
        advanceUntilIdle()

        assertNull(viewModel.lastNavigation)
    }

    @Test
    fun clearingLastNavigationAllowsSameRouteToBeEmittedLater() = runTest {
        val viewModel = NavigationViewModel()
        val events = collectNavigationEvents(viewModel)

        viewModel.navigateTo(AppRoute.Home)
        viewModel.clearLastNavigation()
        viewModel.navigateTo(AppRoute.Home)
        advanceUntilIdle()

        assertEquals(
            listOf(
                NavigationEvent.NavigateTo(AppRoute.Home),
                NavigationEvent.NavigateTo(AppRoute.Home),
            ),
            events,
        )
    }

    @Test
    fun lobbyVisibilityIsOwnedByNavigationState() {
        val viewModel = NavigationViewModel()

        assertFalse(viewModel.uiState.value.showLobbyScreen)

        viewModel.showLobby()
        assertTrue(viewModel.uiState.value.showLobbyScreen)

        viewModel.leaveLobby()
        assertFalse(viewModel.uiState.value.showLobbyScreen)
    }

    private fun TestScope.collectNavigationEvents(
        viewModel: NavigationViewModel,
    ): MutableList<NavigationEvent> {
        val events = mutableListOf<NavigationEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.navigationEvent.toList(events)
        }
        return events
    }
}