package com.machikoro.client.network.debug

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DebugApiFactoryTest {
    private fun baseRequest(): Request = Request.Builder().url("http://localhost/").build()

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
        val api = DebugApiFactory.create("http://localhost/") { "test-token" }
        assertNotNull(api)
    }

    @Test
    fun withAuthAddsAuthorizationHeaderWhenTokenPresent() {
        val result = DebugApiFactory.withAuth(baseRequest(), "my-token")
        assertEquals("Bearer my-token", result.header("Authorization"))
    }

    @Test
    fun withAuthReturnsOriginalRequestWhenTokenNull() {
        val original = baseRequest()
        val result = DebugApiFactory.withAuth(original, null)
        // Same request returned — no header added
        assertNull(result.header("Authorization"))
        assertEquals(original, result)
    }

    @Test
    fun withAuthDoesNotModifyOtherHeaders() {
        val request = baseRequest().newBuilder().header("X-Custom", "value").build()
        val result = DebugApiFactory.withAuth(request, "token")
        assertEquals("value", result.header("X-Custom"))
        assertEquals("Bearer token", result.header("Authorization"))
    }

    @Test
    fun withAuthOverwritesExistingAuthorizationHeader() {
        val request = baseRequest().newBuilder().header("Authorization", "Bearer old").build()
        val result = DebugApiFactory.withAuth(request, "new-token")
        assertEquals("Bearer new-token", result.header("Authorization"))
    }
}