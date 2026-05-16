package com.machikoro.client.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class AppRouteTest {
    @Test
    fun lobbyDestinationOmitsArgumentWhenLobbyCodeIsMissing() {
        val route = AppRoute.Lobby.destination()

        assertEquals(AppRoute.Lobby.BASE_ROUTE, route)
    }

    @Test
    fun lobbyDestinationIncludesLobbyCodeArgument() {
        val route = AppRoute.Lobby.destination(
            AppRoute.AppRouteArguments(lobbyCode = "ABC1234")
        )

        assertEquals("lobby?lobbyCode=ABC1234", route)
    }

    @Test
    fun gameDestinationOmitsArgumentWhenGameIdIsMissing() {
        val route = AppRoute.Game.destination()

        assertEquals(AppRoute.Game.BASE_ROUTE, route)
    }

    @Test
    fun gameDestinationIncludesGameIdArgument() {
        val route = AppRoute.Game.destination(
            AppRoute.AppRouteArguments(gameId = 7)
        )

        assertEquals("game?gameId=7", route)
    }
}
