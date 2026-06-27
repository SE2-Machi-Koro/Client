package com.machikoro.client.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.machikoro.client.domain.cheat.recommendBestBuy
import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.model.shop.ShopCatalog
import com.machikoro.client.domain.model.shop.ShopItem
import com.machikoro.client.domain.model.shop.PurchaseEvent
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.domain.model.state.PurchaseState
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.session.SessionStateHolder
import com.machikoro.client.domain.model.state.isAlreadyOwnedPurpleEstablishment
import com.machikoro.client.domain.model.state.isShopItemAvailableFromMarketplace
import com.machikoro.client.network.debug.DebugApi
import com.machikoro.client.network.debug.EndGameRequest
import com.machikoro.client.network.toClientError
import com.machikoro.client.network.toUserMessage
import com.machikoro.client.network.websocket.WebSocketClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import com.machikoro.client.domain.model.state.AccusationResult
import com.machikoro.client.domain.model.state.triggeredEstablishmentCountForCurrentRoll
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Failsafe for issue #175: the server is expected to answer a roll with a
// diceResult, but if that message is lost or delayed the UI must not animate forever.
private const val DICE_ROLL_TIMEOUT_MS = 10_000L
private const val PURCHASE_DISPLAY_DELAY = 5_000L

class GameScreenViewModel(
    private val webSocketClient: WebSocketClient,
    private val sessionStateHolder: SessionStateHolder,
    private val debugApi: DebugApi,
    private val resolveEffectsDwellMillis: Long = DEFAULT_RESOLVE_EFFECTS_DWELL_MS,
) : ViewModel() {
    val state: StateFlow<GameScreenState>
        get() = mutableState.asStateFlow()

    private val mutableState = MutableStateFlow(GameScreenState.initial())

    /** Insider Trading cheat (#203): the recommended card for this turn, or null when inactive. */
    val cheatRecommendation: StateFlow<CardType?>
        get() = mutableCheatRecommendation.asStateFlow()
    private val mutableCheatRecommendation = MutableStateFlow<CardType?>(null)

    /** One-shot signal each time a shake produces a recommendation (drives the toast). */
    val cheatActivations: SharedFlow<CardType?>
        get() = mutableCheatActivations.asSharedFlow()

    /** One-shot cheating-accusation result (#280) for a toast — pass-through from the WS client. */
    val accusationResults: SharedFlow<AccusationResult>
        get() = webSocketClient.accusationResults

    /** One-shot local-player coin delta from effect resolution (#389) — drives coin SFX. */
    val coinDeltas: SharedFlow<Int>
        get() = webSocketClient.coinDeltas

    /** One-shot server-side accusation rejection (#280) for a toast — pass-through. */
    val accusationErrors: SharedFlow<String>
        get() = webSocketClient.accusationErrors

    /**
     * True until the local player has accused someone this turn — mirrors the
     * server's one-accusation-per-turn rule (#280). Renews when the turn
     * rotates or a new game starts.
     *
     * Turn rotation is detected via the last *non-null* active player / round:
     * a disconnect (e.g. backgrounding the app) emits null and the reconnect
     * snapshot restores the same in-progress turn, so null transitions must
     * not renew a spent budget.
     */
    val canAccuseThisTurn: StateFlow<Boolean>
        get() = mutableCanAccuseThisTurn.asStateFlow()
    private val mutableCanAccuseThisTurn = MutableStateFlow(true)

    /**
     * Radio Tower reroll budget (#326). True until the active player spends their
     * once-per-turn reroll; renews when the turn rotates or a new game starts,
     * using the same non-null discipline as the accusation budget. The server is
     * authoritative — this just stops the client re-sending within a turn.
     */
    val canRerollThisTurn: StateFlow<Boolean>
        get() = mutableCanRerollThisTurn.asStateFlow()
    private val mutableCanRerollThisTurn = MutableStateFlow(true)
    private var lastTurnOwnerUserId: Int? = null
    private var lastSeenRoundNumber: Int? = null
    private val mutableCheatActivations = MutableSharedFlow<CardType?>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Debug End-game (#191): one-shot failure message, drives a snackbar. */
    val debugEndGameErrors: SharedFlow<String>
        get() = mutableDebugEndGameErrors.asSharedFlow()
    private val mutableDebugEndGameErrors = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /**
     * Pending auto-advance out of RESOLVE_EFFECTS (#302). Cancelled when the
     * auto-resolve condition stops holding (see the observer in [init]) so a
     * stale timer never fires after the turn has moved on.
     */
    private var resolveEffectsJob: Job? = null
    private var diceRollTimeoutJob: Job? = null
    private var activeRollId: Int? = null
    private var nextRollId = 0
    private var pendingRollPhase: GamePhase? = null

    init {
        viewModelScope.launch {
            webSocketClient.activeGameId.collect { gameId ->
                // A different game means a fresh accusation budget (#280) and a
                // fresh reroll budget (#326).
                if (gameId != null && mutableState.value.gameId != gameId) {
                    mutableCanAccuseThisTurn.value = true
                    mutableCanRerollThisTurn.value = true
                }
                mutableState.update { current ->
                    current.copy(gameId = gameId)
                }
            }
        }
        viewModelScope.launch {
            webSocketClient.connectionStatus.collect { connectionStatus ->
                mutableState.update { it.copy(connectionStatus = connectionStatus) }
            }
        }
        viewModelScope.launch {
            webSocketClient.gamePhase.collect { gamePhase ->
                var shouldCancelPendingRoll = false
                mutableState.update { state ->
                    // A new roll phase means a fresh roll: drop the previous turn's
                    // dice. The server never sends a null diceResult between turns, so
                    // without this the stale result lingers — hiding the Roll Dice
                    // button (its condition is diceResult == null) and leaving the last
                    // number and a frozen die on screen.
                    val enteringRollDice =
                        gamePhase == GamePhase.ROLL_DICE && state.gamePhase != GamePhase.ROLL_DICE
                    state.copy(
                        gamePhase = gamePhase,
                        diceResult = if (enteringRollDice) null else state.diceResult,
                    )
                        .resetPurchaseFeedbackIf(gamePhase != GamePhase.BUY_OR_BUILD)
                        .let { updated ->
                            // Issue #175: if the server advances the phase without a
                            // diceResult event, stop only the roll tied to the old phase.
                            if (
                                state.isRolling &&
                                pendingRollPhase != null &&
                                gamePhase != pendingRollPhase
                            ) {
                                shouldCancelPendingRoll = true
                                updated.copy(isRolling = false)
                            } else {
                                updated
                            }
                        }
                }
                if (shouldCancelPendingRoll) {
                    cancelPendingRollTimeout()
                }
            }
        }
        viewModelScope.launch {
            webSocketClient.players.collect { players ->
                mutableState.update { it.copy(players = players) }
            }
        }
        viewModelScope.launch {
            webSocketClient.diceResult.collect { diceResult ->
                cancelPendingRollTimeout()
                mutableState.update { it.copy(diceResult = diceResult, isRolling = false) }
            }
        }
        viewModelScope.launch {
            // #346: drives the non-active player's roll/reroll animation. Bumped
            // only on a real DICE_ROLLED/DICE_REROLLED frame, so a same-turn reroll
            // animates while the snapshot collapse ([x, y] -> [total]) does not.
            webSocketClient.diceRollTick.collect { tick ->
                mutableState.update { it.copy(diceRollTick = tick) }
            }
        }
        viewModelScope.launch {
            webSocketClient.activePlayerId.collect { activePlayerId ->
                // The Insider Trading cheat is valid for one turn only — drop it when the turn rotates.
                if (mutableState.value.activePlayerId != activePlayerId) {
                    mutableCheatRecommendation.value = null
                }
                // The accusation budget (#280) renews only on a real rotation:
                // a new non-null owner differing from the last non-null one.
                if (activePlayerId != null) {
                    if (lastTurnOwnerUserId != null && activePlayerId != lastTurnOwnerUserId) {
                        mutableCanAccuseThisTurn.value = true
                        mutableCanRerollThisTurn.value = true
                    }
                    lastTurnOwnerUserId = activePlayerId
                }
                mutableState.update { state ->
                    state.copy(activePlayerId = activePlayerId)
                        .resetPurchaseFeedbackIf(state.activePlayerId != activePlayerId)
                }
            }
        }
        viewModelScope.launch {
            webSocketClient.gameStatus.collect { gameStatus ->
                mutableState.update { it.copy(gameStatus = gameStatus) }
            }
        }
        viewModelScope.launch {
            webSocketClient.winnerId.collect { winnerId ->
                mutableState.update { it.copy(winnerId = winnerId) }
            }
        }
        viewModelScope.launch {
            webSocketClient.roundNumber.collect { roundNumber ->
                if (mutableState.value.roundNumber != roundNumber) {
                    mutableCheatRecommendation.value = null
                }
                // Same non-null discipline as the active-player reset above.
                if (roundNumber != null) {
                    if (lastSeenRoundNumber != null && roundNumber != lastSeenRoundNumber) {
                        mutableCanAccuseThisTurn.value = true
                        mutableCanRerollThisTurn.value = true
                    }
                    lastSeenRoundNumber = roundNumber
                }
                mutableState.update { state ->
                    state.copy(roundNumber = roundNumber)
                        .resetPurchaseFeedbackIf(state.roundNumber != roundNumber)
                }
            }
        }
        viewModelScope.launch {
            webSocketClient.playerCards.collect { playerCards ->
                mutableState.update { it.copy(playerCards = playerCards) }
            }
        }
        viewModelScope.launch {
            webSocketClient.playerLandmarks.collect { playerLandmarks ->
                mutableState.update { it.copy(playerLandmarks = playerLandmarks) }
            }
        }
        viewModelScope.launch {
            webSocketClient.marketplace.collect { marketplace ->
                mutableState.update { it.copy(marketplace = marketplace) }
            }
        }
        viewModelScope.launch {
            webSocketClient.shopItems.collect { shopItems ->
                mutableState.update { it.copy(shopItems = shopItems) }
            }
        }
        viewModelScope.launch {
            webSocketClient.purchaseEvents.collect { event ->
                val before = mutableState.value
                mutableState.update { state -> state.applyPurchaseEvent(event) }
                if (
                    event is PurchaseEvent.Success &&
                    before.pendingPurchaseItemType == event.itemType &&
                    before.gameId != null &&
                    before.gameStatus == GameStatus.IN_PROGRESS &&
                    before.gamePhase == GamePhase.BUY_OR_BUILD &&
                    before.isActivePlayer
                ) {
                    delay(PURCHASE_DISPLAY_DELAY)
                    webSocketClient.endTurn(before.gameId)
                }
            }
        }
        viewModelScope.launch {
            sessionStateHolder.session.collect { session ->
                mutableState.update { it.copy(myUserId = session?.userId) }
            }
        }
        // #302: auto-advance the Resolving Effects phase. Driven off the
        // aggregated state (not a single flow) so it is immune to the order in
        // which phase/activePlayer/status/gameId settle. distinctUntilChanged
        // means the timer is (re)scheduled exactly once per entry into the
        // condition and cancelled exactly once on leaving it.
        viewModelScope.launch {
            mutableState.combine(mutableCanRerollThisTurn) { state, canRerollThisTurn ->
                state.shouldAutoResolveEffects(canRerollThisTurn)
            }
                .distinctUntilChanged()
                .collect { shouldAutoResolve ->
                    resolveEffectsJob?.cancel()
                    if (shouldAutoResolve) {
                        resolveEffectsJob = viewModelScope.launch {
                            val dwellMillis = mutableState.value.resolveEffectsDwellMillis()

                            if (dwellMillis > 0) {
                                delay(dwellMillis)
                            }

                            val now = mutableState.value
                            if (now.shouldAutoResolveEffects(mutableCanRerollThisTurn.value)) {
                                now.gameId?.let { webSocketClient.resolveEffects(it) }
                            }
                        }
                    }
                }
        }
        viewModelScope.launch {
            webSocketClient.chatMessages.collect { chat ->
                mutableState.update { it.copy(chatMessages = it.chatMessages + chat) }
            }
        }
    }

    fun rollDice(diceCount: Int = 1) {
        if (mutableState.value.gameStatus != GameStatus.IN_PROGRESS) return
        if (mutableState.value.gamePhase != GamePhase.ROLL_DICE) return
        if (!mutableState.value.isActivePlayer) return
        // Prevent rapid taps from sending concurrent roll requests and keeping
        // the dice animation stuck in the rolling state.
        if (mutableState.value.isRolling) return

        mutableState.update { it.copy(isRolling = true, requestedDiceCount = diceCount) }
        startRollTimeout(expectedPhase = GamePhase.ROLL_DICE)
        webSocketClient.rollDice(diceCount)
    }

    /**
     * Radio Tower reroll (#326). Re-rolls the active player's dice during
     * RESOLVE_EFFECTS when they have built a Radio Tower ([GameScreenState.canReroll])
     * and still have their once-per-turn budget ([canRerollThisTurn]). No-op
     * otherwise; the server stays authoritative for the result and phase.
     */
    fun rerollDice(diceCount: Int = 1) {
        if (!mutableState.value.canReroll) return
        if (!mutableCanRerollThisTurn.value) return
        if (mutableState.value.isRolling) return
        mutableCanRerollThisTurn.value = false
        mutableState.update { it.copy(isRolling = true) }
        startRollTimeout(expectedPhase = GamePhase.RESOLVE_EFFECTS)
        webSocketClient.rerollDice(diceCount)
    }

    fun skipReroll() {
        val current = mutableState.value
        val gameId = current.gameId ?: return
        if (!current.canReroll) return
        if (!mutableCanRerollThisTurn.value) return
        if (current.isRolling) return

        mutableCanRerollThisTurn.value = false
        resolveEffectsJob?.cancel()
        webSocketClient.resolveEffects(gameId)
    }

    private fun startRollTimeout(expectedPhase: GamePhase) {
        diceRollTimeoutJob?.cancel()
        val rollId = ++nextRollId
        activeRollId = rollId
        pendingRollPhase = expectedPhase
        diceRollTimeoutJob = viewModelScope.launch {
            delay(DICE_ROLL_TIMEOUT_MS)
            if (activeRollId == rollId) {
                activeRollId = null
                pendingRollPhase = null
                diceRollTimeoutJob = null
                mutableState.update { current ->
                    if (current.isRolling) current.copy(isRolling = false) else current
                }
            }
        }
    }

    private fun cancelPendingRollTimeout() {
        diceRollTimeoutJob?.cancel()
        diceRollTimeoutJob = null
        activeRollId = null
        pendingRollPhase = null
    }

    /**
     * Insider Trading cheat (#203). On a shake during the local player's turn,
     * computes a local best-buy recommendation and emits a one-shot activation
     * signal for the toast. No-op off-turn or before the game is running.
     *
     * Also silently reports the cheat usage to the server (#280 / server #361) so
     * other players can later accuse this player of cheating.
     */
    fun onShake() {
        val current = mutableState.value
        if (current.gameStatus != GameStatus.IN_PROGRESS || !current.isActivePlayer) return
        val me = current.players.firstOrNull { it.isCurrentPlayer } ?: return
        val recommendation = recommendBestBuy(current, me)
        mutableCheatRecommendation.value = recommendation
        mutableCheatActivations.tryEmit(recommendation)
        // Only an actual recommendation counts as cheat usage (#280): a null
        // result means the cheat produced nothing, so the player must not
        // become catchable for it.
        if (recommendation != null) {
            current.gameId?.let { webSocketClient.reportCheat(it) }
        }
    }

    /**
     * Accuse [accusedPlayerId] (a PlayerModel.id) of cheating (#280). The server
     * adjudicates and the outcome arrives via [accusationResults]; coin changes
     * arrive through the normal authoritative snapshot. At most one accusation
     * per turn (mirrors the server rule); the budget renews when the turn
     * rotates.
     */
    fun accuse(accusedPlayerId: Int) {
        val current = mutableState.value
        if (current.gameStatus != GameStatus.IN_PROGRESS) return
        if (!mutableCanAccuseThisTurn.value) return
        val gameId = current.gameId ?: return
        mutableCanAccuseThisTurn.value = false
        webSocketClient.accuse(gameId, accusedPlayerId)
    }

    fun performTurnFlowAction() {
        val current = mutableState.value
        val gameId = current.gameId ?: return
        if (current.gameStatus != GameStatus.IN_PROGRESS) return
        if (!current.isActivePlayer) return

        when (current.gamePhase) {
            GamePhase.BUY_OR_BUILD -> webSocketClient.endTurn(gameId)
            // RESOLVE_EFFECTS advances automatically (#302) — no manual action.
            GamePhase.NONE,
            GamePhase.ROLL_DICE,
            GamePhase.RESOLVE_EFFECTS,
            GamePhase.END_TURN -> Unit
        }
    }

    /**
     * #302: true when the local player is the active player and the game is in
     * the Resolving Effects phase — the condition under which the client
     * auto-advances (see the collector in [init]). Only the active player sends
     * resolveEffects; everyone else waits for the server's next snapshot, so all
     * clients dwell on the phase together.
     */
    private fun GameScreenState.isInResolveEffectsAsActivePlayer(): Boolean =
        gameStatus == GameStatus.IN_PROGRESS &&
            gamePhase == GamePhase.RESOLVE_EFFECTS &&
            isActivePlayer &&
            gameId != null

    private fun GameScreenState.shouldAutoResolveEffects(canRerollThisTurn: Boolean): Boolean =
        isInResolveEffectsAsActivePlayer() &&
            !isRolling &&
            !(canReroll && canRerollThisTurn)

    private fun GameScreenState.resolveEffectsDwellMillis(): Long {
        val diceWasRolled = diceResult != null
        val triggeredCount = triggeredEstablishmentCountForCurrentRoll()

        return when {
            diceWasRolled && triggeredCount == 0 -> 0L
            triggeredCount == 1 -> 2_000L
            triggeredCount <= 3 -> 4_000L
            else -> resolveEffectsDwellMillis
        }
    }

    fun selectPurchaseItem(itemType: String) {
        val current = mutableState.value
        val availableItems = current.shopItems.ifEmpty { ShopCatalog.defaultItems }
        val item = availableItems.firstOrNull {
            it.type == itemType && current.isShopItemAvailableFromMarketplace(it)
        } ?: return
        if (!current.canSelectPurchaseItem(item)) return

        mutableState.update { state ->
            state.copy(
                selectedPurchaseItemType = item.type,
                purchaseFeedbackItemType = null,
                purchaseMessage = null
            )
        }
    }

    fun purchaseSelectedItem() {
        val selectedItemType = mutableState.value.selectedPurchaseItemType ?: return
        purchase(selectedItemType)
    }

    fun clearGameState() {
        webSocketClient.clearGameState()
    }

    fun purchase(itemType: String) {
        val current = mutableState.value
        val gameId = current.gameId ?: return
        val availableItems = current.shopItems.ifEmpty { ShopCatalog.defaultItems }
        val item = availableItems.firstOrNull {
            it.type == itemType && current.isShopItemAvailableFromMarketplace(it)
        } ?: return
        if (!current.canStartPurchase(item)) return

        mutableState.update { state ->
            state.copy(
                purchaseState = PurchaseState.PENDING,
                selectedPurchaseItemType = item.type,
                pendingPurchaseItemType = item.type,
                purchaseFeedbackItemType = item.type,
                purchaseMessage = "Buying ${item.displayName}..."
            )
        }
        webSocketClient.sendPurchase(
            gameId = gameId,
            purchaseType = item.purchaseType,
            cardType = item.type.takeIf { item.purchaseType == PurchaseType.ESTABLISHMENT },
            landmarkType = item.type.takeIf { item.purchaseType == PurchaseType.LANDMARK }
        )
    }

    fun sendChatMessage(message: String) {
        val current = mutableState.value
        val gameId = current.gameId ?: return
        if (message.isBlank()) return
        if (message.length > 300) return
        webSocketClient.sendChatMessage(gameId, message.trim())
    }

    private fun GameScreenState.canSelectPurchaseItem(item: ShopItem): Boolean =
        gameStatus == GameStatus.IN_PROGRESS &&
        isBuyingPhase &&
            isActivePlayer &&
            purchaseState != PurchaseState.PENDING &&
            purchaseState != PurchaseState.SUCCESS &&
            isShopItemAvailableFromMarketplace(item) &&
            hasEnoughKnownCoinsFor(item) &&
            !isAlreadyOwnedPurpleEstablishment(item) &&
            !isKnownBuiltLandmark(item)

    private fun GameScreenState.canStartPurchase(item: ShopItem): Boolean =
        gameStatus == GameStatus.IN_PROGRESS &&
        isBuyingPhase &&
            isActivePlayer &&
            purchaseState != PurchaseState.PENDING &&
            purchaseState != PurchaseState.SUCCESS &&
            isShopItemAvailableFromMarketplace(item) &&
            hasEnoughKnownCoinsFor(item) &&
            !isAlreadyOwnedPurpleEstablishment(item) &&
            !isKnownBuiltLandmark(item)

    private fun GameScreenState.hasEnoughKnownCoinsFor(item: ShopItem): Boolean {
        val activePlayerCoins = players.firstOrNull { it.isActivePlayer }?.coins
        return activePlayerCoins == null || activePlayerCoins >= item.cost
    }

    private fun GameScreenState.isKnownBuiltLandmark(item: ShopItem): Boolean {
        if (item.purchaseType != PurchaseType.LANDMARK) return false
        val activePlayerId = players.firstOrNull { it.isActivePlayer }?.id?.toIntOrNull() ?: return false
        val landmarkType = runCatching { LandmarkType.valueOf(item.type) }.getOrNull() ?: return false
        return playerLandmarks[activePlayerId].orEmpty().any {
            it.landmarkType == landmarkType && it.isBuilt
        }
    }

    private fun GameScreenState.applyPurchaseEvent(event: PurchaseEvent): GameScreenState =
        when (event) {
            is PurchaseEvent.Success -> {
                // The active buyer must still match its local pending action. Other players have
                // no pending purchase, but should display the server's broadcast success feedback.
                val matchesPending = pendingPurchaseItemType == event.itemType
                if (isActivePlayer && !matchesPending) {
                    this
                } else {
                    copy(
                        purchaseState = PurchaseState.SUCCESS,
                        selectedPurchaseItemType = null,
                        pendingPurchaseItemType = null,
                        purchaseFeedbackItemType = event.itemType,
                        purchaseMessage = "${event.itemType.toDisplayName()} bought"
                    )
                }
            }
            is PurchaseEvent.Failure -> {
                // Failed purchases are retryable; the backend stays authoritative for the reason.
                if (purchaseState != PurchaseState.PENDING) {
                    this
                } else {
                    copy(
                        purchaseState = PurchaseState.ERROR,
                        pendingPurchaseItemType = null,
                        purchaseFeedbackItemType = purchaseFeedbackItemType,
                        purchaseMessage = event.message.ifBlank { "Purchase failed" }
                    )
                }
            }
        }

    private fun GameScreenState.resetPurchaseFeedbackIf(shouldReset: Boolean): GameScreenState =
        if (!shouldReset) {
            this
        } else {
            copy(
                purchaseState = PurchaseState.IDLE,
                pendingPurchaseItemType = null,
                selectedPurchaseItemType = null,
                purchaseFeedbackItemType = null,
                purchaseMessage = null
            )
        }

    /**
     * Debug-only (#191): force-ends the current game with the local player as
     * winner via the admin debug endpoint. On success the server's GAME_END
     * broadcast drives the winner screen, so there is nothing else to do here;
     * failures surface through [debugEndGameErrors] as a snackbar.
     */
    // Guards against rapid double-taps queuing concurrent end-game calls (#191 review).
    private var endGameInFlight = false

    fun endGame() {
        val gameId = mutableState.value.gameId ?: return
        if (endGameInFlight) return
        endGameInFlight = true
        viewModelScope.launch {
            try {
                val response = debugApi.endGame(EndGameRequest(gameId))
                if (!response.isSuccessful) {
                    mutableDebugEndGameErrors.tryEmit("End game failed (${response.code()})")
                }
            } catch (e: Exception) {
                mutableDebugEndGameErrors.tryEmit("End game error: ${e.toClientError("unknown error").toUserMessage()}")
            } finally {
                endGameInFlight = false
            }
        }
    }

    private fun String.toDisplayName(): String =
        lowercase()
            .split("_")
            .joinToString(" ") { part -> part.replaceFirstChar { it.titlecase() } }

    companion object {
        /**
         * #302: how long the client dwells on RESOLVE_EFFECTS before auto-sending
         * resolveEffects — long enough to read the dice result and income, then
         * advance. This is the same value the RESOLVE_EFFECTS phase timer counts
         * down (see [RESOLVE_EFFECTS_TIMER_SECONDS]), so the visible countdown and
         * the actual phase length stay in lockstep. Players without a Radio Tower
         * have nothing to decide here, so it is kept short rather than the old 20s.
         */
        const val DEFAULT_RESOLVE_EFFECTS_DWELL_MS = 6_000L

        /** RESOLVE_EFFECTS phase-timer length, in seconds, derived from the dwell. */
        const val RESOLVE_EFFECTS_TIMER_SECONDS = (DEFAULT_RESOLVE_EFFECTS_DWELL_MS / 1000L).toInt()
    }

    class Factory(
        private val webSocketClient: WebSocketClient,
        private val sessionStateHolder: SessionStateHolder,
        private val debugApi: DebugApi,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(GameScreenViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return GameScreenViewModel(webSocketClient, sessionStateHolder, debugApi) as T
        }
    }
}
