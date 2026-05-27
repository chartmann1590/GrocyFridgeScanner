package com.charleshartmann.grocyfridge.data

import com.charleshartmann.grocyfridge.model.ConsumeRequest
import com.charleshartmann.grocyfridge.model.CreatedObjectResponse
import com.charleshartmann.grocyfridge.model.GrocyLocation
import com.charleshartmann.grocyfridge.model.GrocyProduct
import com.charleshartmann.grocyfridge.model.GrocyQuantityUnit
import com.charleshartmann.grocyfridge.model.GrocyStockItem
import com.charleshartmann.grocyfridge.model.InventoryRequest
import com.charleshartmann.grocyfridge.model.SystemInfoResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface GrocyApi {
    @GET("api/system/info")
    suspend fun systemInfo(): SystemInfoResponse

    @GET("api/objects/products")
    suspend fun products(): List<GrocyProduct>

    @GET("api/objects/quantity_units")
    suspend fun quantityUnits(): List<GrocyQuantityUnit>

    @GET("api/objects/locations")
    suspend fun locations(): List<GrocyLocation>

    @GET("api/stock")
    suspend fun stock(): List<GrocyStockItem>

    @POST("api/objects/products")
    suspend fun createProduct(@Body product: GrocyProduct): CreatedObjectResponse

    @POST("api/objects/locations")
    suspend fun createLocation(@Body location: GrocyLocation): CreatedObjectResponse

    @POST("api/stock/products/{productId}/inventory")
    suspend fun inventoryProduct(
        @Path("productId") productId: Long,
        @Body request: InventoryRequest
    )

    @POST("api/stock/products/{productId}/consume")
    suspend fun consumeProduct(
        @Path("productId") productId: Long,
        @Body request: ConsumeRequest
    )

    @PUT("api/objects/products/{productId}")
    suspend fun updateProduct(
        @Path("productId") productId: Long,
        @Body product: GrocyProduct
    )

    @DELETE("api/objects/products/{productId}")
    suspend fun deleteProduct(@Path("productId") productId: Long)
}
