package com.machikoro.client.ui.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import com.machikoro.client.domain.model.state.ChatMessageState

@Composable
fun ChatOverlay(
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
            contentAlignment = Alignment.BottomEnd
        ) {

            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .width(320.dp)
                    .height(450.dp),

                // Semi transparent background
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.65f)
                )
            ) {

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {

                    Text(
                        text = "Chat",
                        color = Color.White,
                        modifier = Modifier.padding(16.dp)
                    )

                    Divider()

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)
                    ) {

                        items(messages) { msg ->
                            Text(
                                text = "${msg.sender}: ${msg.message}",
                                color = Color.White,
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