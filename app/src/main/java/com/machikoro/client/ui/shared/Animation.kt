package com.machikoro.client.ui.shared

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Displays content with an entry animation after a delay.
 * E.g. first element in n millis delay, next 2n, 3n etc.
 *
 * The animation starts once [delayMillis] has passed.
 * Different animation styles can be selected using [animationType].
 *
 * Supported animations:
 * - [AnimationType.Fade] - fades content in
 * - [AnimationType.Scale] - scales content from 0 to full size
 * - [AnimationType.SlideUp] - slides content upward into view
 * - [AnimationType.Bounce] - bounces content into place
 *
 * @param delayMillis Delay in milliseconds before the animation starts.
 * @param animationType Type of animation to apply.
 * Defaults to [AnimationType.Fade].
 * @param content Composable content to animate.
 */

@Composable
fun AnimatedItem(
    delayMillis: Int,
    animationType: AnimationType = AnimationType.Fade,
    content: @Composable () -> Unit
) {

    var visible by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        label = ""
    )

    val scale by animateFloatAsState(
        targetValue =
            when (animationType) {
                AnimationType.Scale,
                AnimationType.Bounce -> if (visible) 1f else 0.0f

                else -> 1f
            },
        label = ""
    )

    val translationY by animateFloatAsState(
        targetValue =
            when (animationType) {
                AnimationType.SlideUp -> if (visible) 0f else 1000f
                AnimationType.Bounce -> if (visible) 0f else 500f
                else -> 0f
            },
        label = ""
    )


    Box(
        modifier = Modifier.graphicsLayer {

            this.alpha =
                if (animationType == AnimationType.Fade ||
                    visible
                ) alpha else 1f

            scaleX = scale
            scaleY = scale

            this.translationY = translationY
            this.translationX = translationX
        }
    ) {
        content()
    }
}

enum class AnimationType {
    Fade,
    Scale,
    SlideUp,
    Bounce
}

@Preview(showBackground = true)
@Composable
private fun AnimatedItemPreview() {
    Box(Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center){

        Row(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            AnimatedItem(
                delayMillis = 500,
                animationType = AnimationType.Fade
            ) {
                DemoShape("Fade")
            }

            AnimatedItem(
                delayMillis = 1000,
                animationType = AnimationType.Scale
            ) {
                DemoShape("Scale")
            }

            AnimatedItem(
                delayMillis = 1500,
                animationType = AnimationType.SlideUp
            ) {
                DemoShape("SlideUp")
            }

            AnimatedItem(
                delayMillis = 2000,
                animationType = AnimationType.Bounce
            ) {
                DemoShape("Bounce")
            }
        }
    }
}

@Composable
private fun DemoShape(
    text: String
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .background(Color(0xFF5C6BC0)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White
        )
    }
}