package com.machikoro.client.ui.win

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.machikoro.client.R
import com.machikoro.client.ui.theme.CardBlueBackground
import com.machikoro.client.ui.theme.CardBlueText
import com.machikoro.client.ui.theme.CardGreenBackground
import com.machikoro.client.ui.theme.CardGreenText
import com.machikoro.client.ui.theme.CardPurpleBackground
import com.machikoro.client.ui.theme.CardPurpleText
import com.machikoro.client.ui.theme.CardRedBackground
import com.machikoro.client.ui.theme.CardRedText

/*
This file contains the PlayerProfileCard composable, which displays a player's profile information
 in a stylized card format. The card's appearance (background color, text color, and image)
 changes based on the player's placement (1st, 2nd, 3rd, or 4th). The card also includes
 a crown image for the first-place player.
 */
@Composable
fun PlayerProfileCard(name: String, place: Int) {

    val backgroundColor = when(place) {
        1 -> CardPurpleBackground
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
                modifier = Modifier.fillMaxWidth()
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
