package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.machikoro.client.ui.theme.ButtonBorderBlue
import com.machikoro.client.ui.theme.PrimaryBlueDark
import com.machikoro.client.ui.theme.TextBlueDark

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
            .background(Color(0xCCC4D3DC).copy(alpha = 0.8f).copy(alpha = 0.8f), shape = OutlinedTextFieldDefaults.shape)
    ) {

        OutlinedTextField(
            value = text,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextBlueDark,
                unfocusedBorderColor = TextBlueDark,
            ),
            onValueChange = {
                text = it
            },
            modifier = Modifier.weight(1f)
                .padding(8.dp),
        )

        IconButton(
            onClick = {
                if (text.isNotBlank() && text.length <= 300) {
                    onSend(text)
                    text = ""
                }
            },
            modifier = Modifier.padding(0.dp, 8.dp, 8.dp, 12.dp)
                .align(Alignment.Bottom)
        ) {

            Icon(
                Icons.Default.Send,
                contentDescription = "Send",
                tint = if (text.isNotBlank() && text.length <= 300) PrimaryBlueDark else ButtonBorderBlue
            )
        }
    }
}
