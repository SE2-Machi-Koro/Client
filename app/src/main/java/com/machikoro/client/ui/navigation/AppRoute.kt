package com.machikoro.client.ui.navigation

// Central route definitions for the top-level app navigation graph.
// TODO(#65): Add navigation actions/helper methods so call sites do not navigate by raw route strings.
// TODO(#66): Add typed route arguments here once lobby/game context is passed through navigation.
sealed class AppRoute(val route: String) {
    data object Main : AppRoute("main")
    data object Home : AppRoute("home")
    data object Lobby : AppRoute("lobby")
    data object Game : AppRoute("game")
}