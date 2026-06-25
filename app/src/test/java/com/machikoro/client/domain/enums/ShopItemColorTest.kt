package com.machikoro.client.domain.enums

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopItemColorTest {
    @Test
    fun redTriggersForOpponentsOnlyWhenActivePlayerIsKnown() {
        assertTrue(ShopItemColor.RED.triggersForResolvingEffects(2, 1))
        assertFalse(ShopItemColor.RED.triggersForResolvingEffects(1, 1))
        assertFalse(ShopItemColor.RED.triggersForResolvingEffects(2, null))
    }

    @Test
    fun blueTriggersForEveryPlayer() {
        assertTrue(ShopItemColor.BLUE.triggersForResolvingEffects(1, 1))
        assertTrue(ShopItemColor.BLUE.triggersForResolvingEffects(2, 1))
        assertTrue(ShopItemColor.BLUE.triggersForResolvingEffects(2, null))
    }

    @Test
    fun greenAndPurpleTriggerOnlyForActivePlayer() {
        assertTrue(ShopItemColor.GREEN.triggersForResolvingEffects(1, 1))
        assertFalse(ShopItemColor.GREEN.triggersForResolvingEffects(2, 1))
        assertFalse(ShopItemColor.GREEN.triggersForResolvingEffects(1, null))

        assertTrue(ShopItemColor.PURPLE.triggersForResolvingEffects(1, 1))
        assertFalse(ShopItemColor.PURPLE.triggersForResolvingEffects(2, 1))
        assertFalse(ShopItemColor.PURPLE.triggersForResolvingEffects(1, null))
    }

    @Test
    fun landmarkNeverTriggersDuringResolvingEffects() {
        assertFalse(ShopItemColor.LANDMARK.triggersForResolvingEffects(1, 1))
        assertFalse(ShopItemColor.LANDMARK.triggersForResolvingEffects(2, 1))
        assertFalse(ShopItemColor.LANDMARK.triggersForResolvingEffects(1, null))
    }
}
