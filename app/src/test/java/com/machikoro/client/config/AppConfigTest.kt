package com.machikoro.client.config

import org.junit.Assert.assertEquals
import org.junit.Test

class AppConfigTest {
    @Test
    fun exposesConfiguredDefaultUrls() {
        assertEquals("https://machi-koro.up.railway.app", AppConfig.backendBaseUrl)
        assertEquals("wss://machi-koro.up.railway.app/ws", AppConfig.websocketUrl)
    }
}
