package com.machikoro.client.ui.connection

sealed class ConnectionBannerState {
    data object Hidden : ConnectionBannerState()
    data object Disconnected : ConnectionBannerState()
    data object Reconnected : ConnectionBannerState()
}
