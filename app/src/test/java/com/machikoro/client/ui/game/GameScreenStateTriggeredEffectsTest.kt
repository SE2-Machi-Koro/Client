package com.machikoro.client.domain.model.state

import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test


class GameScreenStateTriggeredEffectsTest {

    @Test
    fun triggeredEstablishmentCountReturnsZeroWhenNoDiceResultExists() {
        val state = baseState(
            diceResult = null,
            playerCards = mapOf(
                1 to listOf(PlayerCardState(CardType.WHEAT_FIELD, quantity = 1))
            )
        )

        assertEquals(0, state.triggeredEstablishmentCountForCurrentRoll())
        assertFalse(state.hasTriggeredEstablishmentsForCurrentRoll())
    }

    @Test
    fun triggeredEstablishmentCountCountsBlueCardsForAllPlayers() {
        val state = baseState(
            diceResult = listOf(1),
            playerCards = mapOf(
                1 to listOf(PlayerCardState(CardType.WHEAT_FIELD, quantity = 1)),
                2 to listOf(PlayerCardState(CardType.WHEAT_FIELD, quantity = 2))
            )
        )

        assertEquals(3, state.triggeredEstablishmentCountForCurrentRoll())
        assertTrue(state.hasTriggeredEstablishmentsForCurrentRoll())
    }

    @Test
    fun triggeredEstablishmentCountCountsGreenCardsOnlyForActivePlayer() {
        val state = baseState(
            diceResult = listOf(2),
            playerCards = mapOf(
                1 to listOf(PlayerCardState(CardType.BAKERY, quantity = 2)),
                2 to listOf(PlayerCardState(CardType.BAKERY, quantity = 3))
            )
        )

        assertEquals(2, state.triggeredEstablishmentCountForCurrentRoll())
    }

    @Test
    fun triggeredEstablishmentCountCountsRedCardsOnlyForOpponents() {
        val state = baseState(
            diceResult = listOf(3),
            playerCards = mapOf(
                1 to listOf(PlayerCardState(CardType.CAFE, quantity = 2)),
                2 to listOf(PlayerCardState(CardType.CAFE, quantity = 3))
            )
        )

        assertEquals(3, state.triggeredEstablishmentCountForCurrentRoll())
    }

    @Test
    fun triggeredEstablishmentCountCountsPurpleCardsOnlyForActivePlayer() {
        val state = baseState(
            diceResult = listOf(6),
            playerCards = mapOf(
                1 to listOf(PlayerCardState(CardType.STADIUM, quantity = 1)),
                2 to listOf(PlayerCardState(CardType.STADIUM, quantity = 1))
            )
        )

        assertEquals(1, state.triggeredEstablishmentCountForCurrentRoll())
    }

    @Test
    fun triggeredEstablishmentCountIgnoresCardsWithZeroQuantity() {
        val state = baseState(
            diceResult = listOf(1),
            playerCards = mapOf(
                1 to listOf(PlayerCardState(CardType.WHEAT_FIELD, quantity = 0))
            )
        )

        assertEquals(0, state.triggeredEstablishmentCountForCurrentRoll())
    }

    @Test
    fun activePlayerUsernameReturnsActivePlayerName() {
        val state = baseState(diceResult = listOf(1))

        assertEquals("You", state.activePlayerUsername)
    }

    @Test
    fun activePlayerUsernameFallsBackWhenNoActivePlayerExists() {
        val state = baseState(
            diceResult = listOf(1),
            players = listOf(
                PlayerCoinState(
                    id = "1",
                    displayName = "You",
                    coins = 5,
                    isCurrentPlayer = true,
                    isActivePlayer = false
                )
            )
        )

        assertEquals("Player", state.activePlayerUsername)
    }

    private fun baseState(
        diceResult: List<Int>? = listOf(1),
        players: List<PlayerCoinState> = listOf(
            PlayerCoinState(
                id = "1",
                displayName = "You",
                coins = 5,
                isCurrentPlayer = true,
                isActivePlayer = true
            ),
            PlayerCoinState(
                id = "2",
                displayName = "Player2",
                coins = 5,
                isCurrentPlayer = false,
                isActivePlayer = false
            )
        ),
        playerCards: Map<Int, List<PlayerCardState>> = emptyMap(),
    ): GameScreenState {
        return GameScreenState(
            gameId = 1,
            connectionStatus = ConnectionStatus.CONNECTED,
            gamePhase = GamePhase.RESOLVE_EFFECTS,
            players = players,
            diceResult = diceResult,
            activePlayerId = 1,
            myUserId = 1,
            purchaseState = PurchaseState.IDLE,
            gameStatus = GameStatus.IN_PROGRESS,
            playerCards = playerCards
        )
    }
}