package com.machikoro.client.ui.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.machikoro.client.R
import com.machikoro.client.domain.enums.GamePhase
import com.machikoro.client.domain.enums.GameStatus
import com.machikoro.client.domain.model.state.GameScreenState
import com.machikoro.client.ui.game.GameSound
import com.machikoro.client.ui.game.SoundManager
import com.machikoro.client.ui.shared.ActionButton
import com.machikoro.client.ui.shared.BasicText
import com.machikoro.client.ui.shared.SecondaryActionButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val DICE_ANIMATION_INTERVAL_MS = 100L // faster change
// One brief roll animation for everyone, so the reveal isn't held behind a long fixed
// delay and active/non-active players stay in lockstep (matters during a Radio Tower
// reroll where both spin off the same roll tick).
private const val DICE_ANIMATION_DURATION_MS = 2000L
private val DICE_SIZE = 58.dp
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
    if(diceCount == 1) {
        Box(
            modifier =   Modifier.clickable(
                enabled = true,
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                role = Role.Button)
        ) {
            Image(
                painter = painterResource(id = R.drawable.game_dice_perspective),
                contentDescription = "Select $diceCount dice",
                modifier = modifier
                    .size(95.dp)
                    .offset(y = 5.dp)
                    .alpha(if (isSelected) 1f else 0.5f)
            )
        }
    } else {
        Row(
            modifier = Modifier.clickable(
                enabled = true,
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource()},
                role = Role.Button)
                .wrapContentSize(),
            horizontalArrangement = Arrangement.spacedBy((-30).dp) // makes dices closer to each other

        ) {
            Image(
                painter = painterResource(id = R.drawable.game_dice_perspective_1),
                contentDescription = "Select $diceCount dice",
                modifier = modifier
                    .size(95.dp) // dice size
                    .alpha(if (isSelected) 1f else 0.5f),
            )
            Image(
                painter = painterResource(id = R.drawable.game_dice_perspective_2),
                contentDescription = "Select $diceCount dice",
                modifier = modifier
                    .size(95.dp)
                    .offset(y = (-45).dp) // second dice is higher
                    .alpha(if (isSelected) 1f else 0.5f),
            )
        }
    }
}

@Composable
fun DiceResultDisplay(
    dice: List<Int>,
    diceSize: Dp = DICE_SIZE,
    modifier: Modifier = Modifier
) {
    val sum = dice.sum()
    // Doubles (two equal dice) matter in Machi Koro — the Amusement Park grants an
    // extra turn on doubles — so call them out instead of leaving players to notice.
    val isDoubles = dice.size == 2 && dice.distinct().size == 1
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        if (isDoubles) {
            Text(
                text = "Doubles!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Yellow,
                modifier = Modifier.semantics {
                    contentDescription = "Doubles"
                }
            )
        }

        if (dice.size == 1) {
            // One die: image and number in one row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = diceDrawableFor(dice.first())),
                    contentDescription = "Dice showing ${dice.first()}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(diceSize)
                )

                Text(
                    text = "$sum",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        } else {
            // Two dice: dice in one row, number underneath
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dice.forEach { value ->
                        Image(
                            painter = painterResource(id = diceDrawableFor(value)),
                            contentDescription = "Dice showing $value",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(diceSize)
                        )
                    }
                }

                Text(
                    text = "$sum",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }


    }
}

