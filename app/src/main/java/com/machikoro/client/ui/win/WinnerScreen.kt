package com.machikoro.client.ui.win


import androidx.compose.foundation.Image

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.machikoro.client.R
import com.machikoro.client.ui.theme.ClientTheme

@Composable
fun WinScreen() {
    Box(modifier = Modifier.fillMaxSize())
    {
        Image(
            painter = painterResource(id = R.drawable.background_left),
            contentDescription = null,
            modifier = Modifier.align(Alignment.BottomStart).offset(x = (-20).dp, y = 30.dp),
            alpha = 0.3f
        )
        Image(
            painter = painterResource(id = R.drawable.background_right),
            contentDescription = null,
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 15.dp, y = 30.dp),
            alpha = 0.3f
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "WINNER SCREEN",
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                Text("Name 1")
                Text("Name 2")
                Text("Name 3")
                Text("Name 4")
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                Button(
                    onClick = { TODO() }
                ) {
                    Text("Finish")
                }
                Button(
                    onClick = { TODO() }
                ) {
                    Text("Back to lobby")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WinScreenPreview() {
    ClientTheme {
        WinScreen()
    }
}
