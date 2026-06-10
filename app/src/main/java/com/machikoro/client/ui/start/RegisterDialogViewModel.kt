package com.machikoro.client.ui.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.machikoro.client.domain.model.state.RegisterDialogState
import com.machikoro.client.network.auth.AuthApi
import com.machikoro.client.network.toClientError
import com.machikoro.client.network.toUserMessage
import com.machikoro.client.network.auth.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterDialogViewModel(
    private val authApi: AuthApi,
) : ViewModel() {
    val state: StateFlow<RegisterDialogState>
        get() = mutableState.asStateFlow()

    private val mutableState = MutableStateFlow(RegisterDialogState())

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
                authApi.register(RegisterRequest(current.username, current.password))
            }
            mutableState.update { previous ->
                result.fold(
                    onSuccess = { response ->
                        previous.copy(
                            submitting = false,
                            registeredUsername = response.username,
                            errorMessage = null,
                        )
                    },
                    onFailure = { throwable ->
                        previous.copy(
                            submitting = false,
                            errorMessage = throwable.toClientError().toUserMessage(),
                        )
                    },
                )
            }
        }
    }

    fun reset() {
        mutableState.value = RegisterDialogState()
    }

    class Factory(
        private val authApi: AuthApi,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(RegisterDialogViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return RegisterDialogViewModel(authApi) as T
        }
    }
}
