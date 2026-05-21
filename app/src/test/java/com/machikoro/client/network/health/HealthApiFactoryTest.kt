package com.machikoro.client.network.health

import org.junit.Assert.assertNotNull
import org.junit.Test

class HealthApiFactoryTest {
    @Test
    fun createReturnsHealthApi() {
        val api = HealthApiFactory.create("http://localhost/")
        assertNotNull(api)
    }

    @Test
    fun createWithTrailingSlashSucceeds() {
        val api = HealthApiFactory.create("http://10.0.2.2:8080/")
        assertNotNull(api)
    }
}
