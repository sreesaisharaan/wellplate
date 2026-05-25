package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.db.MealLogEntity
import com.example.data.local.db.WaterLogEntity
import com.example.data.local.db.WeightLogEntity
import com.example.data.local.db.ShoppingIngredientEntity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class SupabaseMeal(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "date") val date: String,
    @Json(name = "mealType") val mealType: String,
    @Json(name = "foodName") val foodName: String,
    @Json(name = "calories") val calories: Int,
    @Json(name = "proteinG") val proteinG: Double,
    @Json(name = "carbsG") val carbsG: Double,
    @Json(name = "fatG") val fatG: Double,
    @Json(name = "fibreG") val fibreG: Double,
    @Json(name = "sugarG") val sugarG: Double,
    @Json(name = "sodiumMg") val sodiumMg: Double,
    @Json(name = "confidencePercent") val confidencePercent: Int,
    @Json(name = "servingSizeG") val servingSizeG: Double,
    @Json(name = "quantityMultiplier") val quantityMultiplier: Double,
    @Json(name = "timestamp") val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class SupabaseWater(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "date") val date: String,
    @Json(name = "amountMl") val amountMl: Int,
    @Json(name = "timestamp") val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class SupabaseWeight(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "date") val date: String,
    @Json(name = "weightKg") val weightKg: Double,
    @Json(name = "timestamp") val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class SupabaseShoppingItem(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "ingredientName") val ingredientName: String,
    @Json(name = "isChecked") val isChecked: Boolean,
    @Json(name = "associatedFoodName") val associatedFoodName: String,
    @Json(name = "timestamp") val timestamp: Long
)

interface SupabaseService {
    @POST("rest/v1/meals")
    suspend fun insertMeals(@Body meals: List<SupabaseMeal>): Response<ResponseBody>

    @POST("rest/v1/water")
    suspend fun insertWaterLogs(@Body water: List<SupabaseWater>): Response<ResponseBody>

    @POST("rest/v1/weight")
    suspend fun insertWeightLogs(@Body weight: List<SupabaseWeight>): Response<ResponseBody>

    @POST("rest/v1/shopping_list")
    suspend fun insertShoppingItems(@Body items: List<SupabaseShoppingItem>): Response<ResponseBody>

    @GET("rest/v1/meals?order=timestamp.desc")
    suspend fun fetchMeals(): List<SupabaseMeal>

    @GET("rest/v1/water?order=timestamp.desc")
    suspend fun fetchWater(): List<SupabaseWater>

    @GET("rest/v1/weight?order=timestamp.desc")
    suspend fun fetchWeight(): List<SupabaseWeight>

    @GET("rest/v1/shopping_list?order=timestamp.desc")
    suspend fun fetchShopping(): List<SupabaseShoppingItem>
}

object SupabaseClient {
    private val rawUrl = try { BuildConfig.SUPABASE_URL } catch (e: Exception) { "" }
    private val supabaseUrl = sanitizeUrl(rawUrl)
    private val anonKey = try { BuildConfig.SUPABASE_ANON_KEY } catch (e: Exception) { "" }

    val supabase by lazy {
        createSupabaseClient(
            supabaseUrl = if (supabaseUrl.contains("placeholder_url") || supabaseUrl.isBlank()) "https://gzlhogloatgywrjbydjx.supabase.co" else supabaseUrl,
            supabaseKey = if (anonKey.contains("placeholder_anon") || anonKey.isBlank()) "placeholder_key" else anonKey
        ) {
            install(Auth) {
                // Keep session memory clean or customized
            }
        }
    }

