package com.machikoro.client.ui.game.ui.coin_animation

import org.junit.Assert.assertEquals
import org.junit.Test

class CoinTransferPlannerTest {

    @Test
    fun pairsOnePayerWithMultipleReceivers() {
        val transfers = buildCoinTransfers(
            previousCoins = mapOf(1 to 10, 2 to 3, 3 to 4),
            currentCoins = mapOf(1 to 6, 2 to 5, 3 to 6),
        )

        assertEquals(
            listOf(
                CoinTransferUi(fromPlayerId = 1, toPlayerId = 2, amount = 2),
                CoinTransferUi(fromPlayerId = 1, toPlayerId = 3, amount = 2),
            ),
            transfers,
        )
    }

    @Test
    fun pairsMultiplePayersWithOneReceiver() {
        val transfers = buildCoinTransfers(
            previousCoins = mapOf(1 to 5, 2 to 7, 3 to 1),
            currentCoins = mapOf(1 to 3, 2 to 4, 3 to 6),
        )

        assertEquals(
            listOf(
                CoinTransferUi(fromPlayerId = 1, toPlayerId = 3, amount = 2),
                CoinTransferUi(fromPlayerId = 2, toPlayerId = 3, amount = 3),
            ),
            transfers,
        )
    }

    @Test
    fun createsBankTransferForUnmatchedGain() {
        val transfers = buildCoinTransfers(
            previousCoins = mapOf(1 to 2, 2 to 3),
            currentCoins = mapOf(1 to 2, 2 to 7),
        )

        assertEquals(
            listOf(
                CoinTransferUi(fromPlayerId = null, toPlayerId = 2, amount = 4),
            ),
            transfers,
        )
    }

    @Test
    fun combinesPlayerTransferAndBankRemainder() {
        val transfers = buildCoinTransfers(
            previousCoins = mapOf(1 to 5, 2 to 1),
            currentCoins = mapOf(1 to 3, 2 to 6),
        )

        assertEquals(
            listOf(
                CoinTransferUi(fromPlayerId = 1, toPlayerId = 2, amount = 2),
                CoinTransferUi(fromPlayerId = null, toPlayerId = 2, amount = 3),
            ),
            transfers,
        )
    }

    @Test
    fun createsBankTransferForUnmatchedLoss() {
        val transfers = buildCoinTransfers(
            previousCoins = mapOf(1 to 6, 2 to 3),
            currentCoins = mapOf(1 to 2, 2 to 3),
        )

        assertEquals(
            listOf(
                CoinTransferUi(fromPlayerId = 1, toPlayerId = null, amount = 4),
            ),
            transfers,
        )
    }

    @Test
    fun combinesPlayerTransferAndBankLossRemainder() {
        val transfers = buildCoinTransfers(
            previousCoins = mapOf(1 to 8, 2 to 1),
            currentCoins = mapOf(1 to 3, 2 to 4),
        )

        assertEquals(
            listOf(
                CoinTransferUi(fromPlayerId = 1, toPlayerId = 2, amount = 3),
                CoinTransferUi(fromPlayerId = 1, toPlayerId = null, amount = 2),
            ),
            transfers,
        )
    }

    @Test
    fun returnsNoTransfersWhenBalancesDidNotChange() {
        val balances = mapOf(1 to 4, 2 to 8)

        assertEquals(
            emptyList<CoinTransferUi>(),
            buildCoinTransfers(
                previousCoins = balances,
                currentCoins = balances,
            ),
        )
    }
}
