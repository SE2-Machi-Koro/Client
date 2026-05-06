package com.machikoro.client.domain.session

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object SessionManager : SessionStateHolder {
    private const val TAG = "SessionManager"

    private val mutableSession = MutableStateFlow<Session?>(null)
    override val session: StateFlow<Session?> = mutableSession.asStateFlow()

    private var storage: SessionStorage? = null
    private val persistenceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun attach(storage: SessionStorage) {
        this.storage = storage
    }

    suspend fun hydrate() {
        if (mutableSession.value != null) return
        val persisted = storage?.read() ?: return
        // Guard against the race where signIn() fired between the read and the
        // set — in that case the in-memory session wins, persisted is stale.
        mutableSession.compareAndSet(expect = null, update = persisted)
    }

    override fun signIn(token: String, username: String) {
        val session = Session(sessionToken = token, username = username)
        mutableSession.value = session
        storage?.let { s ->
            persistenceScope.launch {
                runCatching { s.write(session) }
                    .onFailure { Log.w(TAG, "Failed to persist session", it) }
            }
        }
    }

    override fun signOut() {
        mutableSession.value = null
        storage?.let { s ->
            persistenceScope.launch {
                runCatching { s.clear() }
                    .onFailure { Log.w(TAG, "Failed to clear persisted session", it) }
            }
        }
    }
}
