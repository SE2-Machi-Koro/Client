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
        if (navController.currentDestination?.route != route.route) return false

        val currentArguments = navController.currentBackStackEntry?.arguments
        return when (route) {
            AppRoute.Lobby -> {
                val expectedLobbyCode = arguments.lobbyCode?.takeIf { it.isNotBlank() }
                currentArguments?.getString(AppRoute.Lobby.LOBBY_CODE_ARGUMENT) == expectedLobbyCode
            }
            AppRoute.Game ->
                currentArguments?.getInt(AppRoute.Game.GAME_ID_ARGUMENT)
                    ?.takeIf { it != AppRoute.Game.MISSING_GAME_ID } == arguments.gameId
            else -> true
        }
    }
}
