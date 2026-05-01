package com.machikoro.client.domain.session

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SessionManagerTest {

    @Before
    fun ensureCleanStartingState() {
        // Defensive — if a prior test class left state in the singleton (or a
        // prior test in this class threw before its @After), don't read it.
        SessionManager.signOut()
    }

    @After
    fun resetSingleton() {
        // Tests share the global singleton; clear after each so order doesn't matter.
        SessionManager.signOut()
    }

    @Test
    fun `signIn populates session`() {
        SessionManager.signIn(token = "uuid-123", username = "alice")

        assertEquals(Session("uuid-123", "alice"), SessionManager.session.value)
    }

    @Test
    fun `signOut clears session`() {
        SessionManager.signIn(token = "uuid-123", username = "alice")

        SessionManager.signOut()

        assertNull(SessionManager.session.value)
    }
}
