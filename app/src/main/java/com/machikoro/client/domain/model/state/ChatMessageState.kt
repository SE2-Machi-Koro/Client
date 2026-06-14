package com.machikoro.client.domain.model.state
/**
 * Represents a chat message in the game lobby or during gameplay.
 *
 * @property sender The name of the player who sent the message.
 * @property message The content of the chat message.
 */
data class ChatMessageState(
    val sender: String,
    val message: String,
)