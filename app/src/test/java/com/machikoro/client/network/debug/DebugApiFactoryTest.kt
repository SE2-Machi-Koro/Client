package com.machikoro.client.network.debug

import org.junit.Assert.assertNotNull
import org.junit.Test

class DebugApiFactoryTest {
    @Test
    fun createReturnsDebugApi() {
        val api = DebugApiFactory.create("http://localhost/")
        assertNotNull(api)
    }

    @Test
    fun createWithTrailingSlashSucceeds() {
        val api = DebugApiFactory.create("http://10.0.2.2:8080/")
        assertNotNull(api)
    }
}