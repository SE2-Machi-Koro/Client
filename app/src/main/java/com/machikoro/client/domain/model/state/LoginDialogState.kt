package com.machikoro.client.domain.model.state

data class LoginDialogState(
    val username: String = "",
    val password: String = "",
    val submitting: Boolean = false,
    val errorMessage: String? = null,
    val loggedInAs: String? = null,
) {
    val canSubmit: Boolean
        get() = !submitting &&
            loggedInAs == null &&
            username.isNotBlank() &&
            password.isNotBlank()
}
