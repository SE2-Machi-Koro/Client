package com.machikoro.client.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.R
import com.machikoro.client.ui.theme.ButtonColor
import com.machikoro.client.ui.theme.ButtonShadowColor
import com.machikoro.client.ui.theme.ButtonTextColor
import com.machikoro.client.ui.theme.HeaderFill
import com.machikoro.client.ui.theme.HeaderStroke
import kotlinx.coroutines.delay

// Shared UI components for the Machikoro client app, including background, header, action button, and animated item composables.


/*
This file contains shared UI components for the Machikoro client app, including:
- Background: A composable that displays a background image.
- Header: A composable that displays a stylized header text.
- ActionButton: A composable that displays a stylized button with a shadow effect.
- AnimatedItem: A composable that animates its content with different animation types (fade, scale, slide up, bounce) based on a delay.
- RegularInfoText: A composable that displays regular informational text with specific styling.
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
                AnimationType.SlideUp -> if (visible) 0f else 300f
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


enum class AnimationType {
    Fade,
    Scale,
    SlideUp,
    Bounce
}