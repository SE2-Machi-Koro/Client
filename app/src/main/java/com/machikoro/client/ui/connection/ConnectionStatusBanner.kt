package com.machikoro.client.ui.connection

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.ui.theme.GreenDark
import com.machikoro.client.ui.theme.GreenLight
import com.machikoro.client.ui.theme.RedDark
import com.machikoro.client.ui.theme.RedLight

@Composable
fun ConnectionStatusBanner(
    state: ConnectionBannerState,
    modifier: Modifier = Modifier,
) {
    val (background, textColor, label) = when (state) {
        ConnectionBannerState.Hidden -> return
        ConnectionBannerState.Disconnected ->
            Triple(RedLight, RedDark, "Connection lost — reconnecting…")
        ConnectionBannerState.Reconnected ->
            Triple(GreenLight, GreenDark, "Reconnected")
    }
    BannerSurface(background = background, textColor = textColor, label = label, modifier = modifier)
}

@Composable
private fun BannerSurface(
    background: Color,
    textColor: Color,
    label: String,
    modifier: Modifier,
) {
    Surface(color = background, modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun ConnectionStatusBannerDisconnectedPreview() {
    ConnectionStatusBanner(state = ConnectionBannerState.Disconnected)
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun ConnectionStatusBannerReconnectedPreview() {
    ConnectionStatusBanner(state = ConnectionBannerState.Reconnected)
}
