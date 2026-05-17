package com.machikoro.client.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Central route definitions for the top-level app navigation graph.
 *
 * Route strings and argument names should live here so callers do not assemble
 * hardcoded destinations across the UI layer.
 */
sealed class AppRoute(val route: String) {
    data object Main : AppRoute("main")
    data object Home : AppRoute("home")
    data object Lobby : AppRoute("lobby?lobbyCode={lobbyCode}") {
        const val BASE_ROUTE = "lobby"
        const val LOBBY_CODE_ARGUMENT = "lobbyCode"

        override fun destination(arguments: AppRouteArguments): String {
            val lobbyCode = arguments.lobbyCode?.takeIf { it.isNotBlank() } ?: return BASE_ROUTE
            return "$BASE_ROUTE?$LOBBY_CODE_ARGUMENT=${lobbyCode.encodeRouteValue()}"
        }
    }

    data object Game : AppRoute("game?gameId={gameId}") {
        const val BASE_ROUTE = "game"
        const val GAME_ID_ARGUMENT = "gameId"
        const val MISSING_GAME_ID = -1

        override fun destination(arguments: AppRouteArguments): String {
            val gameId = arguments.gameId ?: return BASE_ROUTE
            return "$BASE_ROUTE?$GAME_ID_ARGUMENT=$gameId"
        }
    }

    data object Winner : AppRoute("winner")

    /**
     * Builds the concrete destination used by NavController.navigate(...).
     * Routes without arguments return their route unchanged.
     */
    open fun destination(arguments: AppRouteArguments = AppRouteArguments()): String = route

    /**
     * Optional top-level route arguments. Only the destination that understands
     * an argument consumes it; carrying both keeps navigation events uniform.
     */
    data class AppRouteArguments(
        val lobbyCode: String? = null,
        val gameId: Int? = null,
    )

    private companion object {
        fun String.encodeRouteValue(): String =
            URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
    }
}
