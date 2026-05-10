package com.machikoro.client.ui.win

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
fun WinScreen() {
    Box(modifier = Modifier.fillMaxSize())
    {
        //Backgroun
        /*
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


         */
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop

        )
        //Content
        Column(
            modifier = Modifier.fillMaxSize().padding(all = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box {
                Text(
                    text = "Game Over",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        drawStyle = Stroke(width = 18f)
                    ),
                    color = Color(0xFFB06207)
                )

                Text(
                    text = "Game Over",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color(0xFFF49E0A)
                )
            }
            val list = listOf<String>("Name 1", "Long name", "1", "HsDHDHsssaaassaasasasassaDH") // for testing


            //Player profiles
            Row(
                modifier = Modifier.fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            )
            {
                list.forEachIndexed { i, player ->
                    var visible by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        delay(300 + i * 500L)
                        visible = true
                    }

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn() + scaleIn(
                            initialScale = 0.9f
                        )
                    ) {
                        PlayerProfileCard(player, i + 1)
                    }
                }
            }


            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                listOf("Finish game", "Back to lobby").forEach { label ->

                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                    ) {

                        // Bottom shadow layer
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(y = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFB97816))
                        )

                        // Main button
                        Button(
                            onClick = { },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF4B343)
                            ),

                        ) {
                            Text(
                                text = label,
                                color = Color(0xFF7A4300),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }





        }
    }
}

@Composable
fun PlayerProfileCard(name: String, place: Int) {

    val backgroundColor = when(place) {
        1 ->  CardPurpleBackground
        2 -> CardRedBackground
        3 -> CardGreenBackground
        4 -> CardBlueBackground
        else -> null
    }

    val textColor = when(place) {
        1 -> CardPurpleText
        2 -> CardRedText
        3 -> CardGreenText
        4 -> CardBlueText
        else -> null
    }

    val image =  when (place) {
        1 -> R.drawable.first_place
        2 -> R.drawable.second_place
        3 -> R.drawable.third_place
        4 -> R.drawable.fourth_place
        else -> null
    }

    Box(
        modifier = Modifier
            .width(175.dp)
            .height(240.dp)
            .padding(top = 14.dp)
        ,
    ) {
        // Crown
        if (place == 1) {
            Image(
                painter = painterResource(id = R.drawable.lobby_host_icon),
                contentDescription = null,
                alpha = 0.7f,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (-40).dp) // above card
                    .zIndex(1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.5f.dp,
                    color = textColor!!,
                    shape = RoundedCornerShape(20.dp)
                )
                .background(
                    color = backgroundColor!!,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = "$place",
                color = textColor,
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 48.sp
            )

            Image(
                painter = painterResource(id = image!!),
                contentDescription = null,
                modifier = Modifier.size(145.dp, 120.dp)
            )

            Text(
                text = name,
                color = textColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
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
