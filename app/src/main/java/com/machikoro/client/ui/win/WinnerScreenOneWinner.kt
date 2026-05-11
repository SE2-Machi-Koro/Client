package com.machikoro.client.ui.win

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.machikoro.client.R
import com.machikoro.client.ui.theme.*
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.ui.theme.PrimaryBlueDark
import kotlinx.coroutines.delay

@Composable
fun WinScreenOneWinner(winnerName: String, roundsNumber: Int) {
    Box(modifier = Modifier.fillMaxSize())
    {
        Image(
            painter = painterResource(id = R.drawable.frame),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        //Content
        Column(
            modifier = Modifier.fillMaxSize().padding(all = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            Header("Congratulations to...")
            //Player cards
            Row(
                modifier = Modifier.fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            )
            {
                AnimatedItem(delayMillis = 1000, animationType = AnimationType.Bounce) {
                    PlayerProfileCard(winnerName, 1)
                }
                AnimatedItem(delayMillis = 2000, animationType = AnimationType.Fade) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "won the game in \n$roundsNumber rounds!",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            AnimatedItem(delayMillis = 5000, animationType = AnimationType.SlideUp) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionButton("Back to home screen", null)
                    ActionButton("Play again", null)
                }
            }
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


    @Preview(showBackground = true)
    @Composable
    fun WinScreenOnePlayerPreview() {
        ClientTheme {
            WinScreenOneWinner(winnerName = "Alice", roundsNumber = 5)
        }
    }

enum class AnimationType {
    Fade,
    Scale,
    SlideUp,
    Bounce
}

