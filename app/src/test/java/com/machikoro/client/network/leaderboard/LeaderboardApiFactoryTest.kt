package com.machikoro.client.network.leaderboard

import org.junit.Assert.assertNotNull
import org.junit.Test

class LeaderboardApiFactoryTest {

    @Test
    fun createReturnsLeaderboardApiInstance() {
        val api = LeaderboardApiFactory.create("http://localhost/") { null }
        assertNotNull(api)
    }

    @Test
    fun createWithTokenProviderReturnsLeaderboardApiInstance() {
        val api = LeaderboardApiFactory.create("http://localhost/") { "test-token" }
        assertNotNull(api)
    }
}