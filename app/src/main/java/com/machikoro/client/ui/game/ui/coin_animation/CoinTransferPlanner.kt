package com.machikoro.client.ui.game.ui.coin_animation

private data class RemainingCoinChange(
    val playerId: Int,
    var amount: Int,
)

fun buildCoinTransfers(
    previousCoins: Map<Int, Int>,
    currentCoins: Map<Int, Int>,
): List<CoinTransferUi> {
    val playerIds = (previousCoins.keys + currentCoins.keys).sorted()
    val payers = playerIds.mapNotNull { playerId ->
        val delta = currentCoins.getOrDefault(playerId, 0) -
            previousCoins.getOrDefault(playerId, 0)
        if (delta < 0) RemainingCoinChange(playerId, -delta) else null
    }.toMutableList()
    val receivers = playerIds.mapNotNull { playerId ->
        val delta = currentCoins.getOrDefault(playerId, 0) -
            previousCoins.getOrDefault(playerId, 0)
        if (delta > 0) RemainingCoinChange(playerId, delta) else null
    }.toMutableList()

    val transfers = mutableListOf<CoinTransferUi>()
    var payerIndex = 0
    var receiverIndex = 0

    while (payerIndex < payers.size && receiverIndex < receivers.size) {
        val payer = payers[payerIndex]
        val receiver = receivers[receiverIndex]
        val amount = minOf(payer.amount, receiver.amount)

        transfers += CoinTransferUi(
            fromPlayerId = payer.playerId,
            toPlayerId = receiver.playerId,
            amount = amount,
        )

        payer.amount -= amount
        receiver.amount -= amount
        if (payer.amount == 0) payerIndex++
        if (receiver.amount == 0) receiverIndex++
    }

    while (receiverIndex < receivers.size) {
        val receiver = receivers[receiverIndex++]
        if (receiver.amount > 0) {
            transfers += CoinTransferUi(
                fromPlayerId = null,
                toPlayerId = receiver.playerId,
                amount = receiver.amount,
            )
        }
    }

    while (payerIndex < payers.size) {
        val payer = payers[payerIndex++]
        if (payer.amount > 0) {
            transfers += CoinTransferUi(
                fromPlayerId = payer.playerId,
                toPlayerId = null,
                amount = payer.amount,
            )
        }
    }

    return transfers
}
