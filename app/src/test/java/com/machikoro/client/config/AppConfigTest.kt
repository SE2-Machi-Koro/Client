package com.machikoro.client.config

import org.junit.Assert.assertFalse
import org.junit.Test

class AppConfigTest {
    @Test
    fun exposesConfiguredDefaultUrls() {
        // Backend is chosen via backend.properties; verify config is wired, not a specific URL.
        assertFalse("backendBaseUrl must not be empty", AppConfig.backendBaseUrl.isBlank())
        assertFalse("websocketUrl must not be empty", AppConfig.websocketUrl.isBlank())
    }
}
