package com.machikoro.client.domain.model.state

import com.machikoro.client.domain.enums.CardType

/**
 * One owned establishment entry for a player, as carried in authoritative
 * game-state snapshots from the server.
 */
data class PlayerCardState(
    val cardType: CardType,
    val quantity: Int,
)
