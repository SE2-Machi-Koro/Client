package com.machikoro.client.ui.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.domain.model.state.RegisterDialogState
import com.machikoro.client.ui.theme.ClientTheme

@Composable
fun RegisterDialog(
    state: RegisterDialogState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text("Register") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = state.username,
                    onValueChange = onUsernameChange,
                    label = { Text("Username") },
                    singleLine = true,
                    enabled = !state.submitting && state.registeredUsername == null,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    singleLine = true,
                    enabled = !state.submitting && state.registeredUsername == null,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.Visibility
                                },
                                contentDescription = if (passwordVisible) {
                                    "Hide password"
                                } else {
                                    "Show password"
                                },
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (state.registeredUsername != null) {
                    Text(
                        text = "Registered as ${state.registeredUsername}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            if (state.registeredUsername != null) {
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            } else {
                Button(
                    onClick = onSubmit,
                    enabled = state.canSubmit,
                ) {
                    Text(if (state.submitting) "Registering…" else "Register")
                }
            }
        },
        dismissButton = {
            if (state.registeredUsername == null) {
                TextButton(onClick = onDismiss, enabled = !state.submitting) {
                    Text("Cancel")
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun RegisterDialogEmptyPreview() {
    ClientTheme {
        RegisterDialog(
            state = RegisterDialogState(),
            onUsernameChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterDialogErrorPreview() {
    ClientTheme {
        RegisterDialog(
            state = RegisterDialogState(
                username = "alice",
                password = "hunter2",
                errorMessage = "Username 'alice' is already taken",
            ),
            onUsernameChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterDialogSuccessPreview() {
    ClientTheme {
        RegisterDialog(
            state = RegisterDialogState(registeredUsername = "alice"),
            onUsernameChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onDismiss = {},
        )
    }
}
