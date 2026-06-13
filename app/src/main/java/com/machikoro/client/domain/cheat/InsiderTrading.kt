package com.machikoro.client.domain.cheat

import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.model.shop.CardDefinitions
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PlayerCoinState
import kotlin.math.sqrt

/**
 * "Insider Trading" cheat heuristic (client issue #203).
 *
 * Pure analysis over the local [GameScreenState] snapshot: given the local
 * player, pick the affordable, in-stock marketplace establishment with the
 * highest simplified single-roll expected value. Lives in `domain/` (no Android
 * dependencies, fully unit-tested).
 *
 * v1 simplifications (intentionally rough — see issue #203, "refine later"):
 *  - Evaluates only the player's own next roll; it ignores that blue cards also
 *    pay on opponents' rolls and red cards pay on opponents' rolls, so it leans
 *    toward green / your-turn cards.
 *  - Train Station built ⇒ always two dice (doesn't model choosing one die).
 *  - Business Center (a card swap) is left unvalued.
 *  - Landmarks are not recommended in v1 (only marketplace establishments).
 */

/** How a card's payout is derived for the EV estimate. */
private sealed interface Payout {
    fun amount(ownedCounts: Map<CardType, Int>, opponentCount: Int): Int

    /** A flat payout independent of board state. */
    data class Fixed(val value: Int) : Payout {
        override fun amount(ownedCounts: Map<CardType, Int>, opponentCount: Int) = value
    }

    /** Pays [per] for each owned card whose type is in [iconCards] (factories). */
    data class PerOwnedIcon(val per: Int, val iconCards: Set<CardType>) : Payout {
        override fun amount(ownedCounts: Map<CardType, Int>, opponentCount: Int) =
            per * iconCards.sumOf { ownedCounts[it] ?: 0 }
    }

    /** Pays [per] for each opponent (Stadium). */
    data class PerOpponent(val per: Int) : Payout {
        override fun amount(ownedCounts: Map<CardType, Int>, opponentCount: Int) =
            per * opponentCount
    }
}

private data class Economics(val payout: Payout)

/** Payout logic for the EV estimate; card metadata comes from [CardDefinitions]. */
private val establishmentEconomics: Map<CardType, Economics> = mapOf(
    CardType.WHEAT_FIELD to Economics(Payout.Fixed(1)),
    CardType.RANCH to Economics(Payout.Fixed(1)),
    CardType.BAKERY to Economics(Payout.Fixed(1)),
    CardType.CAFE to Economics(Payout.Fixed(1)),
    CardType.CONVENIENCE_STORE to Economics(Payout.Fixed(3)),
    CardType.FOREST to Economics(Payout.Fixed(1)),
    CardType.CHEESE_FACTORY to Economics(Payout.PerOwnedIcon(3, setOf(CardType.RANCH))),
    CardType.FURNITURE_FACTORY to Economics(Payout.PerOwnedIcon(3, setOf(CardType.FOREST, CardType.MINE))),
    CardType.MINE to Economics(Payout.Fixed(5)),
    CardType.FAMILY_RESTAURANT to Economics(Payout.Fixed(2)),
    CardType.APPLE_ORCHARD to Economics(Payout.Fixed(3)),
    CardType.FRUIT_AND_VEGETABLE_MARKET to Economics(
        Payout.PerOwnedIcon(2, setOf(CardType.WHEAT_FIELD, CardType.APPLE_ORCHARD)),
    ),
    CardType.STADIUM to Economics(Payout.PerOpponent(2)),
    CardType.TV_STATION to Economics(Payout.Fixed(5)),
    CardType.BUSINESS_CENTER to Economics(Payout.Fixed(0)),
)

private val oneDieDistribution: Map<Int, Double> = (1..6).associateWith { 1.0 / 6.0 }

private val twoDiceDistribution: Map<Int, Double> = buildMap {
    for (first in 1..6) for (second in 1..6) {
        val sum = first + second
        this[sum] = (this[sum] ?: 0.0) + 1.0 / 36.0
    }
}

/** Probability a single roll (one die, or two if [twoDice]) lands on any of [numbers]. */
fun activationProbability(numbers: Set<Int>, twoDice: Boolean): Double {
    val distribution = if (twoDice) twoDiceDistribution else oneDieDistribution
    return numbers.sumOf { distribution[it] ?: 0.0 }
}

/**
 * Recommends the best establishment for [player] to buy next, or `null` when
 * nothing affordable and in stock has a positive expected value.
 */
fun recommendBestBuy(state: GameScreenState, player: PlayerCoinState): CardType? {
    val rowId = player.id.toIntOrNull()
    val twoDice = rowId != null && state.playerLandmarks[rowId].orEmpty()
        .any { it.landmarkType == LandmarkType.TRAIN_STATION && it.isBuilt }
    val ownedCounts = rowId
        ?.let { state.playerCards[it].orEmpty().associate { card -> card.cardType to card.quantity } }
        .orEmpty()
    val opponentCount = (state.players.size - 1).coerceAtLeast(0)

    return establishmentEconomics.entries
        .asSequence()
        .filter { (type, _) -> (state.marketplace[type] ?: 0) > 0 }
        .mapNotNull { (type, econ) ->
            val cost = costOf(type, state)
            if (cost > player.coins) return@mapNotNull null
            val activationNumbers = activationNumbersOf(type, state)
            val expectedValue = activationProbability(activationNumbers, twoDice) *
                econ.payout.amount(ownedCounts, opponentCount)
            if (expectedValue <= 0.0) return@mapNotNull null
            Candidate(type, expectedValue, cost)
        }
        .sortedWith(
            compareByDescending<Candidate> { it.expectedValue }
                .thenBy { it.cost }
                .thenBy { it.type.ordinal },
        )
        .firstOrNull()
        ?.type
}

private data class Candidate(val type: CardType, val expectedValue: Double, val cost: Int)

/** Card cost from the server-provided shop items, falling back to the local catalog. */
private fun costOf(type: CardType, state: GameScreenState): Int {
    state.shopItems.firstOrNull { it.type == type.name }?.cost?.let { return it }
    return CardDefinitions.forType(type)?.cost ?: Int.MAX_VALUE
}

private fun activationNumbersOf(type: CardType, state: GameScreenState): Set<Int> {
    state.shopItems.firstOrNull { it.type == type.name }?.activationNumbers?.let { return it.toSet() }
    return CardDefinitions.forType(type)?.activationNumbers.orEmpty().toSet()
}

private const val GRAVITY = 9.80665f
private const val SHAKE_THRESHOLD_MS2 = 13f

/** Combined-axis acceleration magnitude (m/s²); ≈ [GRAVITY] when the device is at rest. */
fun accelerationMagnitude(x: Float, y: Float, z: Float): Float = sqrt(x * x + y * y + z * z)

/** Pure shake test: true when the combined acceleration exceeds the shake threshold. */
fun isShake(magnitude: Float): Boolean = magnitude > SHAKE_THRESHOLD_MS2
