package com.machikoro.client.ui.shared

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO: adjust font size and line length upon finished game screen
/**
 * Displays a countdown timer with a decreasing progress line.
 *
 * The line decreases smoothly over time while the remaining
 * seconds are shown on the right side.
 *
 * The timer turns red when there are 5 seconds or less remaining.
 *
 * @param totalTime Total duration of the timer in seconds.
 */
@Composable
fun DecreasingLineTimer(
    totalTime: Int
) {

    var timeLeft by remember {
        mutableIntStateOf(totalTime)
    }

    val progress = remember {
        Animatable(1f)
    }

    // red when <= 5
    val color =
        if (timeLeft <= 5) Color.Red
        else Color.White

    LaunchedEffect(Unit) {

        // smooth line animation
        launch {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = totalTime * 1000,
                    easing = LinearEasing
                )
            )
        }

        // countdown
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        // line
        Box(
            modifier = Modifier
                .width(220.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Gray)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.value)
                    .fillMaxHeight()
                    .background(color)
            )
        }

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        // number
        Text(
            text = "$timeLeft",
            color = color,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(32.dp)
        )
    }
}

@Preview
@Composable
fun Timer() {
    Box(Modifier.fillMaxSize().background(Color.Gray),
        contentAlignment = Alignment.Center) {
        DecreasingLineTimer(15)
    }
}