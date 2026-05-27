package com.charleshartmann.grocyfridge.data

import com.charleshartmann.grocyfridge.model.CreatedObjectResponse
import com.charleshartmann.grocyfridge.model.DetectedFoodItem
import com.charleshartmann.grocyfridge.model.GrocyLocation
import com.charleshartmann.grocyfridge.model.GrocyProduct
import com.charleshartmann.grocyfridge.model.GrocyQuantityUnit
import com.charleshartmann.grocyfridge.model.GrocyStockItem
import com.charleshartmann.grocyfridge.model.InventoryRequest
import com.charleshartmann.grocyfridge.model.StorageLocation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}

private class FakeGrocyApi(
    val products: MutableList<GrocyProduct> = mutableListOf(),
    val locations: MutableList<GrocyLocation> = mutableListOf(GrocyLocation(id = 2, name = "Fridge")),
    val stock: MutableList<GrocyStockItem> = mutableListOf()
) : GrocyApi {
    val inventoryRequests = mutableListOf<Pair<Long, InventoryRequest>>()
    private var nextId = 100L

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
