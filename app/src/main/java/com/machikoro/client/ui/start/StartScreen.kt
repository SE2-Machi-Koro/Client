package com.machikoro.client.ui.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.ui.theme.ClientTheme

@Composable
fun StartScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Machi Koro Client",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Connection status: waiting for WebSocket integration",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Lobby/start: placeholder",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StartScreenPreview() {
    ClientTheme {
        StartScreen()
    }
}
