package com.machikoro.client.domain.session

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SessionManagerTest {

    @Before
    fun ensureCleanStartingState() {
        SessionManager.signOut()
        SessionManager.attach(NoopSessionStorage)
    }

    @After
    fun resetSingleton() {
        SessionManager.signOut()
        SessionManager.attach(NoopSessionStorage)
    }

    @Test
    fun `signIn populates session`() {
        SessionManager.signIn(token = "uuid-123", username = "alice", userId = 1)

        assertEquals(Session("uuid-123", "alice", 1), SessionManager.session.value)
    }

    @Test
    fun `signOut clears session`() {
        SessionManager.signIn(token = "uuid-123", username = "alice", userId = 1)

        SessionManager.signOut()

        assertNull(SessionManager.session.value)
    }

    @Test
    fun `signIn writes through to storage`() = runBlocking {
        val fake = FakeSessionStorage()
        SessionManager.attach(fake)

        SessionManager.signIn(token = "uuid-123", username = "alice", userId = 1)

        val written = withTimeout(2_000) { fake.awaitWrite() }
        assertEquals(Session("uuid-123", "alice", 1), written)
    }

    @Test
    fun `signOut clears storage`() = runBlocking {
        val fake = FakeSessionStorage()
        SessionManager.attach(fake)

        SessionManager.signOut()

        withTimeout(2_000) { fake.awaitClear() }
    }

    @Test
    fun `hydrate emits persisted session into the flow`() = runBlocking {
        val fake = FakeSessionStorage(initial = Session("uuid-xyz", "bob", 2))
        SessionManager.attach(fake)

        SessionManager.hydrate()

        assertEquals(Session("uuid-xyz", "bob", 2), SessionManager.session.value)
    }

    @Test
    fun `hydrate is a no-op when already signed in`() = runBlocking {
        val fake = FakeSessionStorage(initial = Session("from-disk", "from-disk", 99))
        SessionManager.attach(fake)
        SessionManager.signIn(token = "in-memory", username = "alice", userId = 1)

        SessionManager.hydrate()

        assertEquals(Session("in-memory", "alice", 1), SessionManager.session.value)
    }

    @Test
    fun `hydrate with no persisted session leaves flow null`() = runBlocking {
        val fake = FakeSessionStorage(initial = null)
        SessionManager.attach(fake)

        SessionManager.hydrate()

        assertNull(SessionManager.session.value)
    }

    private class FakeSessionStorage(
        initial: Session? = null,
    ) : SessionStorage {
        private var stored: Session? = initial
        private val written = CompletableDeferred<Session>()
        private val cleared = CompletableDeferred<Unit>()

        override suspend fun read(): Session? = stored

        override suspend fun write(session: Session) {
            stored = session
            written.complete(session)
        }

        override suspend fun clear() {
            stored = null
            cleared.complete(Unit)
        }

        suspend fun awaitWrite(): Session = written.await()
        suspend fun awaitClear() = cleared.await()
    }

    private object NoopSessionStorage : SessionStorage {
        override suspend fun read(): Session? = null
        override suspend fun write(session: Session) = Unit
        override suspend fun clear() = Unit
    }
}