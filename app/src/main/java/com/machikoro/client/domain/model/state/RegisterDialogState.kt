package com.machikoro.client.domain.model.state

data class RegisterDialogState(
    val username: String = "",
    val password: String = "",
    val submitting: Boolean = false,
    val errorMessage: String? = null,
    val registeredUsername: String? = null,
) {
    val canSubmit: Boolean
        get() = !submitting &&
            registeredUsername == null &&
            username.isNotBlank() &&
            password.isNotBlank()
}
