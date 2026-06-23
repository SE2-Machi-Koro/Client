package com.machikoro.client.ui.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.text.font.FontWeight
import com.machikoro.client.domain.model.state.ChatMessageState
import com.machikoro.client.ui.theme.Black
import com.machikoro.client.ui.theme.CardBlueBackground
import com.machikoro.client.ui.theme.PrimaryBeigeLight
import com.machikoro.client.ui.theme.ShadowDarkMedium
import com.machikoro.client.ui.theme.TextWhite

@Composable
fun ChatOverlay(
    currentPlayer: String,
    open: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    messages: List<ChatMessageState>,
    onSendMessageClick: (String) -> Unit
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
                        onClose()
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
                    containerColor = Black.copy(alpha = 0.65f)
                )
            ) {

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ShadowDarkMedium)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chat",
                            color = PrimaryBeigeLight,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(
                            onClick = onClose,
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = TextWhite
                            ),

                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close chat",
                                tint = PrimaryBeigeLight
                            )
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
                                color = if(msg.sender == currentPlayer) CardBlueBackground else PrimaryBeigeLight,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }

                    ChatInput(
                        onSend = { text ->
                            onSendMessageClick(text)
                        }
                    )
                }
            }
        }
    }
}