@Composable
fun DiceSection(
    state: GameScreenState,
    onRollDice: (diceCount: Int) -> Unit,
    onReroll: (diceCount: Int) -> Unit,
    onSkipReroll: () -> Unit = {},
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
    // Last roll tick we already animated. Seeded with the current tick so entering
    // mid-turn does not replay a roll that already happened (#346).
    var lastAnimatedTick by remember { mutableStateOf(state.diceRollTick) }

    // Active player uses frozen/selected count; snapshot only stores total so diceResult.size is unreliable after reconnect.
    // Non-active player uses the count captured from the live ROLL_DICE message (before snapshot overwrites it).
    val animationDiceCount = if (state.isActivePlayer) {
        // Without a Train Station the player can only roll one die, so never spin
        // two even if a stale requested count lingers from an earlier turn (#399).
        if (state.hasTrainStation) frozenDiceCount ?: selectedDiceCount ?: state.requestedDiceCount else 1
    } else {
        localAnimationDiceCount
    }
    val showRollingAnimation = isAnimating || (state.isActivePlayer && state.isRolling)

    LaunchedEffect(state.diceResult) {
        if (state.diceResult == null) {
            // New turn — reset per-turn local state.
            selectedDiceCount = null
            frozenDiceCount = null
            hasRerolled = false
        }
    }

    // #346: animate non-active players on every genuine roll AND reroll. The tick
    // is bumped only by real DICE_ROLLED/DICE_REROLLED frames, so a same-turn
    // Radio Tower reroll replays the animation, while the [x, y] -> [total]
    // snapshot collapse (which does not bump the tick) does not.
    LaunchedEffect(state.diceRollTick) {
        if (!state.isActivePlayer &&
            state.diceResult != null &&
            state.diceRollTick > lastAnimatedTick
        ) {
            localAnimationDiceCount = state.diceResult.size
            isAnimating = true
        }
        lastAnimatedTick = state.diceRollTick
    }

    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            // Same brief spin for active and non-active players before the result shows.
            delay(DICE_ANIMATION_DURATION_MS)
            isAnimating = false
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(bottom = 32.dp)
    ) {
        when {
            showRollingAnimation -> {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(animationDiceCount) {
                        DiceAnimationDisplay(animating = true)
                    }
                }
            }
            state.diceResult != null -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                )
                {
                    if(!state.isActivePlayer) {
                        BasicText(
                             state.activePlayerUsername + " has rolled:",
                        )
                    }
                    DiceResultDisplay(dice = state.diceResult)
                }

            }
            else -> {}
        }
        if(!state.isActivePlayer && state.diceResult == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = (-30).dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.game_dice_perspective),
                    contentDescription = "One dice",
                    modifier = Modifier
                        .size(100.dp)
                )
                BasicText(
                   state.activePlayerUsername + " is rolling dice",
                )
            }

        }
        if (state.isActivePlayer &&
            state.gameStatus == GameStatus.IN_PROGRESS &&
            !showRollingAnimation
        ) {
            // Roll controls only appear during the actual rolling phase
            if (state.gamePhase == GamePhase.ROLL_DICE && state.hasTrainStation) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.wrapContentSize()) {
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
            } else if (
                state.gamePhase == GamePhase.ROLL_DICE &&
                state.diceResult == null
            ) {
                Image(
                    painter = painterResource(id = R.drawable.game_dice_perspective),
                    contentDescription = "One dice",
                    modifier = Modifier
                        .size(100.dp)
                )
            }


            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Roll button: only when server has no result yet and we are in ROLL_DICE
                if (state.gamePhase == GamePhase.ROLL_DICE && state.diceResult == null) {
                    val chosen =
                        if (state.hasTrainStation) frozenDiceCount ?: selectedDiceCount ?: state.requestedDiceCount else 1
                    ActionButton(
                        onClick = {
                            SoundManager.play(GameSound.DICE_ROLL)
                            frozenDiceCount = chosen
                            isAnimating = true
                            onRollDice(chosen)
                        },
                        label = "Roll " + (if (chosen > 1) "Two Dice" else "One Die"),
                        modifier = Modifier.semantics {
                            contentDescription = "Roll Dice"
                        }
                            .width(180.dp)
                    )
                }

                // Reroll button: canReroll already gates this to RESOLVE_EFFECTS + Radio Tower + result exists
                if (state.canReroll && canReroll && !hasRerolled) {
                    val rerollCount = frozenDiceCount ?: state.diceResult?.size ?: 1
                    ActionButton(
                        onClick = {
                            SoundManager.play(GameSound.DICE_ROLL)
                            hasRerolled = true
                            frozenDiceCount = rerollCount
                            isAnimating = true
                            onReroll(rerollCount)
                        },
                        label = "Roll Dice Again",
                        modifier = Modifier.semantics {
                            contentDescription = "Reroll Dice"
                        }
                    )
                    SecondaryActionButton(
                        onClick = onSkipReroll,
                        enabled = true,
                        label = "Skip",
                        modifier = Modifier.semantics {
                            contentDescription = "Skip reroll"
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
