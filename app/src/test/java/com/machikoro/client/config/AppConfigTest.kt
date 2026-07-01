package com.machikoro.client.config

import org.junit.Assert.assertTrue
import org.junit.Test

class AppConfigTest {
    @Test
    fun exposesValidConfiguredUrls() {
        assertTrue(AppConfig.backendBaseUrl.startsWith("http"))
        assertTrue(AppConfig.websocketUrl.startsWith("ws"))
        assertTrue(AppConfig.backendBaseUrl.isNotBlank())
        assertTrue(AppConfig.websocketUrl.isNotBlank())
    }
}
