package com.charleshartmann.grocyfridge.data

import com.charleshartmann.grocyfridge.model.CreatedObjectResponse
import com.charleshartmann.grocyfridge.model.DetectedFoodItem
import com.charleshartmann.grocyfridge.model.GrocyLocation
import com.charleshartmann.grocyfridge.model.GrocyProduct
import com.charleshartmann.grocyfridge.model.GrocyQuantityUnit
import com.charleshartmann.grocyfridge.model.GrocyStockItem
import com.charleshartmann.grocyfridge.model.InventoryRequest
import com.charleshartmann.grocyfridge.model.ProposedChange
import com.charleshartmann.grocyfridge.model.StorageLocation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GrocyRepositoryTest {

    @Test
    fun buildsProposalsForExistingAndNewProducts() = runTest {
        val api = FakeGrocyApi(
            products = mutableListOf(GrocyProduct(id = 10, name = "Chips")),
            locations = mutableListOf(GrocyLocation(id = 2, name = "Fridge")),
            stock = mutableListOf(GrocyStockItem(productId = 10, amount = 1.0))
        )
        val repository = GrocyRepository(api)

        val proposals = repository.buildProposals(
            listOf(
                DetectedFoodItem("chips", 2.0, "bag", 0.8),
                DetectedFoodItem("milk", 1.0, "carton", 0.7)
            ),
            StorageLocation.Fridge
        )

        assertEquals(2, proposals.size)
        val chips = proposals.first { it.displayName == "Chips" }
        assertEquals(10L, chips.productId)
        assertEquals(1.0, chips.delta, 0.0)
        assertFalse(chips.isNewProduct)
        assertTrue(proposals.first { it.displayName == "Milk" }.isNewProduct)
    }

    @Test
    fun createsCupboardsLocationWhenMissing() = runTest {
        val api = FakeGrocyApi()
        val repository = GrocyRepository(api)

        val proposals = repository.buildProposals(
            listOf(DetectedFoodItem("rice", 1.0, "bag", 0.8)),
            StorageLocation.Cupboards
        )

        assertEquals("Cupboards", api.locations.last().name)
        assertEquals(api.locations.last().id, proposals.first().locationId)
    }

    @Test
    fun applyChangesCreatesProductAndInventoriesIt() = runTest {
        val api = FakeGrocyApi(locations = mutableListOf(GrocyLocation(id = 2, name = "Fridge")))
        val repository = GrocyRepository(api)
        val proposal = repository.buildProposals(
            listOf(DetectedFoodItem("beans", 3.0, "can", 0.8)),
            StorageLocation.Fridge
        )

        repository.applyChanges(proposal)

        assertEquals("Beans", api.products.last().name)
        assertEquals(3.0, api.inventoryRequests.last().second.newAmount, 0.0)
    }

    @Test
    fun mergesDuplicateDetections() = runTest {
        val api = FakeGrocyApi()
        val repository = GrocyRepository(api)

        val proposals = repository.buildProposals(
            listOf(
                DetectedFoodItem("milk", 1.0, "bottle", 0.8),
                DetectedFoodItem("milk", 2.0, "bottle", 0.7),
                DetectedFoodItem("MILK", 1.0, "bottle", 0.9)
            ),
            StorageLocation.Fridge
        )

        assertEquals(1, proposals.size)
        assertEquals(4.0, proposals.first().count, 0.0)
    }

    @Test
    fun filtersOutBlankNamesAndZeroCounts() = runTest {
        val api = FakeGrocyApi()
        val repository = GrocyRepository(api)

        val proposals = repository.buildProposals(
            listOf(
                DetectedFoodItem("chips", 2.0, "bag", 0.8),
                DetectedFoodItem("", 1.0, "piece", 0.5),
                DetectedFoodItem("  ", 3.0, "box", 0.6),
                DetectedFoodItem("soda", 0.0, "can", 0.7)
            ),
            StorageLocation.Fridge
        )

        assertEquals(1, proposals.size)
        assertEquals("Chips", proposals.first().displayName)
    }

    @Test
    fun matchesExistingProductCaseInsensitively() = runTest {
        val api = FakeGrocyApi(
            products = mutableListOf(GrocyProduct(id = 5, name = "Cheddar Cheese"))
        )
        val repository = GrocyRepository(api)

        val proposals = repository.buildProposals(
            listOf(DetectedFoodItem("cheddar cheese", 2.0, "piece", 0.9)),
            StorageLocation.Fridge
        )

        assertEquals(1, proposals.size)
        assertEquals("Cheddar Cheese", proposals.first().displayName)
        assertEquals(5L, proposals.first().productId)
        assertFalse(proposals.first().isNewProduct)
    }

    @Test
    fun matchesProductAfterRemovingTrailingS() = runTest {
        val api = FakeGrocyApi(
            products = mutableListOf(GrocyProduct(id = 7, name = "Egg"))
        )
        val repository = GrocyRepository(api)

        val proposals = repository.buildProposals(
            listOf(DetectedFoodItem("eggs", 6.0, "box", 0.9)),
            StorageLocation.Fridge
        )

        assertEquals(1, proposals.size)
        assertEquals("Egg", proposals.first().displayName)
        assertFalse(proposals.first().isNewProduct)
    }

    @Test
    fun usesPackUnitForBagContainer() = runTest {
        val api = FakeGrocyApi()
        val repository = GrocyRepository(api)

        val proposals = repository.buildProposals(
            listOf(
                DetectedFoodItem("chips", 1.0, "bag", 0.8),
                DetectedFoodItem("soda", 2.0, "can", 0.8)
            ),
            StorageLocation.Fridge
        )

        val chips = proposals.first { it.displayName == "Chips" }
        val soda = proposals.first { it.displayName == "Soda" }
        assertEquals(3L, chips.unitId)
        assertEquals(2L, soda.unitId)
    }

    @Test
    fun usesPackUnitForBoxContainer() = runTest {
        val api = FakeGrocyApi()
        val repository = GrocyRepository(api)

        val proposals = repository.buildProposals(
            listOf(DetectedFoodItem("cereal", 1.0, "box", 0.8)),
            StorageLocation.Fridge
        )

        assertEquals(3L, proposals.first().unitId)
    }

    @Test
    fun resultsAreSortedAlphabetically() = runTest {
        val api = FakeGrocyApi()
        val repository = GrocyRepository(api)

        val proposals = repository.buildProposals(
            listOf(
                DetectedFoodItem("yogurt", 1.0, "cup", 0.8),
                DetectedFoodItem("apples", 3.0, "bag", 0.7),
                DetectedFoodItem("milk", 2.0, "bottle", 0.9)
            ),
            StorageLocation.Fridge
        )

        assertEquals("Apples", proposals[0].displayName)
        assertEquals("Milk", proposals[1].displayName)
        assertEquals("Yogurt", proposals[2].displayName)
    }

    @Test
    fun currentAmountIsZeroForNewProduct() = runTest {
        val api = FakeGrocyApi()
        val repository = GrocyRepository(api)

        val proposals = repository.buildProposals(
            listOf(DetectedFoodItem("kombucha", 2.0, "bottle", 0.7)),
            StorageLocation.Fridge
        )

        assertEquals(0.0, proposals.first().currentAmount, 0.0)
        assertEquals(2.0, proposals.first().delta, 0.0)
    }

    @Test
    fun currentAmountReadFromStock() = runTest {
        val api = FakeGrocyApi(
            products = mutableListOf(GrocyProduct(id = 10, name = "Milk")),
            stock = mutableListOf(GrocyStockItem(productId = 10, amount = 3.0))
        )
        val repository = GrocyRepository(api)

        val proposals = repository.buildProposals(
            listOf(DetectedFoodItem("milk", 5.0, "bottle", 0.9)),
            StorageLocation.Fridge
        )

        assertEquals(3.0, proposals.first().currentAmount, 0.0)
        assertEquals(2.0, proposals.first().delta, 0.0)
    }

    @Test
    fun applyChangesOnlyIncludesEnabledChanges() = runTest {
        val api = FakeGrocyApi(
            locations = mutableListOf(GrocyLocation(id = 2, name = "Fridge"))
        )
        val repository = GrocyRepository(api)

        val proposals = repository.buildProposals(
            listOf(
                DetectedFoodItem("chips", 2.0, "bag", 0.8),
                DetectedFoodItem("milk", 1.0, "bottle", 0.7)
            ),
            StorageLocation.Fridge
        )
        val modified = proposals.toMutableList()
        modified[0] = modified[0].copy(included = false)

        repository.applyChanges(modified)

        assertEquals(1, api.inventoryRequests.size)
        assertEquals("Milk", api.products.first().name)
    }

    @Test
    fun reusesExistingLocation() = runTest {
        val api = FakeGrocyApi(
            locations = mutableListOf(GrocyLocation(id = 99, name = "Fridge"))
        )
        val repository = GrocyRepository(api)

        val proposals = repository.buildProposals(
            listOf(DetectedFoodItem("cheese", 1.0, "piece", 0.8)),
            StorageLocation.Fridge
        )

        assertEquals(99L, proposals.first().locationId)
        assertEquals(1, api.locations.size)
    }

    @Test
    fun negativeCountItemsAreFilteredOut() = runTest {
        val api = FakeGrocyApi()
        val repository = GrocyRepository(api)

        val proposals = repository.buildProposals(
            listOf(DetectedFoodItem("milk", -1.0, "bottle", 0.5)),
            StorageLocation.Fridge
        )

        assertTrue(proposals.isEmpty())
    }
}

