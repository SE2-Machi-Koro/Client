package com.machikoro.client.ui.navigation

import androidx.navigation.NavHostController
import androidx.navigation.navOptions

class AppNavigator(
    private val navController: NavHostController,
) {
    fun navigateTo(route: AppRoute) {
        if (navController.currentDestination?.route == route.route) return

        navController.navigate(
            route.route,
            navOptions {
                launchSingleTop = true
                popUpTo(AppRoute.Main.route)
            }
        )
    }
}
