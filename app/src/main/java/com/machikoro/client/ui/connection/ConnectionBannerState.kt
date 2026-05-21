package com.machikoro.client.ui.connection

sealed class ConnectionBannerState {
    object Hidden : ConnectionBannerState()
    object Disconnected : ConnectionBannerState()
    object Reconnected : ConnectionBannerState()
}
