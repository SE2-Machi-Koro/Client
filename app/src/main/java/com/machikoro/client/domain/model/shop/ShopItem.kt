package com.machikoro.client.domain.model.shop

import com.machikoro.client.domain.enums.CardType
import com.machikoro.client.domain.enums.LandmarkType
import com.machikoro.client.domain.enums.PurchaseType
import com.machikoro.client.domain.enums.ShopItemColor

/**
 * UI-ready shop item used by the buying phase screen.
 *
 * `type` must stay equal to the server CardType/LandmarkType enum name because
 * the ViewModel sends it back as `cardType` or `landmarkType` during purchase.
 */
data class ShopItem(
    val purchaseType: PurchaseType,
    val type: String,
    val displayName: String,
    val cost: Int,
    val color: ShopItemColor,
    val establishmentType: String,
    val activationNumbers: List<Int>,
    val effectText: String,
    val imageKey: String,
    val isAvailable: Boolean = true
) {
    val activationText: String
        get() = activationNumbers.toActivationText() ?: "Permanent"
}

data class CardDefinition(
    val cardType: CardType,
    val displayName: String,
    val cost: Int,
    val color: ShopItemColor,
    val establishmentType: String,
    val activationNumbers: List<Int>,
    val effectText: String,
    val imageKey: String,
) {
    val activationText: String
        get() = activationNumbers.toActivationText().orEmpty()

    fun toShopItem(isAvailable: Boolean = true): ShopItem =
        ShopItem(
            purchaseType = PurchaseType.ESTABLISHMENT,
            type = cardType.name,
            displayName = displayName,
            cost = cost,
            color = color,
            establishmentType = establishmentType,
            activationNumbers = activationNumbers,
            effectText = effectText,
            imageKey = imageKey,
            isAvailable = isAvailable,
        )
}

object CardDefinitions {
    private const val ONE_COIN_ON_ANY_TURN = "Get 1 coin from the bank on anyone's turn."

    val defaultCards = listOf(
        CardDefinition(
            cardType = CardType.WHEAT_FIELD,
            displayName = "Wheat Field",
            cost = 1,
            color = ShopItemColor.BLUE,
            establishmentType = "WHEAT",
            activationNumbers = listOf(1),
            effectText = ONE_COIN_ON_ANY_TURN,
            imageKey = "card_wheat_field"
        ),
        CardDefinition(
            cardType = CardType.RANCH,
            displayName = "Ranch",
            cost = 1,
            color = ShopItemColor.BLUE,
            establishmentType = "COW",
            activationNumbers = listOf(2),
            effectText = ONE_COIN_ON_ANY_TURN,
            imageKey = "card_ranch"
        ),
        CardDefinition(
            cardType = CardType.FOREST,
            displayName = "Forest",
            cost = 3,
            color = ShopItemColor.BLUE,
            establishmentType = "GEAR",
            activationNumbers = listOf(5),
            effectText = ONE_COIN_ON_ANY_TURN,
            imageKey = "card_forest"
        ),
        CardDefinition(
            cardType = CardType.MINE,
            displayName = "Mine",
            cost = 6,
            color = ShopItemColor.BLUE,
            establishmentType = "GEAR",
            activationNumbers = listOf(9),
            effectText = "Get 5 coins from the bank on anyone's turn.",
            imageKey = "card_mine"
        ),
        CardDefinition(
            cardType = CardType.APPLE_ORCHARD,
            displayName = "Apple Orchard",
            cost = 3,
            color = ShopItemColor.BLUE,
            establishmentType = "WHEAT",
            activationNumbers = listOf(10),
            effectText = "Get 3 coins from the bank on anyone's turn.",
            imageKey = "card_apple_orchard"
        ),
        CardDefinition(
            cardType = CardType.BAKERY,
            displayName = "Bakery",
            cost = 1,
            color = ShopItemColor.GREEN,
            establishmentType = "BREAD",
            activationNumbers = listOf(2, 3),
            effectText = "Get 1 coin from the bank on your turn.",
            imageKey = "card_bakery"
        ),
        CardDefinition(
            cardType = CardType.CONVENIENCE_STORE,
            displayName = "Convenience Store",
            cost = 2,
            color = ShopItemColor.GREEN,
            establishmentType = "BREAD",
            activationNumbers = listOf(4),
            effectText = "Get 3 coins from the bank on your turn.",
            imageKey = "card_convenience_store"
        ),
        CardDefinition(
            cardType = CardType.CHEESE_FACTORY,
            displayName = "Cheese Factory",
            cost = 5,
            color = ShopItemColor.GREEN,
            establishmentType = "FACTORY",
            activationNumbers = listOf(7),
            effectText = "Get 3 coins from the bank for each cow establishment you own.",
            imageKey = "card_cheese_factory"
        ),
        CardDefinition(
            cardType = CardType.FURNITURE_FACTORY,
            displayName = "Furniture Factory",
            cost = 3,
            color = ShopItemColor.GREEN,
            establishmentType = "FACTORY",
            activationNumbers = listOf(8),
            effectText = "Get 3 coins from the bank for each gear establishment you own.",
            imageKey = "card_furniture_factory"
        ),
        CardDefinition(
            cardType = CardType.FRUIT_AND_VEGETABLE_MARKET,
            displayName = "Fruit and Vegetable Market",
            cost = 2,
            color = ShopItemColor.GREEN,
            establishmentType = "FRUIT",
            activationNumbers = listOf(11, 12),
            effectText = "Get 2 coins from the bank for each wheat establishment you own.",
            imageKey = "card_fruit_and_vegetable_market"
        ),
        CardDefinition(
            cardType = CardType.CAFE,
            displayName = "Cafe",
            cost = 2,
            color = ShopItemColor.RED,
            establishmentType = "CUP",
            activationNumbers = listOf(3),
            effectText = "Take 1 coin from the player who rolled the dice.",
            imageKey = "card_cafe"
        ),
        CardDefinition(
            cardType = CardType.FAMILY_RESTAURANT,
            displayName = "Family Restaurant",
            cost = 3,
            color = ShopItemColor.RED,
            establishmentType = "CUP",
            activationNumbers = listOf(9, 10),
            effectText = "Take 2 coins from the player who rolled the dice.",
            imageKey = "card_family_restaurant"
        ),
        CardDefinition(
            cardType = CardType.STADIUM,
            displayName = "Stadium",
            cost = 6,
            color = ShopItemColor.PURPLE,
            establishmentType = "MAJOR",
            activationNumbers = listOf(6),
            effectText = "Take 2 coins from every opponent on your turn.",
            imageKey = "card_stadium"
        ),
        CardDefinition(
            cardType = CardType.TV_STATION,
            displayName = "TV Station",
            cost = 7,
            color = ShopItemColor.PURPLE,
            establishmentType = "MAJOR",
            activationNumbers = listOf(6),
            effectText = "Take 5 coins from one opponent on your turn.",
            imageKey = "card_tv_station"
        ),
        CardDefinition(
            cardType = CardType.BUSINESS_CENTER,
            displayName = "Business Center",
            cost = 8,
            color = ShopItemColor.PURPLE,
            establishmentType = "MAJOR",
            activationNumbers = listOf(6),
            effectText = "Exchange one non-major establishment with an opponent.",
            imageKey = "card_business_center"
        ),
    )

