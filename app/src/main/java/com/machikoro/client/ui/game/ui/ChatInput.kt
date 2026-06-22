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
import com.machikoro.client.ui.theme.Black
import com.machikoro.client.ui.theme.ButtonBorderBlue
import com.machikoro.client.ui.theme.PrimaryBlueDark
import com.machikoro.client.ui.theme.TextOnDark

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
            //.padding(8.dp)
            .background(Black, shape = OutlinedTextFieldDefaults.shape)
    ) {

        OutlinedTextField(
            value = text,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedTextColor = TextOnDark,
                focusedTextColor = TextOnDark,
                unfocusedBorderColor = ButtonBorderBlue,
                focusedBorderColor = PrimaryBlueDark,
                unfocusedLabelColor = ButtonBorderBlue,
                focusedLabelColor = PrimaryBlueDark,
            ),
            onValueChange = {
                text = it
            },
            modifier = Modifier.weight(1f)
                .padding(8.dp),
        )

        //Spacer(Modifier.width(8.dp))

        IconButton(
            onClick = {
                if (text.isNotBlank()) {
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
                tint = if (text.isNotBlank()) PrimaryBlueDark else ButtonBorderBlue
            )
        }
    }
}