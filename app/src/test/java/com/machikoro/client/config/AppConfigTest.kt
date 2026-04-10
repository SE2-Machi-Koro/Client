package com.machikoro.client.config

import org.junit.Assert.assertEquals
import org.junit.Test

class AppConfigTest {
    @Test
    fun exposesConfiguredDefaultUrls() {
        assertEquals("http://10.0.2.2:8080", AppConfig.backendBaseUrl)
        assertEquals("ws://10.0.2.2:8080/ws", AppConfig.websocketUrl)
    }
}
