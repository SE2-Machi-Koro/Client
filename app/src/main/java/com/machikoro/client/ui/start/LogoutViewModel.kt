package com.machikoro.client.ui.start

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.machikoro.client.domain.model.state.LogoutState
import com.machikoro.client.domain.session.SessionStateHolder
import com.machikoro.client.network.auth.AuthApi
import com.machikoro.client.network.auth.LogoutRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LogoutViewModel(
    private val authApi: AuthApi,
    private val sessionStateHolder: SessionStateHolder,
) : ViewModel() {
    val state: StateFlow<LogoutState>
        get() = mutableState.asStateFlow()

    private val mutableState = MutableStateFlow(LogoutState())

    fun submit() {
        val current = sessionStateHolder.session.value ?: return
        if (mutableState.value.submitting) return

        mutableState.update { it.copy(submitting = true) }

        viewModelScope.launch {
            // Tolerate network failure: clear the local session regardless so the user
            // isn't stranded on a bad connection. The server's stale-token endpoint
            // already silently no-ops if the token is later replayed. Log only —
            // by the time we'd surface a UI error the start screen has already
            // flipped to the unauthenticated layout, so there is no visible host
            // for an error banner.
            runCatching { authApi.logout(LogoutRequest(current.sessionToken)) }
                .onFailure { Log.w(TAG, "Logout API call failed; clearing local session anyway", it) }

            sessionStateHolder.signOut()
            mutableState.update { it.copy(submitting = false) }
        }
    }

    class Factory(
        private val authApi: AuthApi,
        private val sessionStateHolder: SessionStateHolder,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(LogoutViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return LogoutViewModel(authApi, sessionStateHolder) as T
        }
    }

    companion object {
        private const val TAG = "LogoutViewModel"
    }
}
