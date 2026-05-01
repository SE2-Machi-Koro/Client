package com.machikoro.client.domain.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionManager : SessionStateHolder {
    private val mutableSession = MutableStateFlow<Session?>(null)
    override val session: StateFlow<Session?> = mutableSession.asStateFlow()

    override fun signIn(token: String, username: String) {
        mutableSession.value = Session(sessionToken = token, username = username)
    }

    override fun signOut() {
        mutableSession.value = null
    }
}
