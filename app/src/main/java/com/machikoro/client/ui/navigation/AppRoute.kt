package com.machikoro.client.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// Central route definitions for the top-level app navigation graph.
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

    open fun destination(arguments: AppRouteArguments = AppRouteArguments()): String = route

    data class AppRouteArguments(
        val lobbyCode: String? = null,
        val gameId: Int? = null,
    )

    private companion object {
        fun String.encodeRouteValue(): String =
            URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
    }
}
