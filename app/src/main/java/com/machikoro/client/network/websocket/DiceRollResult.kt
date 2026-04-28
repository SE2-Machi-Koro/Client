package com.machikoro.client.network.websocket

data class DiceRollResult(
    val dice: List<Int>,
    val total: Int
)
