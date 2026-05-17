package com.machikoro.client.ui.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.ui.theme.HeaderFill
import com.machikoro.client.ui.theme.HeaderStroke

@Composable
fun RegularInfoText(label: String) = (
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
)

/**
 * Displays a styled header text with an outline effect.
 *
 * The header is rendered using two layered texts:
 * - a stroke layer for the border
 * - a fill layer for the main text
 *
 * @param label Text displayed inside the header.
 */

@Composable
fun Header(label: String) {
    Box {
        //Stroke
        Text(
            text = label,
            style = MaterialTheme.typography.headlineLarge.copy(
                drawStyle = Stroke(width = 12f)
            ),
            fontSize = 42.sp,
            color = HeaderStroke
        )

        Text(
            text = label,
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 42.sp,
            color = HeaderFill
        )
    }
}


@Preview(showBackground = true)
@Composable
fun BackgroundHeaderTextPreview() {
    ClientTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Background()
            // Other staff on the top:
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Header("Machi Koro")
                RegularInfoText("Some preview text. Lorem ipsum blah blah")
            }
        }
    }
}