package com.machikoro.client.ui.start

import androidx.compose.foundation.Image
import com.machikoro.client.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.domain.model.state.LoginDialogState
import com.machikoro.client.ui.theme.ButtonColor
import com.machikoro.client.ui.theme.ButtonTextColor
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.ui.theme.PanelBackgroundBeige
import com.machikoro.client.ui.theme.TextBlueDark

@Composable
fun LoginDialog(
    state: LoginDialogState,
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
        containerColor = PanelBackgroundBeige,
        shape = RoundedCornerShape(18.dp),
        title = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.decor_screw),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.TopStart)
                        .offset(x = (-8).dp, y = (-10).dp)
                )

                Image(
                    painter = painterResource(id = R.drawable.decor_screw),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-10).dp)
                )

                Text(
                    text = "LOGIN",
                    color = TextBlueDark,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        },
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
                    enabled = !state.submitting && state.loggedInAs == null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PanelBackgroundBeige,
                        unfocusedContainerColor = PanelBackgroundBeige,
                        disabledContainerColor = PanelBackgroundBeige.copy(alpha = 0.7f),
                        focusedBorderColor = TextBlueDark,
                        unfocusedBorderColor = TextBlueDark.copy(alpha = 0.45f),
                        focusedLabelColor = TextBlueDark,
                        unfocusedLabelColor = TextBlueDark.copy(alpha = 0.6f),
                        cursorColor = TextBlueDark
                    ),
                )
                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    singleLine = true,
                    enabled = !state.submitting && state.loggedInAs == null,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PanelBackgroundBeige,
                        unfocusedContainerColor = PanelBackgroundBeige,
                        disabledContainerColor = PanelBackgroundBeige.copy(alpha = 0.7f),
                        focusedBorderColor = TextBlueDark,
                        unfocusedBorderColor = TextBlueDark.copy(alpha = 0.45f),
                        focusedLabelColor = TextBlueDark,
                        unfocusedLabelColor = TextBlueDark.copy(alpha = 0.6f),
                        cursorColor = TextBlueDark
                    ),
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
                if (state.loggedInAs != null) {
                    Text(
                        text = "Logged in as ${state.loggedInAs}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            if (state.loggedInAs != null) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonColor,
                        contentColor = ButtonTextColor
                    )
                ) {
                    Text("Close", fontWeight = FontWeight.ExtraBold)
                }
            } else {
                Button(
                    onClick = onSubmit,
                    enabled = state.canSubmit,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonColor,
                        contentColor = ButtonTextColor,
                        disabledContainerColor = ButtonColor.copy(alpha = 0.45f),
                        disabledContentColor = ButtonTextColor.copy(alpha = 0.45f)
                    )
                ) {
                    Text(if (state.submitting) "Logging in…" else "Login")
                }
            }
        },
        dismissButton = {
            if (state.loggedInAs == null) {
                TextButton(onClick = onDismiss, enabled = !state.submitting) {
                    Text("Cancel",
                    color = TextBlueDark,
                    fontWeight = FontWeight.ExtraBold)
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun LoginDialogEmptyPreview() {
    ClientTheme {
        LoginDialog(
            state = LoginDialogState(),
            onUsernameChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginDialogErrorPreview() {
    ClientTheme {
        LoginDialog(
            state = LoginDialogState(
                username = "alice",
                password = "wrong",
                errorMessage = "Invalid username or password",
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
private fun LoginDialogSuccessPreview() {
    ClientTheme {
        LoginDialog(
            state = LoginDialogState(loggedInAs = "alice"),
            onUsernameChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onDismiss = {},
        )
    }
}
