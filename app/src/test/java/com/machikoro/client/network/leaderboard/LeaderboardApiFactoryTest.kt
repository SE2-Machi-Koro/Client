package com.machikoro.client.network.leaderboard

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LeaderboardApiFactoryTest {

    private fun baseRequest(): Request = Request.Builder().url("http://localhost/").build()

    @Test
    fun createReturnsLeaderboardApiInstance() {
        val api = LeaderboardApiFactory.create("http://localhost/") { null }
        assertNotNull(api)
    }

    @Test
    fun createWithTrailingSlashSucceeds() {
        val api = LeaderboardApiFactory.create("http://10.0.2.2:8080/") { null }
        assertNotNull(api)
    }

    @Test
    fun createWithTokenProviderSucceeds() {
        val api = LeaderboardApiFactory.create("http://localhost/") { "test-token" }
        assertNotNull(api)
    }

    @Test
    fun withAuthAddsAuthorizationHeaderWhenTokenPresent() {
        val result = LeaderboardApiFactory.withAuth(baseRequest(), "my-token")
        assertEquals("Bearer my-token", result.header("Authorization"))
    }

    @Test
    fun withAuthReturnsOriginalRequestWhenTokenNull() {
        val original = baseRequest()
        val result = LeaderboardApiFactory.withAuth(original, null)
        // No header added, same request returned
        assertNull(result.header("Authorization"))
        assertEquals(original, result)
    }

    @Test
    fun withAuthDoesNotModifyOtherHeaders() {
        val request = baseRequest().newBuilder().header("X-Custom", "value").build()
        val result = LeaderboardApiFactory.withAuth(request, "token")
        assertEquals("value", result.header("X-Custom"))
        assertEquals("Bearer token", result.header("Authorization"))
    }

    @Test
    fun withAuthOverwritesExistingAuthorizationHeader() {
        val request = baseRequest().newBuilder().header("Authorization", "Bearer old").build()
        val result = LeaderboardApiFactory.withAuth(request, "new-token")
        assertEquals("Bearer new-token", result.header("Authorization"))
    }
}