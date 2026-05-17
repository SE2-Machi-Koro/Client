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
            showLobbyScreen = false,
        )
        advanceUntilIdle()

        assertEquals(NavigationEvent.NavigateTo(AppRoute.Main), events.single())
    }

    @Test
    fun loggedInStateNavigatesToHome() = runTest {
        val viewModel = NavigationViewModel()
        val events = collectNavigationEvents(viewModel)

        viewModel.updateNavigationBasedOnState(
            gameScreenState = GameScreenState.initial(),
            startScreenState = StartScreenState.placeholder().copy(loggedInAs = "alice"),
            lobbyCode = null,
            showLobbyScreen = false,
        )
        advanceUntilIdle()

        assertEquals(NavigationEvent.NavigateTo(AppRoute.Home), events.single())
    }

    @Test
    fun lobbyStateNavigatesToLobbyWithLobbyCode() = runTest {
        val viewModel = NavigationViewModel()
        val events = collectNavigationEvents(viewModel)

        viewModel.updateNavigationBasedOnState(
            gameScreenState = GameScreenState.initial(),
            startScreenState = StartScreenState.placeholder().copy(loggedInAs = "alice"),
            lobbyCode = "ABC1234",
            showLobbyScreen = true,
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
    fun activeGameNavigatesToGameWithGameId() = runTest {
        val viewModel = NavigationViewModel()
        val events = collectNavigationEvents(viewModel)

        viewModel.updateNavigationBasedOnState(
            gameScreenState = GameScreenState.initial().copy(
                gamePhase = GamePhase.ROLL_DICE,
                gameId = 42,
            ),
            startScreenState = StartScreenState.placeholder().copy(loggedInAs = "alice"),
            lobbyCode = "ABC1234",
            showLobbyScreen = true,
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

        viewModel.updateNavigationBasedOnState(
            gameScreenState = GameScreenState.initial().copy(
                gameStatus = GameStatus.FINISHED,
                gamePhase = GamePhase.ROLL_DICE,
                gameId = 42,
            ),
            startScreenState = StartScreenState.placeholder().copy(loggedInAs = "alice"),
            lobbyCode = "ABC1234",
            showLobbyScreen = true,
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
    fun duplicateNavigationEventIsNotEmittedAgain() = runTest {
        val viewModel = NavigationViewModel()
        val events = collectNavigationEvents(viewModel)

        viewModel.navigateTo(AppRoute.Home)
        viewModel.navigateTo(AppRoute.Home)
        advanceUntilIdle()

        assertEquals(listOf(NavigationEvent.NavigateTo(AppRoute.Home)), events)
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