    private val byType = defaultCards.associateBy { it.cardType }

    fun forType(cardType: CardType): CardDefinition? = byType[cardType]

    fun sortShopItemsByActivation(items: List<ShopItem>): List<ShopItem> =
        items.sortedWith(
            compareBy<ShopItem> { it.activationSortStart }
                .thenBy { it.activationSortEnd }
                .thenBy { it.cost }
                .thenBy { it.displayName },
        )

    fun sortCardTypesByActivation(types: Iterable<CardType>): List<CardType> =
        types.sortedWith(
            compareBy<CardType> { forType(it)?.activationNumbers?.firstOrNull() ?: Int.MAX_VALUE }
                .thenBy { forType(it)?.activationNumbers?.lastOrNull() ?: Int.MAX_VALUE }
                .thenBy { forType(it)?.cost ?: Int.MAX_VALUE }
                .thenBy { forType(it)?.displayName ?: it.name },
        )
}

object ShopCatalog {
    /** Local fallback catalog used until a snapshot supplies server definitions. */
    val defaultItems = listOf(
        CardDefinitions.defaultCards.map { it.toShopItem() },
        listOf(
            ShopItem(
                purchaseType = PurchaseType.LANDMARK,
                type = LandmarkType.TRAIN_STATION.name,
                displayName = "Train Station",
                cost = 4,
                color = ShopItemColor.LANDMARK,
                establishmentType = "LANDMARK",
                activationNumbers = emptyList(),
                effectText = "You may roll one or two dice.",
                imageKey = "landmark_train_station"
            ),
            ShopItem(
                purchaseType = PurchaseType.LANDMARK,
                type = LandmarkType.SHOPPING_MALL.name,
                displayName = "Shopping Mall",
                cost = 10,
                color = ShopItemColor.LANDMARK,
                establishmentType = "LANDMARK",
                activationNumbers = emptyList(),
                effectText = "Your cup and bread establishments earn 1 extra coin.",
                imageKey = "landmark_shopping_mall"
            ),
            ShopItem(
                purchaseType = PurchaseType.LANDMARK,
                type = LandmarkType.AMUSEMENT_PARK.name,
                displayName = "Amusement Park",
                cost = 16,
                color = ShopItemColor.LANDMARK,
                establishmentType = "LANDMARK",
                activationNumbers = emptyList(),
                effectText = "If you roll doubles, take another turn after this one.",
                imageKey = "landmark_amusement_park"
            ),
            ShopItem(
                purchaseType = PurchaseType.LANDMARK,
                type = LandmarkType.RADIO_TOWER.name,
                displayName = "Radio Tower",
                cost = 22,
                color = ShopItemColor.LANDMARK,
                establishmentType = "LANDMARK",
                activationNumbers = emptyList(),
                effectText = "Once every turn, you can reroll your dice.",
                imageKey = "landmark_radio_tower"
            )
        )
    ).flatten()
}

private val ShopItem.activationSortStart: Int
    get() = activationNumbers.firstOrNull() ?: Int.MAX_VALUE

private val ShopItem.activationSortEnd: Int
    get() = activationNumbers.lastOrNull() ?: Int.MAX_VALUE

fun List<Int>.toActivationText(): String? {
    val numbers = distinct().sorted()
    if (numbers.isEmpty()) return null
    return if (numbers.size == 1) {
        numbers.first().toString()
    } else {
        "${numbers.first()}-${numbers.last()}"
    }
}

fun String.toActivationNumbers(): List<Int> {
    val trimmed = trim()
    if (trimmed.isBlank() || trimmed.equals("Permanent", ignoreCase = true)) return emptyList()
    val rangeParts = trimmed.split("-", limit = 2).map { it.trim().toIntOrNull() }
    if (rangeParts.size == 2 && rangeParts.all { it != null }) {
        val start = rangeParts[0] ?: return emptyList()
        val end = rangeParts[1] ?: return emptyList()
        return (minOf(start, end)..maxOf(start, end)).toList()
    }
    return Regex("\\d+").findAll(trimmed).map { it.value.toInt() }.toList().distinct().sorted()
}
