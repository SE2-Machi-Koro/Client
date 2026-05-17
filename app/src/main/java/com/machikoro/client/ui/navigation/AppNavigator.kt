package com.machikoro.client.ui.navigation

import androidx.navigation.NavHostController
import androidx.navigation.navOptions

class AppNavigator(
    private val navController: NavHostController,
) {
    fun navigateTo(
        route: AppRoute,
        arguments: AppRoute.AppRouteArguments = AppRoute.AppRouteArguments(),
    ) {
        if (isAlreadyAt(route, arguments)) return

        navController.navigate(
            route.destination(arguments),
            navOptions {
                launchSingleTop = true
                popUpTo(AppRoute.Main.route)
            }
        )
    }

    private fun isAlreadyAt(
        route: AppRoute,
        arguments: AppRoute.AppRouteArguments,
    ): Boolean {
        // Robustly compare current destination and arguments.
        // The NavController's currentDestination.route may be the base route (e.g. "lobby")
        // while AppRoute.route for argumented routes contains placeholders
        // (e.g. "lobby?lobbyCode={lobbyCode}"). Use the BASE_ROUTE for membership
        // checks and then compare the concrete arguments to avoid unnecessary
        // navigation events (which previously caused navigation loops).
        val currentRoute = navController.currentDestination?.route ?: return false
        val currentArguments = navController.currentBackStackEntry?.arguments

        return when (route) {
            AppRoute.Lobby -> {
                if (!currentRoute.startsWith(AppRoute.Lobby.BASE_ROUTE)) return false
                val expectedLobbyCode = arguments.lobbyCode?.takeIf { it.isNotBlank() }
                currentArguments?.getString(AppRoute.Lobby.LOBBY_CODE_ARGUMENT) == expectedLobbyCode
            }
            AppRoute.Game -> {
                if (!currentRoute.startsWith(AppRoute.Game.BASE_ROUTE)) return false
                currentArguments?.getInt(AppRoute.Game.GAME_ID_ARGUMENT)
                    ?.takeIf { it != AppRoute.Game.MISSING_GAME_ID } == arguments.gameId
            }
            else -> currentRoute == route.route
        }
    }
}
