package com.machikoro.client.ui.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.R
import com.machikoro.client.ui.theme.ClientTheme

/**
 * Displays a fullscreen background image.
 *
 * The image fills the available screen size and is cropped
 * to maintain its aspect ratio.
 *
 * By default, a wooden background is used.
 *
 * @param painterResource Drawable resource id of the background image.
 */

@Composable
fun Background(painterResource: Int = R.drawable.background_wood) {
    Image(
        painter = painterResource(id = painterResource),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}

@Preview(showBackground = true)
@Composable
fun BackgroundPreview() {
    Background()
}
