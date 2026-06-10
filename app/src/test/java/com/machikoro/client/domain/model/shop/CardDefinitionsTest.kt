package com.machikoro.client.domain.model.shop

import com.machikoro.client.domain.enums.CardType
import org.junit.Assert.assertEquals
import org.junit.Test

class CardDefinitionsTest {
    @Test
    fun activationTextIsDerivedFromNumericActivationNumbers() {
        val bakery = CardDefinitions.forType(CardType.BAKERY)

        assertEquals(listOf(2, 3), bakery?.activationNumbers)
        assertEquals("2-3", bakery?.activationText)
    }

    @Test
    fun sortShopItemsByActivationUsesNumericActivationOrder() {
        val items = listOf(
            CardDefinitions.forType(CardType.FRUIT_AND_VEGETABLE_MARKET)!!.toShopItem(),
            CardDefinitions.forType(CardType.CAFE)!!.toShopItem(),
            CardDefinitions.forType(CardType.BAKERY)!!.toShopItem(),
            CardDefinitions.forType(CardType.WHEAT_FIELD)!!.toShopItem(),
        )

        assertEquals(
            listOf(
                CardType.WHEAT_FIELD.name,
                CardType.BAKERY.name,
                CardType.CAFE.name,
                CardType.FRUIT_AND_VEGETABLE_MARKET.name,
            ),
            CardDefinitions.sortShopItemsByActivation(items).map { it.type },
        )
    }

    @Test
    fun sortCardTypesByActivationUsesNumericActivationOrder() {
        assertEquals(
            listOf(
                CardType.WHEAT_FIELD,
                CardType.BAKERY,
                CardType.CAFE,
                CardType.FRUIT_AND_VEGETABLE_MARKET,
            ),
            CardDefinitions.sortCardTypesByActivation(
                listOf(
                    CardType.FRUIT_AND_VEGETABLE_MARKET,
                    CardType.CAFE,
                    CardType.BAKERY,
                    CardType.WHEAT_FIELD,
                )
            ),
        )
    }

    @Test
    fun activationNumbersCanBeParsedFromLegacyDisplayText() {
        assertEquals(listOf(9, 10), "9-10".toActivationNumbers())
        assertEquals(listOf(11, 12), "11, 12".toActivationNumbers())
        assertEquals(emptyList<Int>(), "Permanent".toActivationNumbers())
    }
}
