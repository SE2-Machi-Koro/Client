package com.machikoro.client.ui.game.ui.coin_animation

data class CoinTransferUi(
    val fromPlayerId: Int?,
    val toPlayerId: Int?,
    val amount: Int
)

enum class CoinChangeHighlight {
    NONE,
    GAIN,
    LOSS
}