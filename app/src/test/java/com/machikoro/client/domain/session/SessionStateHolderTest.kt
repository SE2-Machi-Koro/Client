package com.machikoro.client.domain.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class DummySessionStateHolder : SessionStateHolder {
    override val session: StateFlow<Session?> = MutableStateFlow(null)
    override fun signIn(token: String, username: String) {}
    override fun signOut() {}
}

class SessionStateHolderTest {
    @Test
    fun testDummyImplementation() {
        val holder = DummySessionStateHolder()
        assertNull(holder.session.value)
        holder.signIn("token", "user")
        holder.signOut()
    }
}
