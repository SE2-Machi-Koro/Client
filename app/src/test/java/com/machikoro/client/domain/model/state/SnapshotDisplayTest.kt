package com.machikoro.client.domain.model.state

import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.LandmarkType
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotDisplayTest {

    @Test
    fun landmarkTypeDisplayTextIsTitleCased() {
        assertEquals("Train Station", LandmarkType.TRAIN_STATION.toDisplayText())
        assertEquals("Radio Tower", LandmarkType.RADIO_TOWER.toDisplayText())
    }

    @Test
    fun cardTypeDisplayTextIsTitleCased() {
        assertEquals("Wheat Field", CardType.WHEAT_FIELD.toDisplayText())
        assertEquals(
            "Fruit And Vegetable Market",
            CardType.FRUIT_AND_VEGETABLE_MARKET.toDisplayText(),
        )
    }

    @Test
    fun singleWordEnumDisplayTextIsCapitalized() {
        assertEquals("Forest", CardType.FOREST.toDisplayText())
    }
}
