package com.machikoro.client.network.auth

import org.junit.Assert.assertNotNull
import org.junit.Test

class AuthApiFactoryTest {
    @Test
    fun testCreateReturnsAuthApi() {
        val api = AuthApiFactory.create("http://localhost/")
        assertNotNull(api)
    }
}
