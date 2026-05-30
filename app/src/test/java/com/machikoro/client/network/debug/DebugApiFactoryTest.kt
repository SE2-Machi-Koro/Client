package com.machikoro.client.network.debug

import org.junit.Assert.assertNotNull
import org.junit.Test

class DebugApiFactoryTest {
    @Test
    fun createReturnsDebugApi() {
        val api = DebugApiFactory.create("http://localhost/") { null }
        assertNotNull(api)
    }

    @Test
    fun createWithTrailingSlashSucceeds() {
        val api = DebugApiFactory.create("http://10.0.2.2:8080/") { null }
        assertNotNull(api)
    }

    @Test
    fun createWithTokenProviderSucceeds() {
        // Token provider returning a value should not break construction
        val api = DebugApiFactory.create("http://localhost/") { "test-token" }
        assertNotNull(api)
    }
}