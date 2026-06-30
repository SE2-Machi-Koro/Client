package com.machikoro.client.ui.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.machikoro.client.domain.model.state.ChatMessageState
import com.machikoro.client.ui.theme.Black
import com.machikoro.client.ui.theme.CardBlueBackground
import com.machikoro.client.ui.theme.PrimaryBeigeLight
import com.machikoro.client.ui.theme.PrimaryOrange
import com.machikoro.client.ui.theme.ShadowDarkMedium
import com.machikoro.client.ui.theme.TextBlueDark
import com.machikoro.client.ui.theme.TextWhite
import com.machikoro.client.ui.theme.White

@Composable
fun ChatOverlay(
    currentPlayer: String,
    open: Boolean,
    onClose: (() -> Unit)? = null,
    messages: List<ChatMessageState>,
    onSendMessageClick: ((String) -> Unit)? = null
) {

    AnimatedVisibility(
        visible = open,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it }),
        modifier = Modifier.fillMaxSize()
    ) {

        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Background that closes the overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onClose?.invoke()
                    }
            )
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .width(320.dp)
                    .height(450.dp)
                    .align(Alignment.BottomEnd),

                // Semi transparent background
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xCCC4D3DC).copy(alpha = 0.8f)
                )
            ) {

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xCCC4D3DC).copy(alpha = 0.8f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Chat",
                            color = TextBlueDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        onClose?.let {
                            IconButton(
                                onClick = it,
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = TextWhite
                                ),

                                ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close chat",
                                    tint = TextBlueDark
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = PrimaryBeigeLight)

                    LazyColumn(
                        reverseLayout = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(8.dp)
                    ) {

                        items(messages.reversed()) { msg ->
                            Text(
                                text = "${msg.sender}: ${msg.message}",
                                color = TextBlueDark,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }

                    ChatInput(
                        onSend = { text ->
                            onSendMessageClick?.invoke(text)
                        }
                    )
                }
            }
        }
    }
}


@Preview
@Composable
fun prev() {
    ChatOverlay(
        currentPlayer = "Me",
        open = true,
        messages = emptyList<ChatMessageState>(),
        onClose = null,
        onSendMessageClick = null,
    )
}