private class FakeGrocyApi(
    val products: MutableList<GrocyProduct> = mutableListOf(),
    val locations: MutableList<GrocyLocation> = mutableListOf(GrocyLocation(id = 2, name = "Fridge")),
    val stock: MutableList<GrocyStockItem> = mutableListOf()
) : GrocyApi {
    val inventoryRequests = mutableListOf<Pair<Long, InventoryRequest>>()
    private var nextId = 100L

    override suspend fun systemInfo(): Map<String, Any?> = mapOf("grocy_version" to "test")
    override suspend fun products(): List<GrocyProduct> = products
    override suspend fun quantityUnits(): List<GrocyQuantityUnit> = listOf(
        GrocyQuantityUnit(id = 2, name = "Piece"),
        GrocyQuantityUnit(id = 3, name = "Pack")
    )

    override suspend fun locations(): List<GrocyLocation> = locations
    override suspend fun stock(): List<GrocyStockItem> = stock

    override suspend fun createProduct(product: GrocyProduct): CreatedObjectResponse {
        val id = nextId++
        products += product.copy(id = id)
        return CreatedObjectResponse(id)
    }

    override suspend fun createLocation(location: GrocyLocation): CreatedObjectResponse {
        val id = nextId++
        locations += location.copy(id = id)
        return CreatedObjectResponse(id)
    }

    override suspend fun inventoryProduct(productId: Long, request: InventoryRequest) {
        inventoryRequests += productId to request
    }
}
