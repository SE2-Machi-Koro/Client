package com.machikoro.client.ui.win


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.R
import com.machikoro.client.ui.theme.*
import com.machikoro.client.ui.theme.ClientTheme
import com.machikoro.client.ui.theme.PrimaryBlueDark
import kotlinx.coroutines.delay

@Composable
fun WinScreen() {
    Box(modifier = Modifier.fillMaxSize())
    {

        //Background
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

        //Content
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            //Headline
            Text(
                text = "WINNER SCREEN",
                style = MaterialTheme.typography.headlineLarge,
                color = PrimaryBlueDark
                )
            val list = listOf<String>("Name 1", "Long name", "1", "HsDHDHsssaaassaasasasassaDH") // for testing


            var index by remember { mutableIntStateOf(0) }




            //Player cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            )
            {
                list.forEachIndexed { i, player ->

                    var visible by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        delay(i * 850L)
                        visible = true
                    }

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn() +
                                scaleIn(initialScale = 0.5f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy
                                    ))
                    ) {
                        PlayerProfileCard(player, i + 1)
                    }
                }
            }





            //Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                Button(
                    onClick = { TODO() }
                ) {
                        Text(text = "Finish",)
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

@Composable
fun PlayerProfileCard(name: String, place: Int) {
    val backgroundColors = listOf(
        CardPurpleBackground,
        CardRedBackground,
        CardGreenBackground,
        CardBlueBackground)
    val textColors = listOf(
        CardPurpleText,
        CardRedText,
        CardGreenText,
        CardBlueText)
            Box(
                modifier = Modifier
                    .width(170.dp)
                    .height(200.dp)
                    .border(
                        width = 1.dp,
                        color = textColors.get(place -1),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .background(
                        color = backgroundColors.get(place -1),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(all = 8.dp)
            ) {
                    if (place == 1) {
                        Image(
                            painter = painterResource(id = R.drawable.lobby_host_icon),
                            contentDescription = null,
                            alpha = 0.7f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(0.dp, (-40).dp)
                        )
                    }
                    Text(
                        modifier = Modifier
                            .align(Alignment.TopCenter),
                        text = "$place",
                        color = textColors.get(place - 1),
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = 36.sp,
                    )
                    Image(
                        painter = painterResource(id = R.drawable.login_user_icon),
                        contentDescription = null,
                        alpha = 0.7f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .size(80.dp)
                            .align(Alignment.Center),
                    )
                    Text(
                        text = name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = textColors.get(place - 1),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    )
                }


}


@Preview(showBackground = true)
@Composable
fun WinScreenPreview() {
    ClientTheme {
        WinScreen()
    }
}
