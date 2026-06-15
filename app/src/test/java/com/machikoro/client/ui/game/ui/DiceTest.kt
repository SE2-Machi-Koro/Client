package com.machikoro.client.ui.game.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [shouldAnimateNonActiveRoll], the decision behind the non-active
 * player's dice animation. Guards the Radio Tower reroll case (#346): a genuine
 * reroll must re-animate within the same turn, while the authoritative snapshot
 * that collapses the live per-die result into its total must not.
 */
class DiceTest {

    @Test
    fun firstRollAnimatesWhenNothingHasAnimatedYet() {
        assertTrue(shouldAnimateNonActiveRoll(newResult = listOf(3, 4), lastAnimatedResult = null))
    }

    @Test
    fun snapshotCollapseOfTwoDiceIntoTotalDoesNotReAnimate() {
        // Live roll [3, 4] already animated; snapshot overwrites it with the total [7].
        assertFalse(shouldAnimateNonActiveRoll(newResult = listOf(7), lastAnimatedResult = listOf(3, 4)))
    }

    @Test
    fun rerollWithinSameTurnReAnimatesAfterCollapse() {
        // lastAnimatedResult stays [3, 4] across the collapse; a fresh reroll [2, 5] must animate.
        assertTrue(shouldAnimateNonActiveRoll(newResult = listOf(2, 5), lastAnimatedResult = listOf(3, 4)))
    }

    @Test
    fun rerollWithSameTotalButDifferentDiceReAnimates() {
        assertTrue(shouldAnimateNonActiveRoll(newResult = listOf(5, 2), lastAnimatedResult = listOf(3, 4)))
    }

    @Test
    fun singleDieRerollReAnimates() {
        assertTrue(shouldAnimateNonActiveRoll(newResult = listOf(6), lastAnimatedResult = listOf(4)))
    }

    @Test
    fun identicalResultDoesNotReAnimate() {
        assertFalse(shouldAnimateNonActiveRoll(newResult = listOf(3, 4), lastAnimatedResult = listOf(3, 4)))
    }

    @Test
    fun nullResultDoesNotAnimate() {
        assertFalse(shouldAnimateNonActiveRoll(newResult = null, lastAnimatedResult = listOf(3, 4)))
    }

    @Test
    fun emptyResultDoesNotAnimate() {
        assertFalse(shouldAnimateNonActiveRoll(newResult = emptyList(), lastAnimatedResult = null))
    }

    @Test
    fun newSingleDieValueAfterAnotherSingleDieReAnimates() {
        // Both results are single-die, so this is a real roll, not a snapshot collapse.
        assertTrue(shouldAnimateNonActiveRoll(newResult = listOf(5), lastAnimatedResult = listOf(2)))
    }
}