    private fun sanitizeUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return "https://empty.supabase.co/"
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer $anonKey")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    private val moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val service: SupabaseService? by lazy {
        try {
            if (rawUrl.isBlank() || anonKey.isBlank() || rawUrl.contains("placeholder_url") || anonKey.contains("placeholder_anon")) {
                Log.w("SupabaseClient", "Supabase credentials are placeholders. Service will return null.")
                null
            } else {
                Retrofit.Builder()
                    .baseUrl(supabaseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                    .create(SupabaseService::class.java)
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Failed to build Retrofit client", e)
            null
        }
    }

    fun isConfigured(): Boolean {
        return service != null && rawUrl.isNotBlank() && !rawUrl.contains("placeholder")
    }

    fun getProjId(): String {
        if (rawUrl.isBlank()) return "Unknown"
        // Try parsing gzlhogloatgywrjbydjx.supabase.co
        val sub = rawUrl.substringAfter("://").substringBefore(".")
        return if (sub.isNotBlank()) sub else "Unknown"
    }

    suspend fun syncLocalToRemote(
        meals: List<MealLogEntity>,
        waterLogs: List<WaterLogEntity>,
        weights: List<WeightLogEntity>,
        shoppingItems: List<ShoppingIngredientEntity>
    ): Boolean {
        val s = service ?: return false
        try {
            if (meals.isNotEmpty()) {
                val mapped = meals.map { m ->
                    SupabaseMeal(
                        date = m.date,
                        mealType = m.mealType,
                        foodName = m.foodName,
                        calories = m.calories,
                        proteinG = m.proteinG,
                        carbsG = m.carbsG,
                        fatG = m.fatG,
                        fibreG = m.fibreG,
                        sugarG = m.sugarG,
                        sodiumMg = m.sodiumMg,
                        confidencePercent = m.confidencePercent,
                        servingSizeG = m.servingSizeG,
                        quantityMultiplier = m.quantityMultiplier,
                        timestamp = m.timestamp
                    )
                }
                val response = s.insertMeals(mapped)
                if (!response.isSuccessful) {
                    Log.e("SupabaseClient", "Meal Sync failed: ${response.errorBody()?.string()}")
                    return false
                }
            }

            if (waterLogs.isNotEmpty()) {
                val mapped = waterLogs.map { w ->
                    SupabaseWater(
                        date = w.date,
                        amountMl = w.amountMl,
                        timestamp = w.timestamp
                    )
                }
                val response = s.insertWaterLogs(mapped)
                if (!response.isSuccessful) {
                    Log.e("SupabaseClient", "Water Sync failed: ${response.errorBody()?.string()}")
                    return false
                }
            }

            if (weights.isNotEmpty()) {
                val mapped = weights.map { w ->
                    SupabaseWeight(
                        date = w.date,
                        weightKg = w.weightKg,
                        timestamp = w.timestamp
                    )
                }
                val response = s.insertWeightLogs(mapped)
                if (!response.isSuccessful) {
                    Log.e("SupabaseClient", "Weight Sync failed: ${response.errorBody()?.string()}")
                    return false
                }
            }

            if (shoppingItems.isNotEmpty()) {
                val mapped = shoppingItems.map { item ->
                    SupabaseShoppingItem(
                        ingredientName = item.ingredientName,
                        isChecked = item.isChecked,
                        associatedFoodName = item.associatedFoodName,
                        timestamp = item.timestamp
                    )
                }
                val response = s.insertShoppingItems(mapped)
                if (!response.isSuccessful) {
                    Log.e("SupabaseClient", "Shopping Sync failed: ${response.errorBody()?.string()}")
                    return false
                }
            }
            return true
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Error syncing local data to Supabase", e)
            return false
        }
    }

    suspend fun fetchRemoteMeals(): List<SupabaseMeal> {
        return service?.fetchMeals() ?: emptyList()
    }

    suspend fun fetchRemoteWater(): List<SupabaseWater> {
        return service?.fetchWater() ?: emptyList()
    }

    suspend fun fetchRemoteWeight(): List<SupabaseWeight> {
        return service?.fetchWeight() ?: emptyList()
    }

    suspend fun fetchRemoteShopping(): List<SupabaseShoppingItem> {
        return service?.fetchShopping() ?: emptyList()
    }
}
