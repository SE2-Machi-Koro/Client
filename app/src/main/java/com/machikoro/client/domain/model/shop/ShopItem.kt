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
    val imageKey: String,
    val isAvailable: Boolean = true
)

object ShopCatalog {
    /**
     * Temporary local catalog for issue #38.
     *
     * The server already owns the real card/landmark database. Until the client
     * consumes server-provided shop definitions, this list gives the UI all
     * current server enum values so every purchase button can send a valid
     * request. Availability/error feedback stays with #39 and server #228.
     */
    val defaultItems = listOf(
        ShopItem(
            purchaseType = PurchaseType.ESTABLISHMENT,
            type = CardType.WHEAT_FIELD.name,
            displayName = "Wheat Field",
            cost = 1,
            color = ShopItemColor.BLUE,
            establishmentType = "WHEAT",
            imageKey = "card_wheat_field"
        ),
        ShopItem(
            purchaseType = PurchaseType.ESTABLISHMENT,
            type = CardType.RANCH.name,
            displayName = "Ranch",
            cost = 1,
            color = ShopItemColor.BLUE,
            establishmentType = "COW",
            imageKey = "card_ranch"
        ),
        ShopItem(
            purchaseType = PurchaseType.ESTABLISHMENT,
            type = CardType.FOREST.name,
            displayName = "Forest",
            cost = 3,
            color = ShopItemColor.BLUE,
            establishmentType = "GEAR",
            imageKey = "card_forest"
        ),
        ShopItem(
            purchaseType = PurchaseType.ESTABLISHMENT,
            type = CardType.MINE.name,
            displayName = "Mine",
            cost = 6,
            color = ShopItemColor.BLUE,
            establishmentType = "GEAR",
            imageKey = "card_mine"
        ),
        ShopItem(
            purchaseType = PurchaseType.ESTABLISHMENT,
            type = CardType.APPLE_ORCHARD.name,
            displayName = "Apple Orchard",
            cost = 3,
            color = ShopItemColor.BLUE,
            establishmentType = "WHEAT",
            imageKey = "card_apple_orchard"
        ),
        ShopItem(
            purchaseType = PurchaseType.ESTABLISHMENT,
            type = CardType.BAKERY.name,
            displayName = "Bakery",
            cost = 1,
            color = ShopItemColor.GREEN,
            establishmentType = "BREAD",
            imageKey = "card_bakery"
        ),
        ShopItem(
            purchaseType = PurchaseType.ESTABLISHMENT,
            type = CardType.CONVENIENCE_STORE.name,
            displayName = "Convenience Store",
            cost = 2,
            color = ShopItemColor.GREEN,
            establishmentType = "BREAD",
            imageKey = "card_convenience_store"
        ),
        ShopItem(
            purchaseType = PurchaseType.ESTABLISHMENT,
            type = CardType.CHEESE_FACTORY.name,
            displayName = "Cheese Factory",
            cost = 5,
            color = ShopItemColor.GREEN,
            establishmentType = "FACTORY",
            imageKey = "card_cheese_factory"
        ),
        ShopItem(
            purchaseType = PurchaseType.ESTABLISHMENT,
            type = CardType.FURNITURE_FACTORY.name,
            displayName = "Furniture Factory",
            cost = 3,
            color = ShopItemColor.GREEN,
            establishmentType = "FACTORY",
            imageKey = "card_furniture_factory"
        ),
        ShopItem(
            purchaseType = PurchaseType.ESTABLISHMENT,
            type = CardType.FRUIT_AND_VEGETABLE_MARKET.name,
            displayName = "Fruit and Vegetable Market",
            cost = 2,
            color = ShopItemColor.GREEN,
            establishmentType = "FRUIT",
            imageKey = "card_fruit_and_vegetable_market"
        ),
        ShopItem(
            purchaseType = PurchaseType.ESTABLISHMENT,
            type = CardType.CAFE.name,
            displayName = "Cafe",
            cost = 2,
            color = ShopItemColor.RED,
            establishmentType = "CUP",
            imageKey = "card_cafe"
        ),
        ShopItem(
            purchaseType = PurchaseType.ESTABLISHMENT,
            type = CardType.FAMILY_RESTAURANT.name,
            displayName = "Family Restaurant",
            cost = 3,
            color = ShopItemColor.RED,
            establishmentType = "CUP",
            imageKey = "card_family_restaurant"
        ),
        ShopItem(
            purchaseType = PurchaseType.ESTABLISHMENT,
            type = CardType.STADIUM.name,
            displayName = "Stadium",
            cost = 6,
            color = ShopItemColor.PURPLE,
            establishmentType = "MAJOR",
            imageKey = "card_stadium"
        ),
        ShopItem(
            purchaseType = PurchaseType.ESTABLISHMENT,
            type = CardType.TV_STATION.name,
            displayName = "TV Station",
            cost = 7,
            color = ShopItemColor.PURPLE,
            establishmentType = "MAJOR",
            imageKey = "card_tv_station"
        ),
        ShopItem(
            purchaseType = PurchaseType.ESTABLISHMENT,
            type = CardType.BUSINESS_CENTER.name,
            displayName = "Business Center",
            cost = 8,
            color = ShopItemColor.PURPLE,
            establishmentType = "MAJOR",
            imageKey = "card_business_center"
        ),
        ShopItem(
            purchaseType = PurchaseType.LANDMARK,
            type = LandmarkType.TRAIN_STATION.name,
            displayName = "Train Station",
            cost = 4,
            color = ShopItemColor.LANDMARK,
            establishmentType = "LANDMARK",
            imageKey = "landmark_train_station"
        ),
        ShopItem(
            purchaseType = PurchaseType.LANDMARK,
            type = LandmarkType.SHOPPING_MALL.name,
            displayName = "Shopping Mall",
            cost = 10,
            color = ShopItemColor.LANDMARK,
            establishmentType = "LANDMARK",
            imageKey = "landmark_shopping_mall"
        ),
        ShopItem(
            purchaseType = PurchaseType.LANDMARK,
            type = LandmarkType.AMUSEMENT_PARK.name,
            displayName = "Amusement Park",
            cost = 16,
            color = ShopItemColor.LANDMARK,
            establishmentType = "LANDMARK",
            imageKey = "landmark_amusement_park"
        ),
        ShopItem(
            purchaseType = PurchaseType.LANDMARK,
            type = LandmarkType.RADIO_TOWER.name,
            displayName = "Radio Tower",
            cost = 22,
            color = ShopItemColor.LANDMARK,
            establishmentType = "LANDMARK",
            imageKey = "landmark_radio_tower"
        )
    )
}
