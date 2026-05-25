package com.example.data.network

import com.example.data.model.OFFProduct
import com.example.data.model.OFFSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenFoodFactsApi {

    // Search by food name (used after Gemini identifies the food)
    @GET("cgi/search.pl")
    suspend fun searchByName(
        @Query("search_terms")  query: String,
        @Query("search_simple") searchSimple: Int = 1,
        @Query("action")        action: String = "process",
        @Query("json")          json: Int = 1,
        @Query("page_size")     pageSize: Int = 5,
        @Query("fields")        fields: String =
            "product_name,brands,nutriments,image_front_small_url,nova_group,nutriscore_grade"
    ): OFFSearchResponse

    // Look up by barcode (exact match — very fast)
    @GET("api/v0/product/{barcode}.json")
    suspend fun getByBarcode(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String =
            "product_name,brands,nutriments,image_front_small_url,nova_group,nutriscore_grade"
    ): OFFBarcodeResponse
}

data class OFFBarcodeResponse(
    val status: Int,          // 1 = found, 0 = not found
    val product: OFFProduct?
)
