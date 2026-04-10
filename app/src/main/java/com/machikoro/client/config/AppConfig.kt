package com.machikoro.client.config

import com.machikoro.client.BuildConfig

object AppConfig {
    val backendBaseUrl: String = BuildConfig.BACKEND_BASE_URL
    val websocketUrl: String = BuildConfig.WEBSOCKET_URL
}
