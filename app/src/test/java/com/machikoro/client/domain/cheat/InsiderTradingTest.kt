package com.machikoro.client.domain.cheat

import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PlayerCardState
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InsiderTradingTest {

    private fun me(coins: Int, id: String = "1") = PlayerCoinState(
        id = id,
        displayName = "me",
        coins = coins,
        isCurrentPlayer = true,
        isActivePlayer = true,
    )

    private fun state(
        player: PlayerCoinState,
        marketplace: Map<CardType, Int>,
        playerLandmarks: Map<Int, List<PlayerLandmarkState>> = emptyMap(),
        playerCards: Map<Int, List<PlayerCardState>> = emptyMap(),
        opponents: Int = 1,
    ): GameScreenState {
        val others = (0 until opponents).map { idx ->
            PlayerCoinState(id = "${100 + idx}", displayName = "opp$idx", coins = 99)
        }
        return GameScreenState.initial().copy(
            players = listOf(player) + others,
            marketplace = marketplace,
            playerLandmarks = playerLandmarks,
            playerCards = playerCards,
        )
    }

    // --- activationProbability ---

    @Test
    fun oneDieProbabilityIsOneSixthPerNumber() {
        assertEquals(1.0 / 6.0, activationProbability(setOf(1), twoDice = false), 1e-9)
        assertEquals(2.0 / 6.0, activationProbability(setOf(2, 3), twoDice = false), 1e-9)
        // 9 is unreachable with a single die.
        assertEquals(0.0, activationProbability(setOf(9), twoDice = false), 1e-9)
    }

    @Test
    fun twoDiceProbabilityFollowsSumDistribution() {
        assertEquals(6.0 / 36.0, activationProbability(setOf(7), twoDice = true), 1e-9)
        assertEquals(1.0 / 36.0, activationProbability(setOf(2), twoDice = true), 1e-9)
        // 1 is impossible with two dice.
        assertEquals(0.0, activationProbability(setOf(1), twoDice = true), 1e-9)
    }

    @Test
    fun twoDiceDistributionSumsToOne() {
        val total = (2..12).sumOf { activationProbability(setOf(it), twoDice = true) }
        assertEquals(1.0, total, 1e-9)
    }

    // --- recommendBestBuy ---

    @Test
    fun recommendsHighestExpectedValueAffordableCard() {
        // Convenience Store ([4], pays 3 -> EV 0.5) beats Wheat Field ([1], pays 1 -> EV 0.167).
        val s = state(
            player = me(coins = 5),
            marketplace = mapOf(CardType.WHEAT_FIELD to 6, CardType.CONVENIENCE_STORE to 6),
        )
        assertEquals(CardType.CONVENIENCE_STORE, recommendBestBuy(s, s.players.first()))
    }

    @Test
    fun returnsNullWhenNothingAffordable() {
        val s = state(player = me(coins = 1), marketplace = mapOf(CardType.MINE to 6)) // Mine costs 6
        assertNull(recommendBestBuy(s, s.players.first()))
    }

    @Test
    fun returnsNullWhenMarketplaceEmpty() {
        val s = state(player = me(coins = 10), marketplace = emptyMap())
        assertNull(recommendBestBuy(s, s.players.first()))
    }

    @Test
    fun skipsCardsUnreachableWithOneDie() {
        // Mine activates on 9 — impossible on one die, so EV 0 and not recommended even if affordable.
        val s = state(player = me(coins = 6), marketplace = mapOf(CardType.MINE to 6))
        assertNull(recommendBestBuy(s, s.players.first()))
    }

    @Test
    fun trainStationUnlocksTwoDiceCards() {
        val s = state(
            player = me(coins = 6),
            marketplace = mapOf(CardType.MINE to 6),
            playerLandmarks = mapOf(
                1 to listOf(PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true)),
            ),
        )
        assertEquals(CardType.MINE, recommendBestBuy(s, s.players.first()))
    }

    @Test
    fun perIconPayoutCountsOwnedCards() {
        // Cheese Factory ([7]) pays 3 per owned Ranch; with Train Station (two dice) and 2 ranches
        // EV = 6/36 * 6 = 1.0, beating a lone Bakery.
        val s = state(
            player = me(coins = 6),
            marketplace = mapOf(CardType.CHEESE_FACTORY to 6, CardType.BAKERY to 6),
            playerLandmarks = mapOf(
                1 to listOf(PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true)),
            ),
            playerCards = mapOf(1 to listOf(PlayerCardState(CardType.RANCH, quantity = 2))),
        )
        assertEquals(CardType.CHEESE_FACTORY, recommendBestBuy(s, s.players.first()))
    }

    @Test
    fun cheeseFactoryNotRecommendedWithoutRanches() {
        // No ranches -> Cheese Factory payout 0 -> excluded; Bakery (EV > 0) wins instead.
        val s = state(
            player = me(coins = 6),
            marketplace = mapOf(CardType.CHEESE_FACTORY to 6, CardType.BAKERY to 6),
            playerLandmarks = mapOf(
                1 to listOf(PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true)),
            ),
        )
        assertEquals(CardType.BAKERY, recommendBestBuy(s, s.players.first()))
    }

    @Test
    fun stadiumValuedPerOpponent() {
        // Stadium ([6]) pays 2 per opponent; with 2 opponents it has positive EV and,
        // as the only affordable card, is recommended.
        val s = state(
            player = me(coins = 6),
            marketplace = mapOf(CardType.STADIUM to 6),
            opponents = 2,
        )
        assertEquals(CardType.STADIUM, recommendBestBuy(s, s.players.first()))
    }

    // --- shake threshold ---

    @Test
    fun isShakeThresholdsOnMagnitude() {
        assertFalse(isShake(accelerationMagnitude(0f, 9.81f, 0f))) // device at rest
        assertTrue(isShake(accelerationMagnitude(12f, 9.81f, 6f)))  // sharp movement
    }
}
