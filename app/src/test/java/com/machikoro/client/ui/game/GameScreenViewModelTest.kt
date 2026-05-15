package com.machikoro.client.ui.game

import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.model.state.ConnectionStatus
import com.machikoro.client.domain.model.state.PlayerCoinState
import com.machikoro.client.domain.session.Session
import com.machikoro.client.domain.session.SessionStateHolder
import com.machikoro.client.network.websocket.FakeWebSocketClient
import com.machikoro.client.ui.start.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

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
    ) = GameScreenViewModel(fakeClient, fakeSession(userId))

    @Test
    fun initialStateUsesInitialValues() = runTest {
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(GamePhase.NONE, viewModel.state.value.gamePhase)
        assertEquals(ConnectionStatus.IDLE, viewModel.state.value.connectionStatus)
        assertEquals(emptyList<PlayerCoinState>(), viewModel.state.value.players)
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
    fun rollDiceForwardsDiceCountToClientWhenPhaseIsRollDiceAndIsActivePlayer() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

        fakeClient.emitGamePhase(GamePhase.ROLL_DICE)
        fakeClient.emitActivePlayerId(42)
        advanceUntilIdle()

        viewModel.rollDice(diceCount = 1)

        assertEquals(1, fakeClient.lastRolledDiceCount)
    }

    @Test
    fun rollDiceIsIgnoredWhenPhaseIsNotRollDice() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

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

        fakeClient.emitGamePhase(GamePhase.ROLL_DICE)
        fakeClient.emitActivePlayerId(99)
        advanceUntilIdle()

        viewModel.rollDice(diceCount = 1)

        assertNull(fakeClient.lastRolledDiceCount)
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
    fun isRollingIsFalseInInitialState() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isRolling)
    }

    @Test
    fun rollDiceSetsIsRollingToTrue() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

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
    fun rollDiceDoesNotSetIsRollingWhenPhaseIsNotRollDice() = runTest {
        val fakeClient = FakeWebSocketClient()
        val viewModel = viewModel(fakeClient, userId = 42)

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

        fakeClient.emitGamePhase(GamePhase.ROLL_DICE)
        fakeClient.emitActivePlayerId(99)
        advanceUntilIdle()

        viewModel.rollDice(diceCount = 1)

        assertFalse(viewModel.state.value.isRolling)
    }
}