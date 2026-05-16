package com.machikoro.client.domain.session

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Test

class DummySessionStorage : SessionStorage {
    override suspend fun read() = null
    override suspend fun write(session: Session) {}
    override suspend fun clear() {}
}

class SessionStorageTest {
    @Test
    fun testDummyImplementation() = runBlocking {
        val storage = DummySessionStorage()
        assertNull(storage.read())
        storage.write(Session("token", "user", 1))
        storage.clear()
    }
}
