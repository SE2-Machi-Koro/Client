package com.machikoro.client.domain.session

import kotlinx.coroutines.flow.StateFlow

/**
 * Owns the in-memory authenticated-session state. Survives screen rotation
 * but not an app cold-start. Persisting the token across cold-starts is a
 * separate follow-up issue.
 */
interface SessionStateHolder {
    val session: StateFlow<Session?>
    fun signIn(token: String, username: String)
    fun signOut()
}
