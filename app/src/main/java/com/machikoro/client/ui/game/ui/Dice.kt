package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.R
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.ui.shared.ActionButton
import com.machikoro.client.ui.shared.DecreasingLineTimer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val DICE_ANIMATION_INTERVAL_MS = 100L // faster change
private const val DICE_ANIMATION_DURATION_MS = 5000L // active player: full timer
private const val NON_ACTIVE_DICE_ANIMATION_MS = 2000L // non-active player: brief animation before reveal
private const val ROLL_TIMER_SECONDS = 5
private val DICE_FACES = listOf(
    R.drawable.game_dice_1,
    R.drawable.game_dice_2,
    R.drawable.game_dice_3,
    R.drawable.game_dice_4,
    R.drawable.game_dice_5,
    R.drawable.game_dice_6
)

fun diceDrawableFor(value: Int): Int =
    DICE_FACES.getOrElse(value - 1) { R.drawable.game_dice_perspective }

/**
 * Decides whether a newly received dice result should re-trigger the non-active player's
 * rolling animation. A genuine roll or Radio Tower reroll always animates, but the
 * authoritative snapshot that collapses the live per-die result `[a, b]` into its total
 * `[a + b]` must not — otherwise the animation replays on every snapshot (#346).
 *
 * @param newResult the dice result that just arrived.
 * @param lastAnimatedResult the result the animation last played for this turn, or null.
 */
internal fun shouldAnimateNonActiveRoll(
    newResult: List<Int>?,
    lastAnimatedResult: List<Int>?,
): Boolean {
    if (newResult.isNullOrEmpty()) return false
    if (newResult == lastAnimatedResult) return false
    val isSnapshotCollapse = newResult.size == 1 &&
        lastAnimatedResult != null &&
        lastAnimatedResult.size > 1 &&
        newResult.single() == lastAnimatedResult.sum()
    return !isSnapshotCollapse
}

@Composable
fun DiceAnimationDisplay(
    animating: Boolean,
    intervalMs: Long = DICE_ANIMATION_INTERVAL_MS,
    modifier: Modifier = Modifier
) {
    var currentFaceIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(animating) {
        if (animating) {
            // Keep changing faces while animating is true. This loop is cancelable when
            // the composable's LaunchedEffect key changes (animating -> false) or
            // when the composition leaves.
            while (animating && isActive) {
                currentFaceIndex = (0..5).random()
                delay(intervalMs)
            }
        }
    }

    Image(
        painter = painterResource(id = DICE_FACES[currentFaceIndex]),
        contentDescription = "Dice rolling",
        modifier = modifier.size(64.dp)
    )
}

@Composable
fun DiceCountSelector(
    diceCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = diceDrawableFor(diceCount)),
        contentDescription = "Select $diceCount dice",
        modifier = modifier
            .size(80.dp)
            .border(
                width = if (isSelected) 4.dp else 2.dp,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp)
            .clickable(
                enabled = true,
                onClick = onClick,
                role = Role.Button
            )
            .alpha(if (isSelected) 1f else 0.6f)
    )
}

