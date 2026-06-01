package com.machikoro.client.network.leaderboard

import org.junit.Assert.assertNotNull
import org.junit.Test

class LeaderboardApiTest {

    @Test
    fun interfaceCanBeReferenced() {
        // Confirms the interface is accessible and loadable by the class loader
        val clazz = LeaderboardApi::class
        assertNotNull(clazz)
    }
}