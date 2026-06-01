package com.machikoro.client.network.leaderboard

import org.junit.Assert.assertNotNull
import org.junit.Test

class LeaderboardApiTest {

    @Test
    fun interfaceCanBeReferenced() {
        val clazz = LeaderboardApi::class
        assertNotNull(clazz)
    }

    @Test
    fun getLeaderboardMethodExistsOnInterface() {
        val method = LeaderboardApi::class.java.methods.find { it.name == "getLeaderboard" }
        assertNotNull(method)
    }
}