@Composable
fun DiceResultDisplay(
    dice: List<Int>,
    modifier: Modifier = Modifier
) {
    val sum = dice.sum()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        dice.forEach { value ->
            Image(
                painter = painterResource(id = diceDrawableFor(value)),
                contentDescription = "Dice showing $value",
                modifier = Modifier.size(56.dp)
            )
        }
        Text(
            text = "$sum",
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun DiceSection(
    state: GameScreenState,
    onRollDice: (diceCount: Int) -> Unit,
    onReroll: (diceCount: Int) -> Unit,
    canReroll: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Show during rolling and the reroll window (Radio Tower)
    if (state.gamePhase != GamePhase.ROLL_DICE && state.gamePhase != GamePhase.RESOLVE_EFFECTS) return

    var isAnimating by remember { mutableStateOf(false) }
    // Key on both roundNumber + activePlayerId so state resets on every player's turn, not just per round.
    var selectedDiceCount by remember(state.roundNumber, state.activePlayerId) { mutableStateOf<Int?>(null) }
    var frozenDiceCount by remember(state.roundNumber, state.activePlayerId) { mutableStateOf<Int?>(null) }
    // Tracks whether the player already used the Radio Tower reroll this turn.
    var hasRerolled by remember(state.roundNumber, state.activePlayerId) { mutableStateOf(false) }
    // Captures dice count when non-active player animation is triggered (ROLL_DICE message has per-die values).
    var localAnimationDiceCount by remember(state.roundNumber, state.activePlayerId) { mutableIntStateOf(1) }
    // Last result the non-active animation played for. Lets a genuine reroll re-animate within the
    // same turn while ignoring the snapshot that collapses the live [x, y] result into [total] (#346).
    var lastAnimatedResult by remember(state.roundNumber, state.activePlayerId) { mutableStateOf<List<Int>?>(null) }

    // Active player uses frozen/selected count; snapshot only stores total so diceResult.size is unreliable after reconnect.
    // Non-active player uses the count captured from the live ROLL_DICE message (before snapshot overwrites it).
    val animationDiceCount = if (state.isActivePlayer) {
        frozenDiceCount ?: selectedDiceCount ?: state.requestedDiceCount
    } else {
        localAnimationDiceCount
    }

    LaunchedEffect(state.diceResult) {
        val result = state.diceResult
        if (result == null) {
            // New turn — reset all per-turn local state.
            selectedDiceCount = null
            frozenDiceCount = null
            hasRerolled = false
            lastAnimatedResult = null
            return@LaunchedEffect
        }
        if (!state.isActivePlayer && !isAnimating && shouldAnimateNonActiveRoll(result, lastAnimatedResult)) {
            // Non-active player: capture count from the live ROLL_DICE message (has per-die values)
            // and animate. Snapshot collapses [x, y] into [total]; that overwrite is ignored (#346).
            localAnimationDiceCount = result.size
            lastAnimatedResult = result
            isAnimating = true
        }
    }

    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            // Non-active players already have the result; show a shorter animation before revealing it.
            delay(if (state.isActivePlayer) DICE_ANIMATION_DURATION_MS else NON_ACTIVE_DICE_ANIMATION_MS)
            isAnimating = false
        }
    }

    // Stop animation for active player as soon as server confirms the result — avoids blocking
    // the result display behind the full fixed timer when server responds in <5 s.
    LaunchedEffect(state.isRolling) {
        if (!state.isRolling && state.isActivePlayer && isAnimating) {
            isAnimating = false
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(bottom = 32.dp)
    ) {
        when {
            isAnimating -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(animationDiceCount) {
                        DiceAnimationDisplay(animating = true)
                    }
                }
            }
            state.diceResult != null -> DiceResultDisplay(dice = state.diceResult)
            else -> {}
        }

        if (state.isActivePlayer &&
            state.gameStatus == GameStatus.IN_PROGRESS &&
            !isAnimating
        ) {
            // Roll controls only appear during the actual rolling phase
            if (state.gamePhase == GamePhase.ROLL_DICE) {
                if (state.hasTrainStation) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        listOf(1, 2).forEach { count ->
                            val isSelected = (selectedDiceCount ?: state.requestedDiceCount) == count
                            DiceCountSelector(
                                diceCount = count,
                                isSelected = isSelected,
                                onClick = {
                                    selectedDiceCount = count
                                    frozenDiceCount = count
                                }
                            )
                        }
                    }
                }
                DecreasingLineTimer(ROLL_TIMER_SECONDS)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Roll button: only when server has no result yet and we are in ROLL_DICE
                if (state.gamePhase == GamePhase.ROLL_DICE && state.diceResult == null) {
                    val chosen = frozenDiceCount ?: selectedDiceCount ?: state.requestedDiceCount
                    Image(
                        painter = painterResource(id = R.drawable.game_dice_perspective),
                        contentDescription = "Dice",
                        modifier = Modifier.size(48.dp)
                    )
                    ActionButton(
                        onClick = {
                            frozenDiceCount = chosen
                            isAnimating = true
                            onRollDice(chosen)
                        },
                        enabled = true,
                        label = "Roll Dice",
                        modifier = Modifier.semantics {
                            contentDescription = "Roll Dice"
                        }
                    )
                }

                // Reroll button: canReroll already gates this to RESOLVE_EFFECTS + Radio Tower + result exists
                if (state.canReroll && canReroll && !hasRerolled) {
                    val rerollCount = frozenDiceCount ?: state.diceResult?.size ?: 1
                    ActionButton(
                        onClick = {
                            hasRerolled = true
                            frozenDiceCount = rerollCount
                            isAnimating = true
                            onReroll(rerollCount)
                        },
                        enabled = true,
                        label = "Roll Dice Again",
                        modifier = Modifier.semantics {
                            contentDescription = "Reroll Dice"
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 400)
@Composable
private fun DiceAnimationPreview() {
    DiceAnimationDisplay(animating = true, intervalMs = 80)
}
