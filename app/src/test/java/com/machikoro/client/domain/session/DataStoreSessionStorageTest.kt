package com.machikoro.client.domain.session

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSessionStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var storage: DataStoreSessionStorage

    @Before
    fun setUp() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(tempFolder.root, "session.preferences_pb") },
        )
        storage = DataStoreSessionStorage(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `read returns null when nothing has been written`() = runBlocking {
        assertNull(storage.read())
    }

    @Test
    fun `write then read returns the same session`() = runBlocking {
        val session = Session(sessionToken = "uuid-123", username = "alice")

        storage.write(session)

        assertEquals(session, storage.read())
    }

    @Test
    fun `clear removes the persisted session`() = runBlocking {
        storage.write(Session(sessionToken = "uuid-123", username = "alice"))

        storage.clear()

        assertNull(storage.read())
    }

    @Test
    fun `write overwrites previous session`() = runBlocking {
        storage.write(Session(sessionToken = "old", username = "alice"))
        storage.write(Session(sessionToken = "new", username = "bob"))

        assertEquals(Session("new", "bob"), storage.read())
    }
}
