package com.machikoro.client.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.ui.theme.ButtonColor
import com.machikoro.client.ui.theme.ButtonShadowColor
import com.machikoro.client.ui.theme.ButtonTextColor
import com.machikoro.client.ui.theme.ClientTheme

@Composable
fun ActionButton(label: String, onClick: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .wrapContentSize()
    ) {

        // Bottom shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ButtonShadowColor)
        )

        // Main button
        Button(
            onClick = { onClick?.invoke() },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ButtonColor
            ),

            ) {
            Text(
                text = label,
                color = ButtonTextColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ButtonsPreview() {
    ClientTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Other staff on the top:
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActionButton("Some text", null)
                ActionButton("Some text but long", null)
            }
        }
    }
}