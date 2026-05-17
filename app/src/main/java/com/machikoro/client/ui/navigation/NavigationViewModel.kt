package com.machikoro.client.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NavigationViewModel : ViewModel() {

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>(extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    fun navigateTo(
        route: AppRoute,
        arguments: AppRoute.AppRouteArguments = AppRoute.AppRouteArguments(),
    ) {
        _navigationEvent.tryEmit(NavigationEvent.NavigateTo(route, arguments))
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
}

sealed class NavigationEvent {
    data class NavigateTo(
        val route: AppRoute,
        val arguments: AppRoute.AppRouteArguments = AppRoute.AppRouteArguments(),
    ) : NavigationEvent()
}