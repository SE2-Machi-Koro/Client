package com.machikoro.client.domain.session

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionTest {
    @Test
    fun testSessionData() {
        val session = Session(sessionToken = "token123", username = "user1")
        assertEquals("token123", session.sessionToken)
        assertEquals("user1", session.username)
    }
}
