package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Composable
fun ChatInput(
    onSend: (String) -> Unit
) {

    var text by remember {
        mutableStateOf("")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {

        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
            },
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(8.dp))

        IconButton(
            onClick = {

                if (text.isNotBlank()) {

                    onSend(text)

                    text = ""
                }
            }
        ) {

            Icon(
                Icons.Default.Send,
                contentDescription = "Send"
            )
        }
    }
}