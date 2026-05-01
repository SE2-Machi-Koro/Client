package com.machikoro.client.ui.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.machikoro.client.domain.model.state.LoginDialogState
import com.machikoro.client.domain.session.SessionStateHolder
import com.machikoro.client.network.auth.AuthApi
import com.machikoro.client.network.auth.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class LoginDialogViewModel(
    private val authApi: AuthApi,
    private val sessionStateHolder: SessionStateHolder,
) : ViewModel() {
    val state: StateFlow<LoginDialogState>
        get() = mutableState.asStateFlow()

    private val mutableState = MutableStateFlow(LoginDialogState())

    fun usernameChanged(value: String) {
        mutableState.update { it.copy(username = value, errorMessage = null) }
    }

    fun passwordChanged(value: String) {
        mutableState.update { it.copy(password = value, errorMessage = null) }
    }

    fun submit() {
        val current = mutableState.value
        if (!current.canSubmit) return

        mutableState.update { it.copy(submitting = true, errorMessage = null) }

        viewModelScope.launch {
            val result = runCatching {
                authApi.login(LoginRequest(current.username, current.password))
            }
            mutableState.update { previous ->
                result.fold(
                    onSuccess = { response ->
                        sessionStateHolder.signIn(response.sessionToken, response.username)
                        previous.copy(
                            submitting = false,
                            loggedInAs = response.username,
                            errorMessage = null,
                        )
                    },
                    onFailure = { throwable ->
                        previous.copy(
                            submitting = false,
                            errorMessage = throwable.toUserMessage(),
                        )
                    },
                )
            }
        }
    }

    fun reset() {
        mutableState.value = LoginDialogState()
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        is HttpException -> response()?.errorBody()?.string()?.takeIf { it.isNotBlank() }
            ?: "Login failed (HTTP ${code()})"
        is IOException -> "Network error: ${message ?: "could not reach server"}"
        else -> message ?: "Login failed"
    }

    class Factory(
        private val authApi: AuthApi,
        private val sessionStateHolder: SessionStateHolder,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(LoginDialogViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return LoginDialogViewModel(authApi, sessionStateHolder) as T
        }
    }
}
