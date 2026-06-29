package com.machikoro.client.ui.game

import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.model.state.PurchaseState
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.enums.ShopItemColor
import com.machikoro.client.domain.model.shop.PurchaseEvent
import com.machikoro.client.domain.model.shop.ShopItem
import com.machikoro.client.domain.model.state.ChatMessageState
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.PlayerCardState
import com.machikoro.client.domain.model.state.PlayerLandmarkState
import com.machikoro.client.domain.session.Session
import com.machikoro.client.domain.session.SessionStateHolder
import com.machikoro.client.network.debug.DebugApi
import com.machikoro.client.network.debug.EndGameRequest
import com.machikoro.client.network.debug.FillLobbyRequest
import com.machikoro.client.network.debug.ResetLobbyRequest
import com.machikoro.client.network.websocket.FakeWebSocketClient
import com.machikoro.client.ui.start.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class GameScreenViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun fakeSession(userId: Int = 1) = object : SessionStateHolder {
        override val session: StateFlow<Session?> = MutableStateFlow(
            Session(sessionToken = "token", username = "alice", userId = userId)
        )
        override fun signIn(token: String, username: String, userId: Int) = Unit
        override fun signOut() = Unit
    }

    private fun viewModel(
        fakeClient: FakeWebSocketClient = FakeWebSocketClient(),
        userId: Int = 1,
        fakeDebugApi: FakeDebugApi = FakeDebugApi(),
        resolveEffectsDwellMillis: Long = GameScreenViewModel.DEFAULT_RESOLVE_EFFECTS_DWELL_MS,
    ) = GameScreenViewModel(fakeClient, fakeSession(userId), fakeDebugApi, resolveEffectsDwellMillis)

    @Test
    fun initialStateUsesInitialValues() = runTest {
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(GamePhase.NONE, viewModel.state.value.gamePhase)
        assertEquals(ConnectionStatus.IDLE, viewModel.state.value.connectionStatus)
        assertEquals(emptyList<PlayerCoinState>(), viewModel.state.value.players)
        assertEquals(null, viewModel.state.value.gameId)
        assertEquals(PurchaseState.IDLE, viewModel.state.value.purchaseState)
    }

    @Test
    fun connectionStatusUpdatesAreReflectedInState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)

        fakeClient.emitConnectionStatus(ConnectionStatus.CONNECTING)
        advanceUntilIdle()
        assertEquals(ConnectionStatus.CONNECTING, viewModel.state.value.connectionStatus)

        fakeClient.emitConnectionStatus(ConnectionStatus.CONNECTED)
        advanceUntilIdle()
        assertEquals(ConnectionStatus.CONNECTED, viewModel.state.value.connectionStatus)

        fakeClient.emitConnectionStatus(ConnectionStatus.ERROR)
        advanceUntilIdle()
        assertEquals(ConnectionStatus.ERROR, viewModel.state.value.connectionStatus)
    }

    @Test
    fun diceRollTickIncrementsInStateOnEachRoll() = runTest {
        // #346: the tick is what drives the non-active player's roll/reroll
        // animation, so every genuine dice result must bump it in state.
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)

        assertEquals(0L, viewModel.state.value.diceRollTick)

        fakeClient.emitDiceResult(listOf(3, 4))
        advanceUntilIdle()
        val afterRoll = viewModel.state.value.diceRollTick
        assertTrue(afterRoll > 0L)

        // A same-turn reroll (a second genuine result) bumps the tick again.
        fakeClient.emitDiceResult(listOf(6, 6))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.diceRollTick > afterRoll)
    }

    @Test
    fun gamePhaseUpdatesAreReflectedInState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)

        fakeClient.emitGamePhase(GamePhase.ROLL_DICE)
        advanceUntilIdle()
        assertEquals(GamePhase.ROLL_DICE, viewModel.state.value.gamePhase)

        fakeClient.emitGamePhase(GamePhase.RESOLVE_EFFECTS)
        advanceUntilIdle()
        assertEquals(GamePhase.RESOLVE_EFFECTS, viewModel.state.value.gamePhase)

        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        advanceUntilIdle()
        assertEquals(GamePhase.BUY_OR_BUILD, viewModel.state.value.gamePhase)

        fakeClient.emitGamePhase(GamePhase.END_TURN)
        advanceUntilIdle()
        assertEquals(GamePhase.END_TURN, viewModel.state.value.gamePhase)
    }

    @Test
    fun connectionStatusAndGamePhaseUpdateIndependently() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)

        fakeClient.emitConnectionStatus(ConnectionStatus.CONNECTED)
        fakeClient.emitGamePhase(GamePhase.ROLL_DICE)
        advanceUntilIdle()

        assertEquals(ConnectionStatus.CONNECTED, viewModel.state.value.connectionStatus)
        assertEquals(GamePhase.ROLL_DICE, viewModel.state.value.gamePhase)

        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        advanceUntilIdle()
        assertEquals(ConnectionStatus.CONNECTED, viewModel.state.value.connectionStatus)
        assertEquals(GamePhase.BUY_OR_BUILD, viewModel.state.value.gamePhase)

        fakeClient.emitConnectionStatus(ConnectionStatus.DISCONNECTED)
        advanceUntilIdle()
        assertEquals(ConnectionStatus.DISCONNECTED, viewModel.state.value.connectionStatus)
        assertEquals(GamePhase.BUY_OR_BUILD, viewModel.state.value.gamePhase)
    }

    @Test
    fun playerCoinUpdatesAreReflectedInState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)
        val players = listOf(
            PlayerCoinState(id = "player-1", displayName = "You", coins = 3, isCurrentPlayer = true),
            PlayerCoinState(id = "player-2", displayName = "SoupCube", coins = 5, isActivePlayer = true)
        )

        fakeClient.emitPlayers(players)
        advanceUntilIdle()

        assertEquals(players, viewModel.state.value.players)
    }

    @Test
    fun playerCoinUpdatesReplacePreviousValuesForIncreasesAndDecreases() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)

        fakeClient.emitPlayers(
            listOf(
                PlayerCoinState(id = "player-1", displayName = "You", coins = 3),
                PlayerCoinState(id = "player-2", displayName = "SoupCube", coins = 5)
            )
        )
        advanceUntilIdle()

        val updatedPlayers = listOf(
            PlayerCoinState(id = "player-1", displayName = "You", coins = 8),
            PlayerCoinState(id = "player-2", displayName = "SoupCube", coins = 2)
        )
        fakeClient.emitPlayers(updatedPlayers)
        advanceUntilIdle()

        assertEquals(updatedPlayers, viewModel.state.value.players)
    }

    @Test
    fun diceResultIsNullInInitialState() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertNull(viewModel.state.value.diceResult)
    }

    @Test
    fun diceResultFromClientIsReflectedInState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)

        fakeClient.emitDiceResult(listOf(3, 4))
        advanceUntilIdle()

        assertEquals(listOf(3, 4), viewModel.state.value.diceResult)
    }

    @Test
    fun diceResultIsClearedWhenEnteringRollDicePhase() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)

        // Previous turn's result, outside the roll phase.
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitDiceResult(listOf(3, 4))
        advanceUntilIdle()
        assertEquals(listOf(3, 4), viewModel.state.value.diceResult)

        // A new turn enters ROLL_DICE: the stale result must be dropped so the
        // Roll Dice button reappears and the old number/die stop showing.
        fakeClient.emitGamePhase(GamePhase.ROLL_DICE)
        advanceUntilIdle()
        assertNull(viewModel.state.value.diceResult)
    }

    @Test
    fun activeGameIdFromClientIsReflectedInState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)

        fakeClient.emitActiveGameId(7)
        advanceUntilIdle()

        assertEquals(7, viewModel.state.value.gameId)
    }

    @Test
    fun rollDiceForwardsDiceCountToClientWhenPhaseIsRollDiceAndIsActivePlayer() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.ROLL_DICE)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.rollDice(diceCount = 1)

        assertEquals(1, fakeClient.lastRolledDiceCount)
    }

    @Test
    fun rollDiceClampsToOneDieWhenActivePlayerHasNoTrainStation() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.ROLL_DICE)
        fakeClient.emitActivePlayerId(42)
        fakeClient.emitPlayers(
            listOf(PlayerCoinState(id = "42", displayName = "me", coins = 3, isActivePlayer = true))
        )
        fakeClient.emitPlayerLandmarks(
            mapOf(42 to listOf(PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = false)))
        )
        advanceUntilIdle()

        // Player asks (or a stale count asks) for two dice without a Train Station (#399).
        viewModel.rollDice(diceCount = 2)

        assertEquals(1, fakeClient.lastRolledDiceCount)
        assertEquals(1, viewModel.state.value.requestedDiceCount)
    }

    @Test
    fun rollDiceForwardsTwoDiceWhenActivePlayerHasTrainStation() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.ROLL_DICE)
        fakeClient.emitActivePlayerId(42)
        fakeClient.emitPlayers(
            listOf(PlayerCoinState(id = "42", displayName = "me", coins = 3, isActivePlayer = true))
        )
        fakeClient.emitPlayerLandmarks(
            mapOf(42 to listOf(PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true)))
        )
        advanceUntilIdle()

        viewModel.rollDice(diceCount = 2)

        assertEquals(2, fakeClient.lastRolledDiceCount)
        assertEquals(2, viewModel.state.value.requestedDiceCount)
    }

    @Test
    fun rollDiceIsIgnoredWhenPhaseIsNotRollDice() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.rollDice(diceCount = 1)

        assertNull(fakeClient.lastRolledDiceCount)
    }

    @Test
    fun rollDiceIsIgnoredWhenNotActivePlayer() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 1)

        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.ROLL_DICE)
        fakeClient.emitActivePlayerId(99)
        advanceUntilIdle()

        viewModel.rollDice(diceCount = 1)

        assertNull(fakeClient.lastRolledDiceCount)
    }

    @Test
    fun rerollForwardsDiceCountWhenActivePlayerHasRadioTowerInResolveEffects() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)
        setUpRerollableTurn(fakeClient)
        advanceUntilIdle()

        viewModel.rerollDice(diceCount = 2)

        assertEquals(2, fakeClient.lastRerolledDiceCount)
    }

    @Test
    fun rerollIsIgnoredOutsideResolveEffectsPhase() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)
        setUpRerollableTurn(fakeClient)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        advanceUntilIdle()

        viewModel.rerollDice(diceCount = 1)

        assertNull(fakeClient.lastRerolledDiceCount)
    }

    @Test
    fun rerollIsIgnoredWhenActivePlayerHasNoRadioTower() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)
        setUpRerollableTurn(fakeClient, radioTowerBuilt = false)
        advanceUntilIdle()

        viewModel.rerollDice(diceCount = 1)

        assertNull(fakeClient.lastRerolledDiceCount)
    }

    @Test
    fun rerollIsIgnoredWhenNotActivePlayer() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 1)
        setUpRerollableTurn(fakeClient, activePlayerId = 99)
        advanceUntilIdle()

        viewModel.rerollDice(diceCount = 1)

        assertNull(fakeClient.lastRerolledDiceCount)
    }

    @Test
    fun rerollIsLimitedToOncePerTurn() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)
        setUpRerollableTurn(fakeClient)
        advanceUntilIdle()

        viewModel.rerollDice(diceCount = 1)
        viewModel.rerollDice(diceCount = 1)

        assertEquals(1, fakeClient.rerollCallCount)
        assertFalse(viewModel.canRerollThisTurn.value)
    }

    @Test
    fun rerollIsIgnoredWhileDiceAnimationIsAlreadyRunning() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)
        setUpRerollableTurn(fakeClient)
        advanceUntilIdle()

        viewModel.rerollDice(diceCount = 1)
        fakeClient.emitActivePlayerId(99)
        runCurrent()
        fakeClient.emitActivePlayerId(42)
        runCurrent()
        viewModel.rerollDice(diceCount = 1)

        assertEquals(1, fakeClient.rerollCallCount)
    }

    @Test
    fun rerollClearsIsRollingAfterTimeoutWhenServerNeverReplies() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)
        setUpRerollableTurn(fakeClient)
        advanceUntilIdle()

        viewModel.rerollDice(diceCount = 1)
        assertTrue(viewModel.state.value.isRolling)

        advanceTimeBy(11_000L)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isRolling)
    }

    @Test
    fun rerollBudgetRenewsWhenTurnRotates() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)
        setUpRerollableTurn(fakeClient)
        advanceUntilIdle()

        viewModel.rerollDice(diceCount = 1)
        // Turn rotates to another player and back to us. advanceUntilIdle between
        // emits so the conflating StateFlow actually surfaces the intermediate
        // owner (99) to the rotation observer rather than collapsing to 42.
        fakeClient.emitActivePlayerId(99)
        advanceUntilIdle()
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        assertTrue(viewModel.canRerollThisTurn.value)

        viewModel.rerollDice(diceCount = 1)

        assertEquals(2, fakeClient.rerollCallCount)
    }

    /**
     * Drives the fake client into a state where the local player (user id 42,
     * database id 7) is the active player in RESOLVE_EFFECTS after a roll, with a
     * built Radio Tower — the precondition for [GameScreenViewModel.rerollDice].
     */
    private fun setUpRerollableTurn(
        client: FakeWebSocketClient,
        activePlayerId: Int = 42,
        radioTowerBuilt: Boolean = true,
    ) {
        client.emitGameStatus(GameStatus.IN_PROGRESS)
        client.emitGamePhase(GamePhase.RESOLVE_EFFECTS)
        client.emitActivePlayerId(activePlayerId)
        client.emitPlayers(
            listOf(
                PlayerCoinState(id = "7", displayName = "alice", coins = 5, isActivePlayer = true),
            )
        )
        client.emitPlayerLandmarks(
            mapOf(7 to listOf(PlayerLandmarkState(LandmarkType.RADIO_TOWER, isBuilt = radioTowerBuilt)))
        )
        client.emitDiceResult(listOf(4))
    }

    @Test
    fun activePlayerIdFromClientIsReflectedInState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)

        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        assertEquals(42, viewModel.state.value.activePlayerId)
    }

    @Test
    fun isActivePlayerIsTrueWhenMyUserIdMatchesActivePlayerId() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        assertEquals(true, viewModel.state.value.isActivePlayer)
    }

    @Test
    fun isActivePlayerIsFalseWhenMyUserIdDoesNotMatchActivePlayerId() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 1)

        fakeClient.emitActivePlayerId(99)
        advanceUntilIdle()

        assertEquals(false, viewModel.state.value.isActivePlayer)
    }

    @Test
    fun selectingPurchaseItemDoesNotSendPurchase() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.selectPurchaseItem("BAKERY")

        assertEquals("BAKERY", viewModel.state.value.selectedPurchaseItemType)
        assertNull(fakeClient.lastPurchase)
        assertEquals(PurchaseState.IDLE, viewModel.state.value.purchaseState)
    }

    @Test
    fun purchaseSelectedItemBuysSelectedCard() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.selectPurchaseItem("BAKERY")
        viewModel.purchaseSelectedItem()

        assertEquals(
            FakeWebSocketClient.PurchaseCall(
                gameId = 7,
                purchaseType = PurchaseType.ESTABLISHMENT,
                cardType = "BAKERY",
                landmarkType = null
            ),
            fakeClient.lastPurchase
        )
        assertEquals(PurchaseState.PENDING, viewModel.state.value.purchaseState)
        assertEquals("BAKERY", viewModel.state.value.pendingPurchaseItemType)
    }

    @Test
    fun activePlayerCanPurchaseEstablishmentDuringBuyOrBuild() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.purchase("BAKERY")

        assertEquals(
            FakeWebSocketClient.PurchaseCall(
                gameId = 7,
                purchaseType = PurchaseType.ESTABLISHMENT,
                cardType = "BAKERY",
                landmarkType = null
            ),
            fakeClient.lastPurchase
        )
        assertEquals(PurchaseState.PENDING, viewModel.state.value.purchaseState)
        assertEquals("BAKERY", viewModel.state.value.pendingPurchaseItemType)
    }

    @Test
    fun purchaseIsIgnoredWhenMarketplaceHasNoRemainingCopies() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(42)
        fakeClient.emitMarketplace(mapOf(CardType.BAKERY to 0))
        advanceUntilIdle()

        viewModel.purchase("BAKERY")

        assertNull(fakeClient.lastPurchase)
        assertEquals(PurchaseState.IDLE, viewModel.state.value.purchaseState)
    }

    @Test
    fun selectingPurchaseItemUsesMarketplaceAvailabilityOverShopItemAvailability() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(42)
        fakeClient.emitShopItems(
            listOf(
                ShopItem(
                    type = "BAKERY",
                    displayName = "Bakery",
                    cost = 1,
                    purchaseType = PurchaseType.ESTABLISHMENT,
                    color = ShopItemColor.GREEN,
                    imageKey = "bakery",
                    establishmentType = "BREAD",
                    activationNumbers = listOf(2, 3),
                    effectText = "Get 1 coin from the bank on your turn.",
                    isAvailable = false
                )
            )
        )
        fakeClient.emitMarketplace(mapOf(CardType.BAKERY to 2))
        advanceUntilIdle()

        viewModel.selectPurchaseItem("BAKERY")

        assertEquals("BAKERY", viewModel.state.value.selectedPurchaseItemType)
    }

    @Test
    fun selectingAlreadyOwnedPurpleEstablishmentIsIgnored() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(42)
        fakeClient.emitPlayers(
            listOf(
                PlayerCoinState(
                    id = "100",
                    displayName = "Active player",
                    coins = 10,
                    isActivePlayer = true,
                    isCurrentPlayer = true
                )
            )
        )
        fakeClient.emitPlayerCards(mapOf(100 to listOf(PlayerCardState(CardType.STADIUM, quantity = 1))))
        fakeClient.emitMarketplace(mapOf(CardType.STADIUM to 1))
        advanceUntilIdle()

        viewModel.selectPurchaseItem("STADIUM")

        assertNull(viewModel.state.value.selectedPurchaseItemType)
        assertNull(fakeClient.lastPurchase)
    }

    @Test
    fun purchaseAlreadyOwnedPurpleEstablishmentIsIgnored() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(42)
        fakeClient.emitPlayers(
            listOf(
                PlayerCoinState(
                    id = "100",
                    displayName = "Active player",
                    coins = 10,
                    isActivePlayer = true,
                    isCurrentPlayer = true
                )
            )
        )
        fakeClient.emitPlayerCards(mapOf(100 to listOf(PlayerCardState(CardType.STADIUM, quantity = 1))))
        fakeClient.emitMarketplace(mapOf(CardType.STADIUM to 1))
        advanceUntilIdle()

        viewModel.purchase("STADIUM")

        assertNull(fakeClient.lastPurchase)
        assertEquals(PurchaseState.IDLE, viewModel.state.value.purchaseState)
    }

    @Test
    fun ownedNonPurpleEstablishmentCanStillBePurchasedAsDuplicate() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(42)
        fakeClient.emitPlayers(
            listOf(
                PlayerCoinState(
                    id = "100",
                    displayName = "Active player",
                    coins = 10,
                    isActivePlayer = true,
                    isCurrentPlayer = true
                )
            )
        )
        fakeClient.emitPlayerCards(mapOf(100 to listOf(PlayerCardState(CardType.BAKERY, quantity = 1))))
        fakeClient.emitMarketplace(mapOf(CardType.BAKERY to 5))
        advanceUntilIdle()

        viewModel.purchase("BAKERY")

        assertEquals(
            FakeWebSocketClient.PurchaseCall(
                gameId = 7,
                purchaseType = PurchaseType.ESTABLISHMENT,
                cardType = "BAKERY",
                landmarkType = null
            ),
            fakeClient.lastPurchase
        )
        assertEquals(PurchaseState.PENDING, viewModel.state.value.purchaseState)
    }

    @Test
    fun unownedPurpleEstablishmentCanBePurchased() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(42)
        fakeClient.emitPlayers(
            listOf(
                PlayerCoinState(
                    id = "100",
                    displayName = "Active player",
                    coins = 10,
                    isActivePlayer = true,
                    isCurrentPlayer = true
                )
            )
        )
        fakeClient.emitPlayerCards(mapOf(100 to listOf(PlayerCardState(CardType.BAKERY, quantity = 1))))
        fakeClient.emitMarketplace(mapOf(CardType.STADIUM to 1))
        advanceUntilIdle()

        viewModel.purchase("STADIUM")

        assertEquals(
            FakeWebSocketClient.PurchaseCall(
                gameId = 7,
                purchaseType = PurchaseType.ESTABLISHMENT,
                cardType = "STADIUM",
                landmarkType = null
            ),
            fakeClient.lastPurchase
        )
        assertEquals(PurchaseState.PENDING, viewModel.state.value.purchaseState)
    }

    @Test
    fun activePlayerCanPurchaseLandmarkDuringBuyOrBuild() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.purchase("TRAIN_STATION")

        assertEquals(
            FakeWebSocketClient.PurchaseCall(
                gameId = 7,
                purchaseType = PurchaseType.LANDMARK,
                cardType = null,
                landmarkType = "TRAIN_STATION"
            ),
            fakeClient.lastPurchase
        )
        assertEquals(PurchaseState.PENDING, viewModel.state.value.purchaseState)
        assertEquals("TRAIN_STATION", viewModel.state.value.pendingPurchaseItemType)
    }

    @Test
    fun purchaseIsIgnoredWithoutGameId() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.purchase("BAKERY")

        assertNull(fakeClient.lastPurchase)
        assertEquals(PurchaseState.IDLE, viewModel.state.value.purchaseState)
    }

    @Test
    fun purchaseIsIgnoredWhenCurrentUserIsNotActivePlayer() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(99)
        advanceUntilIdle()

        viewModel.purchase("BAKERY")

        assertNull(fakeClient.lastPurchase)
        assertEquals(PurchaseState.IDLE, viewModel.state.value.purchaseState)
    }

    @Test
    fun successfulPurchaseDisablesSecondLocalPurchase() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.purchase("BAKERY")
        viewModel.purchase("CAFE")

        assertEquals(
            FakeWebSocketClient.PurchaseCall(
                gameId = 7,
                purchaseType = PurchaseType.ESTABLISHMENT,
                cardType = "BAKERY",
                landmarkType = null
            ),
            fakeClient.lastPurchase
        )
        assertEquals(PurchaseState.PENDING, viewModel.state.value.purchaseState)
    }

    @Test
    fun matchingPurchaseSuccessEventCompletesPendingPurchase() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.purchase("BAKERY")
        fakeClient.emitPurchaseEvent(
            PurchaseEvent.Success(
                purchaseType = PurchaseType.ESTABLISHMENT,
                itemType = "BAKERY"
            )
        )
        advanceUntilIdle()

        assertEquals(PurchaseState.SUCCESS, viewModel.state.value.purchaseState)
        assertNull(viewModel.state.value.selectedPurchaseItemType)
        assertNull(viewModel.state.value.pendingPurchaseItemType)
        assertEquals("BAKERY", viewModel.state.value.purchaseFeedbackItemType)
        assertEquals("Bakery bought", viewModel.state.value.purchaseMessage)
    }

    @Test
    fun purchaseSuccessEventShowsFeedbackForNonActivePlayer() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 7)

        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        fakeClient.emitPurchaseEvent(
            PurchaseEvent.Success(
                purchaseType = PurchaseType.ESTABLISHMENT,
                itemType = "BAKERY"
            )
        )
        advanceUntilIdle()

        assertEquals(PurchaseState.SUCCESS, viewModel.state.value.purchaseState)
        assertEquals("BAKERY", viewModel.state.value.purchaseFeedbackItemType)
        assertEquals("Bakery bought", viewModel.state.value.purchaseMessage)
        assertNull(fakeClient.endedTurnGameId)
    }

    @Test
    fun matchingPurchaseSuccessEventEndsTurn() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.purchase("BAKERY")
        fakeClient.emitPurchaseEvent(
            PurchaseEvent.Success(
                purchaseType = PurchaseType.ESTABLISHMENT,
                itemType = "BAKERY"
            )
        )
        advanceUntilIdle()

        assertEquals(7, fakeClient.endedTurnGameId)
    }

    @Test
    fun purchaseFailureEventShowsErrorAndAllowsRetry() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.purchase("BAKERY")
        fakeClient.emitPurchaseEvent(PurchaseEvent.Failure("Not enough coins"))
        advanceUntilIdle()

        assertEquals(PurchaseState.ERROR, viewModel.state.value.purchaseState)
        assertNull(viewModel.state.value.pendingPurchaseItemType)
        assertEquals("BAKERY", viewModel.state.value.purchaseFeedbackItemType)
        assertEquals("Not enough coins", viewModel.state.value.purchaseMessage)

        viewModel.purchase("CAFE")

        assertEquals(
            FakeWebSocketClient.PurchaseCall(
                gameId = 7,
                purchaseType = PurchaseType.ESTABLISHMENT,
                cardType = "CAFE",
                landmarkType = null
            ),
            fakeClient.lastPurchase
        )
        assertEquals(PurchaseState.PENDING, viewModel.state.value.purchaseState)
    }

    @Test
    fun purchaseFeedbackResetsWhenLeavingBuyingPhase() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.purchase("BAKERY")
        fakeClient.emitGamePhase(GamePhase.END_TURN)
        advanceUntilIdle()

        assertEquals(PurchaseState.IDLE, viewModel.state.value.purchaseState)
        assertNull(viewModel.state.value.pendingPurchaseItemType)
        assertNull(viewModel.state.value.purchaseFeedbackItemType)
        assertNull(viewModel.state.value.purchaseMessage)
    }

    @Test
    fun isRollingIsFalseInInitialState() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isRolling)
    }

    @Test
    fun rollDiceSetsIsRollingToTrue() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.ROLL_DICE)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.rollDice(diceCount = 1)

        assertTrue(viewModel.state.value.isRolling)
    }

    @Test
    fun isRollingIsClearedWhenDiceResultArrives() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.ROLL_DICE)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.rollDice(diceCount = 1)
        assertTrue(viewModel.state.value.isRolling)

        fakeClient.emitDiceResult(listOf(4))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isRolling)
    }

    @Test
    fun isRollingIsClearedWhenGamePhaseChangesAwayFromRollDice() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.ROLL_DICE)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.rollDice(diceCount = 1)
        assertTrue(viewModel.state.value.isRolling)

        fakeClient.emitGamePhase(GamePhase.RESOLVE_EFFECTS)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isRolling)
    }

    @Test
    fun secondRollDiceCallIsIgnoredWhileRollingIsInProgress() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.ROLL_DICE)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.rollDice(diceCount = 1)
        viewModel.rollDice(diceCount = 2)

        assertEquals(1, fakeClient.lastRolledDiceCount)
    }

    @Test
    fun isRollingIsClearedAfterTimeoutWhenServerNeverReplies() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.ROLL_DICE)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.rollDice(diceCount = 1)
        assertTrue(viewModel.state.value.isRolling)

        advanceTimeBy(11_000L)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isRolling)
    }

    @Test
    fun staleRollTimeoutDoesNotClearLaterRoll() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.ROLL_DICE)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.rollDice(diceCount = 1)
        advanceTimeBy(2_000L)
        fakeClient.emitDiceResult(listOf(4))
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isRolling)

        viewModel.rollDice(diceCount = 2)
        assertTrue(viewModel.state.value.isRolling)

        advanceTimeBy(8_100L)
        runCurrent()

        assertTrue(viewModel.state.value.isRolling)
    }

    @Test
    fun rollDiceDoesNotSetIsRollingWhenPhaseIsNotRollDice() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.rollDice(diceCount = 1)

        assertFalse(viewModel.state.value.isRolling)
    }

    @Test
    fun rollDiceDoesNotSetIsRollingWhenNotActivePlayer() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 1)

        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.ROLL_DICE)
        fakeClient.emitActivePlayerId(99)
        advanceUntilIdle()

        viewModel.rollDice(diceCount = 1)

        assertFalse(viewModel.state.value.isRolling)
    }


    @Test
    fun gameStatusFromClientIsReflectedInState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)

        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        advanceUntilIdle()

        assertEquals(GameStatus.IN_PROGRESS, viewModel.state.value.gameStatus)
    }

    @Test
    fun winnerIdFromClientIsReflectedInState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)

        fakeClient.emitWinnerId(11)
        advanceUntilIdle()

        assertEquals(11, viewModel.state.value.winnerId)
    }

    @Test
    fun clearGameStateClearsFinishedGameDataFromClient() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)

        fakeClient.emitGameStatus(GameStatus.FINISHED)
        fakeClient.emitWinnerId(11)
        fakeClient.emitRoundNumber(4)
        advanceUntilIdle()

        viewModel.clearGameState()
        advanceUntilIdle()

        assertNull(viewModel.state.value.gameStatus)
        assertNull(viewModel.state.value.winnerId)
        assertNull(viewModel.state.value.roundNumber)
    }

    @Test
    fun roundNumberFromClientIsReflectedInState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)

        fakeClient.emitRoundNumber(4)
        advanceUntilIdle()

        assertEquals(4, viewModel.state.value.roundNumber)
    }

    @Test
    fun playerLandmarksFromClientAreReflectedInState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)
        val landmarks = mapOf(
            1 to listOf(
                PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true),
                PlayerLandmarkState(LandmarkType.SHOPPING_MALL, isBuilt = false),
            )
        )

        fakeClient.emitPlayerLandmarks(landmarks)
        advanceUntilIdle()

        assertEquals(landmarks, viewModel.state.value.playerLandmarks)
    }

    @Test
    fun playerCardsFromClientAreReflectedInState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)
        val cards = mapOf(
            1 to listOf(
                PlayerCardState(CardType.WHEAT_FIELD, quantity = 1),
                PlayerCardState(CardType.BAKERY, quantity = 2),
            )
        )

        fakeClient.emitPlayerCards(cards)
        advanceUntilIdle()

        assertEquals(cards, viewModel.state.value.playerCards)
    }

    @Test
    fun marketplaceFromClientIsReflectedInState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)
        val marketplace = mapOf(CardType.WHEAT_FIELD to 6, CardType.BAKERY to 5)

        fakeClient.emitMarketplace(marketplace)
        advanceUntilIdle()

        assertEquals(marketplace, viewModel.state.value.marketplace)
    }

    @Test
    fun rollDiceIsIgnoredWhenGameStatusIsNotInProgress() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitGameStatus(GameStatus.WAITING)  // Not IN_PROGRESS
        fakeClient.emitGamePhase(GamePhase.ROLL_DICE)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.rollDice(diceCount = 1)

        assertNull(fakeClient.lastRolledDiceCount)
    }

    @Test
    fun purchaseIsIgnoredWhenGameStatusIsNotInProgress() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitGameStatus(GameStatus.WAITING)  // Not IN_PROGRESS
        fakeClient.emitActiveGameId(7)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.purchase("BAKERY")

        assertNull(fakeClient.lastPurchase)
        assertEquals(PurchaseState.IDLE, viewModel.state.value.purchaseState)
    }

    @Test
    fun rollDiceIsIgnoredWhenGameStatusIsFinished() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitGameStatus(GameStatus.FINISHED)
        fakeClient.emitGamePhase(GamePhase.ROLL_DICE)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.rollDice(diceCount = 1)

        assertNull(fakeClient.lastRolledDiceCount)
    }

    @Test
    fun playersWithStartingCoinsFromGameStartedAreReflectedInState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)

        val players = listOf(
            PlayerCoinState(id = "1", displayName = "Alice", coins = 4, isCurrentPlayer = true),
            PlayerCoinState(id = "2", displayName = "Bob", coins = 4),
            PlayerCoinState(id = "3", displayName = "Charlie", coins = 4),
        )

        fakeClient.emitPlayers(players)
        advanceUntilIdle()

        assertEquals(players, viewModel.state.value.players)
        assertEquals(4, viewModel.state.value.players[0].coins)
        assertEquals(4, viewModel.state.value.players[1].coins)
    }

    @Test
    fun playerLandmarksAllStartUnbuiltFromGameStarted() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)

        val landmarks = mapOf(
            1 to LandmarkType.entries.map { PlayerLandmarkState(it, isBuilt = false) },
            2 to LandmarkType.entries.map { PlayerLandmarkState(it, isBuilt = false) },
        )

        fakeClient.emitPlayerLandmarks(landmarks)
        advanceUntilIdle()

        assertEquals(landmarks, viewModel.state.value.playerLandmarks)
        // All should start unbuilt
        viewModel.state.value.playerLandmarks[1]?.forEach { landmark ->
            assertEquals(false, landmark.isBuilt)
        }
    }

    @Test
    fun marketplaceInitialSupplyFromGameStartedReflectsServerState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)

        val marketplace = mapOf(
            CardType.WHEAT_FIELD to 6,
            CardType.BAKERY to 6,
            CardType.CAFE to 6,
            CardType.CONVENIENCE_STORE to 4,
            CardType.FOREST to 6,
            CardType.MINE to 6,
            CardType.APPLE_ORCHARD to 6
        )

        fakeClient.emitMarketplace(marketplace)
        advanceUntilIdle()

        assertEquals(marketplace, viewModel.state.value.marketplace)
        assertEquals(6, viewModel.state.value.marketplace[CardType.WHEAT_FIELD])
        assertEquals(4, viewModel.state.value.marketplace[CardType.CONVENIENCE_STORE])
    }

    @Test
    fun shopItemsFromGameStartedAreReflectedInState() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)

        val shopItems = listOf(
            ShopItem(
                type = "BAKERY",
                displayName = "Bakery",
                cost = 1,
                purchaseType = PurchaseType.ESTABLISHMENT,
                color = ShopItemColor.GREEN,
                imageKey = "bakery",
                establishmentType = "Bakery",
                activationNumbers = listOf(2, 3),
                effectText = "Get 1 coin from the bank on your turn.",
            ),
            ShopItem(
                type = "CAFE",
                displayName = "Cafe",
                cost = 2,
                purchaseType = PurchaseType.ESTABLISHMENT,
                color = ShopItemColor.RED,
                imageKey = "cafe",
                establishmentType = "Cafe",
                activationNumbers = listOf(3),
                effectText = "Take 1 coin from the active player.",
            ),
        )

        fakeClient.emitShopItems(shopItems)
        advanceUntilIdle()

        assertEquals(shopItems, viewModel.state.value.shopItems)
    }

    @Test
    fun reconnectSnapshotRestoresFullGameStateIncludingCoinsLandmarksMarketplaceShop() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 1)

        val players = listOf(
            PlayerCoinState(id = "1", displayName = "Alice", coins = 8, isCurrentPlayer = true, isActivePlayer = true),
            PlayerCoinState(id = "2", displayName = "Bob", coins = 5),
        )
        val landmarks = mapOf(
            1 to listOf(
                PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true),
                PlayerLandmarkState(LandmarkType.SHOPPING_MALL, isBuilt = false),
            )
        )
        val marketplace = mapOf(
            CardType.WHEAT_FIELD to 5,
            CardType.BAKERY to 4,
        )
        val shopItems = listOf(
            ShopItem(
                type = "BAKERY",
                displayName = "Bakery",
                cost = 1,
                purchaseType = PurchaseType.ESTABLISHMENT,
                color = ShopItemColor.GREEN,
                imageKey = "bakery",
                establishmentType = "Bakery",
                activationNumbers = listOf(2, 3),
                effectText = "Get 1 coin from the bank on your turn.",
            ),
        )

        fakeClient.emitPlayers(players)
        fakeClient.emitPlayerLandmarks(landmarks)
        fakeClient.emitMarketplace(marketplace)
        fakeClient.emitShopItems(shopItems)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitRoundNumber(3)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(players, state.players)
        assertEquals(8, state.players[0].coins)
        assertEquals(landmarks, state.playerLandmarks)
        assertEquals(true, state.playerLandmarks[1]?.get(0)?.isBuilt)
        assertEquals(false, state.playerLandmarks[1]?.get(1)?.isBuilt)
        assertEquals(marketplace, state.marketplace)
        assertEquals(shopItems, state.shopItems)
        assertEquals(GameStatus.IN_PROGRESS, state.gameStatus)
        assertEquals(3, state.roundNumber)
    }

    @Test
    fun firstGameStartedPayloadWithStartingStateCompletesInitialization() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        // Simulate GAME_STARTED with starting state
        fakeClient.emitActiveGameId(7)
        fakeClient.emitGamePhase(GamePhase.ROLL_DICE)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitPlayers(listOf(
            PlayerCoinState(id = "42", displayName = "Player1", coins = 4, isCurrentPlayer = true, isActivePlayer = true),
            PlayerCoinState(id = "99", displayName = "Player2", coins = 4),
        ))
        fakeClient.emitPlayerLandmarks(mapOf(
            42 to LandmarkType.entries.map { PlayerLandmarkState(it, isBuilt = false) },
            99 to LandmarkType.entries.map { PlayerLandmarkState(it, isBuilt = false) },
        ))
        fakeClient.emitMarketplace(mapOf(
            CardType.WHEAT_FIELD to 6,
            CardType.BAKERY to 6,
        ))
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(7, state.gameId)
        assertEquals(GamePhase.ROLL_DICE, state.gamePhase)
        assertEquals(GameStatus.IN_PROGRESS, state.gameStatus)
        assertEquals(2, state.players.size)
        assertEquals(4, state.players[0].coins)
        assertEquals(2, state.playerLandmarks.size)
        assertEquals(4, state.playerLandmarks[42]?.size)  // 4 landmark types
        assertEquals(2, state.marketplace.size)
        assertEquals(true, viewModel.state.value.isActivePlayer)
    }

    @Test
    fun playerCoinsDecreaseCorrectlyWhenReentering() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)

        fakeClient.emitPlayers(listOf(
            PlayerCoinState(id = "1", displayName = "Alice", coins = 4),
        ))
        advanceUntilIdle()
        assertEquals(4, viewModel.state.value.players[0].coins)

        // Player buys something
        fakeClient.emitPlayers(listOf(
            PlayerCoinState(id = "1", displayName = "Alice", coins = 3),
        ))
        advanceUntilIdle()

        assertEquals(3, viewModel.state.value.players[0].coins)
    }

    @Test
    fun builtLandmarksDisplayCorrectlyWhenPurchased() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)

        val landmarks = mapOf(
            1 to listOf(
                PlayerLandmarkState(LandmarkType.TRAIN_STATION, isBuilt = true),
                PlayerLandmarkState(LandmarkType.SHOPPING_MALL, isBuilt = false),
                PlayerLandmarkState(LandmarkType.AMUSEMENT_PARK, isBuilt = false),
                PlayerLandmarkState(LandmarkType.RADIO_TOWER, isBuilt = false),
            )
        )

        fakeClient.emitPlayerLandmarks(landmarks)
        advanceUntilIdle()

        val trainStationState = viewModel.state.value.playerLandmarks[1]?.find { it.landmarkType == LandmarkType.TRAIN_STATION }
        val shoppingMallState = viewModel.state.value.playerLandmarks[1]?.find { it.landmarkType == LandmarkType.SHOPPING_MALL }

        assertEquals(true, trainStationState?.isBuilt)
        assertEquals(false, shoppingMallState?.isBuilt)
    }
    // ── Insider Trading cheat (#203) ──────────────────────────────────────────

    private fun inProgressMyTurn(
        fakeClient: FakeWebSocketClient,
        coins: Int = 5,
        marketplace: Map<CardType, Int> = mapOf(CardType.CONVENIENCE_STORE to 6),
    ) {
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitActivePlayerId(1) // matches fakeSession userId 1 -> isActivePlayer
        fakeClient.emitPlayers(
            listOf(
                PlayerCoinState(
                    id = "1",
                    displayName = "me",
                    coins = coins,
                    isCurrentPlayer = true,
                    isActivePlayer = true,
                ),
                PlayerCoinState(id = "2", displayName = "opp", coins = 3),
            ),
        )
        fakeClient.emitMarketplace(marketplace)
    }

    @Test
    fun onShakeSetsRecommendationDuringMyTurn() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 1)
        inProgressMyTurn(fakeClient)
        advanceUntilIdle()

        viewModel.onShake()

        assertEquals(CardType.CONVENIENCE_STORE, viewModel.cheatRecommendation.value)
    }

    @Test
    fun onShakeIsNoOpWhenNotMyTurn() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 1)
        inProgressMyTurn(fakeClient)
        fakeClient.emitActivePlayerId(2) // another player's turn
        advanceUntilIdle()

        viewModel.onShake()

        assertNull(viewModel.cheatRecommendation.value)
    }

    @Test
    fun onShakeIsNoOpWhenGameNotInProgress() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 1)
        inProgressMyTurn(fakeClient)
        fakeClient.emitGameStatus(GameStatus.WAITING)
        advanceUntilIdle()

        viewModel.onShake()

        assertNull(viewModel.cheatRecommendation.value)
    }

    @Test
    fun cheatRecommendationClearsWhenActivePlayerRotates() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 1)
        inProgressMyTurn(fakeClient)
        advanceUntilIdle()
        viewModel.onShake()
        assertEquals(CardType.CONVENIENCE_STORE, viewModel.cheatRecommendation.value)

        fakeClient.emitActivePlayerId(2) // turn rotates away
        advanceUntilIdle()

        assertNull(viewModel.cheatRecommendation.value)
    }

    @Test
    fun cheatRecommendationClearsWhenRoundChanges() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 1)
        inProgressMyTurn(fakeClient)
        fakeClient.emitRoundNumber(1)
        advanceUntilIdle()
        viewModel.onShake()
        assertEquals(CardType.CONVENIENCE_STORE, viewModel.cheatRecommendation.value)

        fakeClient.emitRoundNumber(2)
        advanceUntilIdle()

        assertNull(viewModel.cheatRecommendation.value)
    }

    @Test
    fun onShakeEmitsActivationSignal() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 1)
        inProgressMyTurn(fakeClient)
        advanceUntilIdle()
        val activations = mutableListOf<CardType?>()
        val job = launch { viewModel.cheatActivations.collect { activations.add(it) } }
        advanceUntilIdle()

        viewModel.onShake()
        advanceUntilIdle()

        assertEquals(listOf(CardType.CONVENIENCE_STORE), activations)
        job.cancel()
    }

    // -- Resolving Effects auto-transition (#302) ------------------------------

    private fun FakeWebSocketClient.enterResolveEffects(activeUserId: Int, gameId: Int = 7) {
        emitGameStatus(GameStatus.IN_PROGRESS)
        emitActiveGameId(gameId)
        emitActivePlayerId(activeUserId)
        emitGamePhase(GamePhase.RESOLVE_EFFECTS)
    }

    private fun FakeWebSocketClient.enterRadioTowerRerollDecision(
        activeUserId: Int = 42,
        activePlayerDatabaseId: Int = 7,
        gameId: Int = 7,
    ) {
        emitGameStatus(GameStatus.IN_PROGRESS)
        emitActiveGameId(gameId)

        emitPlayers(
            listOf(
                PlayerCoinState(
                    id = activePlayerDatabaseId.toString(),
                    displayName = "alice",
                    coins = 5,
                    isCurrentPlayer = true,
                    isActivePlayer = true
                ),
            )
        )

        emitPlayerLandmarks(
            mapOf(
                activePlayerDatabaseId to listOf(
                    PlayerLandmarkState(LandmarkType.RADIO_TOWER, isBuilt = true)
                )
            )
        )

        emitDiceResult(listOf(6))

        emitPlayerCards(
            mapOf(
                activePlayerDatabaseId to listOf(
                    PlayerCardState(CardType.STADIUM, quantity = 1)
                )
            )
        )

        emitActivePlayerId(activeUserId)
        emitGamePhase(GamePhase.RESOLVE_EFFECTS)
    }
    @Test
    fun performTurnFlowActionNoLongerResolvesEffectsManually() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.enterResolveEffects(activeUserId = 42)
        runCurrent()

        viewModel.performTurnFlowAction()

        assertNull(fakeClient.resolvedEffectsGameId)
        assertEquals(0, fakeClient.resolveEffectsCallCount)
    }

    @Test
    fun resolveEffectsAutoSendsAfterDwellForActivePlayer() = runTest {
        val fakeClient = FakeWebSocketClient()
        viewModel(fakeClient, userId = 42)

        fakeClient.enterResolveEffects(activeUserId = 42)
        advanceUntilIdle()

        assertEquals(7, fakeClient.resolvedEffectsGameId)
        assertEquals(1, fakeClient.resolveEffectsCallCount)
        assertNull(fakeClient.endedTurnGameId)
        assertNull(fakeClient.advancedPhaseGameId)
    }

    @Test
    fun resolveEffectsAutoSendsImmediatelyWhenDiceRolledAndNoCardsTriggered() = runTest {
        val fakeClient = FakeWebSocketClient()
        viewModel(fakeClient, userId = 42)

        fakeClient.enterResolveEffects(activeUserId = 42)
        fakeClient.emitDiceResult(listOf(6, 6))
        runCurrent()

        assertEquals(7, fakeClient.resolvedEffectsGameId)
        assertEquals(1, fakeClient.resolveEffectsCallCount)
    }

    @Test
    fun resolveEffectsDoesNotAutoSendBeforeShortDwellElapses() = runTest {
        val dwell = GameScreenViewModel.DEFAULT_RESOLVE_EFFECTS_DWELL_MS
        val fakeClient = FakeWebSocketClient()
        viewModel(fakeClient, userId = 42, resolveEffectsDwellMillis = dwell)

        fakeClient.enterResolveEffects(activeUserId = 42)
        runCurrent()
        advanceTimeBy(1_999L)
        runCurrent()

        assertEquals(0, fakeClient.resolveEffectsCallCount)

        advanceUntilIdle()

        assertEquals(1, fakeClient.resolveEffectsCallCount)
    }

    @Test
    fun resolveEffectsDoesNotAutoSendWhileRadioTowerDecisionIsPending() = runTest {
        val dwell = GameScreenViewModel.DEFAULT_RESOLVE_EFFECTS_DWELL_MS
        val fakeClient = FakeWebSocketClient()
        viewModel(fakeClient, userId = 42, resolveEffectsDwellMillis = dwell)

        fakeClient.enterRadioTowerRerollDecision()
        runCurrent()
        advanceTimeBy(dwell + 1)
        runCurrent()

        assertEquals(0, fakeClient.resolveEffectsCallCount)
    }

    @Test
    fun resolveEffectsAutoSendsAfterShortDwellForSingleTriggeredCard() = runTest {
        val fakeClient = FakeWebSocketClient()
        viewModel(fakeClient, userId = 42)

        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitActiveGameId(7)
        fakeClient.emitPlayers(
            listOf(
                PlayerCoinState(
                    id = "7",
                    displayName = "alice",
                    coins = 5,
                    isCurrentPlayer = true,
                    isActivePlayer = true
                )
            )
        )
        fakeClient.emitDiceResult(listOf(1))
        fakeClient.emitPlayerCards(
            mapOf(
                7 to listOf(
                    PlayerCardState(CardType.WHEAT_FIELD, quantity = 1)
                )
            )
        )
        fakeClient.emitActivePlayerId(42)
        fakeClient.emitGamePhase(GamePhase.RESOLVE_EFFECTS)

        runCurrent()
        advanceTimeBy(1_999L)
        runCurrent()

        assertEquals(0, fakeClient.resolveEffectsCallCount)

        advanceTimeBy(1L)
        runCurrent()

        assertEquals(1, fakeClient.resolveEffectsCallCount)
    }

    @Test
    fun resolveEffectsAutoSendsAfterMediumDwellForFewTriggeredCards() = runTest {
        val fakeClient = FakeWebSocketClient()
        viewModel(fakeClient, userId = 42)

        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitActiveGameId(7)
        fakeClient.emitPlayers(
            listOf(
                PlayerCoinState(
                    id = "7",
                    displayName = "alice",
                    coins = 5,
                    isCurrentPlayer = true,
                    isActivePlayer = true
                )
            )
        )
        fakeClient.emitDiceResult(listOf(1))
        fakeClient.emitPlayerCards(
            mapOf(
                7 to listOf(
                    PlayerCardState(CardType.WHEAT_FIELD, quantity = 2)
                )
            )
        )
        fakeClient.emitActivePlayerId(42)
        fakeClient.emitGamePhase(GamePhase.RESOLVE_EFFECTS)

        runCurrent()
        advanceTimeBy(3_999L)
        runCurrent()

        assertEquals(0, fakeClient.resolveEffectsCallCount)

        advanceTimeBy(1L)
        runCurrent()

        assertEquals(1, fakeClient.resolveEffectsCallCount)
    }

    @Test
    fun skipRerollAcceptsDiceResultAndResolvesEffects() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)
        fakeClient.enterRadioTowerRerollDecision()
        runCurrent()

        viewModel.skipReroll()
        runCurrent()

        assertEquals(7, fakeClient.resolvedEffectsGameId)
        assertEquals(1, fakeClient.resolveEffectsCallCount)
        assertFalse(viewModel.canRerollThisTurn.value)
    }

    @Test
    fun resolveEffectsDoesNotAutoSendForNonActivePlayer() = runTest {
        val fakeClient = FakeWebSocketClient()
        viewModel(fakeClient, userId = 1)

        fakeClient.enterResolveEffects(activeUserId = 42)
        advanceUntilIdle()

        assertEquals(0, fakeClient.resolveEffectsCallCount)
    }

    @Test
    fun resolveEffectsDoesNotAutoSendWhenGameIsNotInProgress() = runTest {
        val fakeClient = FakeWebSocketClient()
        viewModel(fakeClient, userId = 42)

        fakeClient.emitGameStatus(GameStatus.WAITING)
        fakeClient.emitActiveGameId(7)
        fakeClient.emitActivePlayerId(42)
        fakeClient.emitGamePhase(GamePhase.RESOLVE_EFFECTS)
        advanceUntilIdle()

        assertEquals(0, fakeClient.resolveEffectsCallCount)
    }

    @Test
    fun resolveEffectsAutoSendIsCancelledWhenPhaseLeavesResolveEffectsMidDwell() = runTest {
        val dwell = GameScreenViewModel.DEFAULT_RESOLVE_EFFECTS_DWELL_MS
        val fakeClient = FakeWebSocketClient()
        viewModel(fakeClient, userId = 42, resolveEffectsDwellMillis = dwell)

        fakeClient.enterResolveEffects(activeUserId = 42)
        runCurrent()
        advanceTimeBy(dwell / 2)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        advanceUntilIdle()

        assertEquals(0, fakeClient.resolveEffectsCallCount)
    }

    @Test
    fun resolveEffectsAutoSendFiresOnlyOncePerEntry() = runTest {
        val fakeClient = FakeWebSocketClient()
        viewModel(fakeClient, userId = 42)

        fakeClient.enterResolveEffects(activeUserId = 42)
        advanceUntilIdle()
        fakeClient.emitGamePhase(GamePhase.RESOLVE_EFFECTS)
        advanceUntilIdle()

        assertEquals(1, fakeClient.resolveEffectsCallCount)
    }

    @Test
    fun buyBuildPhaseActionEndsTurnByActivePlayer() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitActiveGameId(7)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.performTurnFlowAction()

        assertEquals(7, fakeClient.endedTurnGameId)
        assertNull(fakeClient.advancedPhaseGameId)
        assertNull(fakeClient.resolvedEffectsGameId)
    }

    @Test
    fun endTurnPhaseActionIsIgnoredBecauseServerEndsTurnsFromBuyBuild() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitActiveGameId(7)
        fakeClient.emitGamePhase(GamePhase.END_TURN)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.performTurnFlowAction()

        assertNull(fakeClient.endedTurnGameId)
        assertNull(fakeClient.advancedPhaseGameId)
        assertNull(fakeClient.resolvedEffectsGameId)
    }

    @Test
    fun turnFlowActionIsIgnoredWhenNotActivePlayer() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 1)

        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitActiveGameId(7)
        fakeClient.emitGamePhase(GamePhase.END_TURN)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.performTurnFlowAction()

        assertNull(fakeClient.endedTurnGameId)
    }

    @Test
    fun turnFlowActionIsIgnoredWhenGameIsNotInProgress() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitGameStatus(GameStatus.WAITING)
        fakeClient.emitActiveGameId(7)
        fakeClient.emitGamePhase(GamePhase.RESOLVE_EFFECTS)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.performTurnFlowAction()

        assertNull(fakeClient.resolvedEffectsGameId)
    }

    @Test
    fun turnFlowActionIsIgnoredWithoutGameId() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.performTurnFlowAction()

        assertNull(fakeClient.advancedPhaseGameId)
    }

    // === Debug End-game (#191) ===

    @Test
    fun endGameCallsDebugApiWithCurrentGameId() = runTest {
        val fakeClient = FakeWebSocketClient()
        val fakeDebugApi = FakeDebugApi()
        val viewModel = viewModel(fakeClient = fakeClient, userId = 42, fakeDebugApi = fakeDebugApi)
        fakeClient.emitActiveGameId(7)
        advanceUntilIdle()
        val errors = mutableListOf<String>()
        val job = launch { viewModel.debugEndGameErrors.collect { errors.add(it) } }
        advanceUntilIdle()

        viewModel.endGame()
        advanceUntilIdle()

        assertEquals(1, fakeDebugApi.endGameCallCount)
        assertEquals(EndGameRequest(gameId = 7), fakeDebugApi.lastEndGameRequest)
        // On a successful response no error is surfaced — GAME_END drives the transition.
        assertEquals(emptyList<String>(), errors)
        job.cancel()
    }

    @Test
    fun endGameIsIgnoredWithoutGameId() = runTest {
        val fakeDebugApi = FakeDebugApi()
        val viewModel = viewModel(fakeDebugApi = fakeDebugApi)
        advanceUntilIdle()

        viewModel.endGame()
        advanceUntilIdle()

        assertEquals(0, fakeDebugApi.endGameCallCount)
    }

    @Test
    fun endGameEmitsErrorOnNonSuccessfulResponse() = runTest {
        val fakeClient = FakeWebSocketClient()
        val fakeDebugApi = FakeDebugApi(response = Response.error(422, "".toResponseBody()))
        val viewModel = viewModel(fakeClient = fakeClient, userId = 42, fakeDebugApi = fakeDebugApi)
        fakeClient.emitActiveGameId(7)
        advanceUntilIdle()
        val errors = mutableListOf<String>()
        val job = launch { viewModel.debugEndGameErrors.collect { errors.add(it) } }
        advanceUntilIdle()

        viewModel.endGame()
        advanceUntilIdle()

        assertEquals(listOf("End game failed (422)"), errors)
        job.cancel()
    }

    @Test
    fun endGameEmitsErrorOnException() = runTest {
        val fakeClient = FakeWebSocketClient()
        val fakeDebugApi = FakeDebugApi(throwError = true)
        val viewModel = viewModel(fakeClient = fakeClient, userId = 42, fakeDebugApi = fakeDebugApi)
        fakeClient.emitActiveGameId(7)
        advanceUntilIdle()
        val errors = mutableListOf<String>()
        val job = launch { viewModel.debugEndGameErrors.collect { errors.add(it) } }
        advanceUntilIdle()

        viewModel.endGame()
        advanceUntilIdle()

        assertEquals(1, errors.size)
        assertTrue(errors.first().startsWith("End game error"))
        job.cancel()
    }

    @Test
    fun endGameEmitsUnknownErrorWhenExceptionMessageIsNull() = runTest {
        val fakeClient = FakeWebSocketClient()
        val fakeDebugApi = FakeDebugApi(throwError = true, errorMessage = null)
        val viewModel = viewModel(fakeClient = fakeClient, userId = 42, fakeDebugApi = fakeDebugApi)
        fakeClient.emitActiveGameId(7)
        advanceUntilIdle()
        val errors = mutableListOf<String>()
        val job = launch { viewModel.debugEndGameErrors.collect { errors.add(it) } }
        advanceUntilIdle()

        viewModel.endGame()
        advanceUntilIdle()

        assertEquals(listOf("End game error: unknown error"), errors)
        job.cancel()
    }

    @Test
    fun endGameIgnoresRapidSecondTapWhileInFlight() = runTest {
        val fakeClient = FakeWebSocketClient()
        val fakeDebugApi = FakeDebugApi()
        val viewModel = viewModel(fakeClient = fakeClient, userId = 42, fakeDebugApi = fakeDebugApi)
        fakeClient.emitActiveGameId(7)
        advanceUntilIdle()

        viewModel.endGame()
        viewModel.endGame() // rapid second tap before the first call completes
        advanceUntilIdle()

        assertEquals(1, fakeDebugApi.endGameCallCount)
    }

    // -- Cheating accusations (#280) ------------------------------------------

    @Test
    fun accuseForwardsGameIdAndAccusedPlayerIdToClient() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)
        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        advanceUntilIdle()

        viewModel.accuse(22)

        assertEquals(listOf(7 to 22), fakeClient.accusations)
    }

    @Test
    fun accuseIsIgnoredWhenGameIsNotInProgress() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)
        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.WAITING)
        advanceUntilIdle()

        viewModel.accuse(22)

        assertTrue(fakeClient.accusations.isEmpty())
    }

    @Test
    fun secondAccusationInTheSameTurnIsIgnored() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)
        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        advanceUntilIdle()

        viewModel.accuse(22)
        viewModel.accuse(33)

        assertEquals(listOf(7 to 22), fakeClient.accusations)
        assertFalse(viewModel.canAccuseThisTurn.value)
    }

    @Test
    fun accusationBudgetRenewsWhenTheActivePlayerRotates() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)
        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.accuse(22)
        fakeClient.emitActivePlayerId(99)
        advanceUntilIdle()

        assertTrue(viewModel.canAccuseThisTurn.value)
        viewModel.accuse(33)
        assertEquals(listOf(7 to 22, 7 to 33), fakeClient.accusations)
    }

    @Test
    fun accusationBudgetRenewsWhenTheRoundAdvances() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)
        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitRoundNumber(1)
        advanceUntilIdle()

        viewModel.accuse(22)
        fakeClient.emitRoundNumber(2)
        advanceUntilIdle()

        assertTrue(viewModel.canAccuseThisTurn.value)
        viewModel.accuse(33)
        assertEquals(listOf(7 to 22, 7 to 33), fakeClient.accusations)
    }

    @Test
    fun accusationBudgetSurvivesADisconnectNullEmissionWithinTheSameTurn() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)
        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.accuse(22)
        fakeClient.emitActivePlayerId(null)
        advanceUntilIdle()
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        assertFalse(viewModel.canAccuseThisTurn.value)
        viewModel.accuse(33)
        assertEquals(listOf(7 to 22), fakeClient.accusations)
    }

    @Test
    fun accusationBudgetSurvivesADisconnectNullRoundEmission() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)
        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitRoundNumber(3)
        advanceUntilIdle()

        viewModel.accuse(22)
        fakeClient.emitRoundNumber(null)
        advanceUntilIdle()
        fakeClient.emitRoundNumber(3)
        advanceUntilIdle()

        assertFalse(viewModel.canAccuseThisTurn.value)
    }

    @Test
    fun accusationBudgetRenewsInAFreshGame() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)
        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        advanceUntilIdle()

        viewModel.accuse(22)
        assertFalse(viewModel.canAccuseThisTurn.value)

        fakeClient.emitActiveGameId(8)
        advanceUntilIdle()

        assertTrue(viewModel.canAccuseThisTurn.value)
    }

    @Test
    fun onShakeReportsCheatWhenARecommendationIsProduced() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)
        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitActivePlayerId(42)
        fakeClient.emitPlayers(
            listOf(
                PlayerCoinState(
                    id = "11",
                    displayName = "alice",
                    coins = 3,
                    isCurrentPlayer = true,
                    isActivePlayer = true,
                )
            )
        )
        fakeClient.emitMarketplace(mapOf(CardType.WHEAT_FIELD to 6))
        advanceUntilIdle()

        viewModel.onShake()

        assertEquals(1, fakeClient.reportCheatCalls)
    }

    @Test
    fun onShakeDoesNotReportCheatWhenNoRecommendationIsProduced() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)
        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitActivePlayerId(42)
        fakeClient.emitPlayers(
            listOf(
                PlayerCoinState(
                    id = "11",
                    displayName = "alice",
                    coins = 3,
                    isCurrentPlayer = true,
                    isActivePlayer = true,
                )
            )
        )
        fakeClient.emitMarketplace(emptyMap())
        advanceUntilIdle()

        viewModel.onShake()

        assertEquals(0, fakeClient.reportCheatCalls)
    }

    @Test
    fun endGameEmitsNetworkErrorMessageOnIoException() = runTest {
        val fakeClient = FakeWebSocketClient()
        val fakeDebugApi = FakeDebugApi(error = IOException("connect timed out"))
        val viewModel = viewModel(fakeClient = fakeClient, userId = 42, fakeDebugApi = fakeDebugApi)
        fakeClient.emitActiveGameId(7)
        advanceUntilIdle()
        val errors = mutableListOf<String>()
        val job = launch { viewModel.debugEndGameErrors.collect { errors.add(it) } }
        advanceUntilIdle()

        viewModel.endGame()
        advanceUntilIdle()

        assertEquals(listOf("End game error: Network error: connect timed out"), errors)
        job.cancel()
    }

    @Test
    fun endGameEmitsParsedServerMessageOnHttpJsonError() = runTest {
        val fakeClient = FakeWebSocketClient()
        val errorBody = """{"errorCode":"GAME_NOT_FOUND","message":"Game does not exist"}"""
            .toResponseBody("application/json".toMediaType())
        val fakeDebugApi = FakeDebugApi(error = HttpException(Response.error<Unit>(404, errorBody)))
        val viewModel = viewModel(fakeClient = fakeClient, userId = 42, fakeDebugApi = fakeDebugApi)
        fakeClient.emitActiveGameId(7)
        advanceUntilIdle()
        val errors = mutableListOf<String>()
        val job = launch { viewModel.debugEndGameErrors.collect { errors.add(it) } }
        advanceUntilIdle()

        viewModel.endGame()
        advanceUntilIdle()

        assertEquals(listOf("End game error: Game does not exist"), errors)
        job.cancel()
    }

    @Test
    fun purchaseFailureWithBlankMessageShowsFallbackMessage() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        fakeClient.emitGamePhase(GamePhase.BUY_OR_BUILD)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.purchase("BAKERY")
        fakeClient.emitPurchaseEvent(PurchaseEvent.Failure(""))
        advanceUntilIdle()

        assertEquals(PurchaseState.ERROR, viewModel.state.value.purchaseState)
        assertEquals("Purchase failed", viewModel.state.value.purchaseMessage)
    }

    private class FakeDebugApi(
        private val response: Response<Unit> = Response.success(Unit),
        private val throwError: Boolean = false,
        private val errorMessage: String? = "Simulated network error",
        private val error: Throwable? = null,
    ) : DebugApi {
        var endGameCallCount = 0
            private set
        var lastEndGameRequest: EndGameRequest? = null
            private set

        override suspend fun fillLobby(body: FillLobbyRequest): Response<Unit> = Response.success(Unit)
        override suspend fun resetLobby(body: ResetLobbyRequest): Response<Unit> = Response.success(Unit)
        override suspend fun purge(): Response<Unit> = Response.success(Unit)

        override suspend fun endGame(body: EndGameRequest): Response<Unit> {
            endGameCallCount++
            lastEndGameRequest = body
            error?.let { throw it }
            if (throwError) throw RuntimeException(errorMessage)
            return response
        }
    }

    @Test
    fun chatMessagesAreClearedInAFreshGame() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient)
        fakeClient.emitActiveGameId(7)
        fakeClient.emitGameStatus(GameStatus.IN_PROGRESS)
        advanceUntilIdle()

        fakeClient.emitChatMessage(ChatMessageState("Alice","Hello"))
        advanceUntilIdle()
        assertEquals(listOf("Hello"), viewModel.state.value.chatMessages.map { it.message })

        fakeClient.emitActiveGameId(8)
        advanceUntilIdle()

        assertEquals(emptyList<String>(), viewModel.state.value.chatMessages.map { it.message })
    }
}
