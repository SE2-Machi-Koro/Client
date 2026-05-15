package com.machikoro.client.network.auth

import org.junit.Assert.assertNotNull
import org.junit.Test

class AuthApiTest {
    @Test
    fun testInterfaceExists() {
        // Just check that the interface can be referenced
        val clazz = AuthApi::class
        assertNotNull(clazz)
    }
}
