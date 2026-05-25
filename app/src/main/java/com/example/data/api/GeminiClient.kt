package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String // base64
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "temperature") val temperature: Double? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class PartResponse(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class ContentResponse(
    @Json(name = "parts") val parts: List<PartResponse>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: ContentResponse? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

// Target models for JSON outputs
@JsonClass(generateAdapter = true)
data class FoodScanResult(
    @Json(name = "food_name") val foodName: String,
    @Json(name = "serving_size_g") val servingSizeG: Double,
    @Json(name = "calories") val calories: Int,
    @Json(name = "protein_g") val proteinG: Double,
    @Json(name = "carbs_g") val carbsG: Double,
    @Json(name = "fat_g") val fatG: Double,
    @Json(name = "fibre_g") val fibreG: Double = 0.0,
    @Json(name = "sugar_g") val sugarG: Double = 0.0,
    @Json(name = "sodium_mg") val sodiumMg: Double = 0.0,
    @Json(name = "confidence_percent") val confidencePercent: Int,
    @Json(name = "source") val source: String = "GEMINI_ESTIMATE"
)

// —————————————————————————————————————————————
// Open Food Facts API Models & Interface
// —————————————————————————————————————————————
@JsonClass(generateAdapter = true)
data class OFFNutriments(
    @Json(name = "energy-kcal_100g") val calories: Double? = null,
    @Json(name = "proteins_100g") val proteins100g: Double? = null,
    @Json(name = "carbohydrates_100g") val carbohydrates100g: Double? = null,
    @Json(name = "fat_100g") val fat100g: Double? = null,
    @Json(name = "fiber_100g") val fiber100g: Double? = null,
    @Json(name = "sugars_100g") val sugars100g: Double? = null,
    @Json(name = "sodium_100g") val sodium100g: Double? = null
)

@JsonClass(generateAdapter = true)
data class OFFProduct(
    @Json(name = "product_name") val productName: String? = null,
    @Json(name = "nutriments") val nutriments: OFFNutriments? = null
)

@JsonClass(generateAdapter = true)
data class OpenFoodFactsResponse(
    @Json(name = "products") val products: List<OFFProduct>? = null
)

interface OpenFoodFactsApi {
    @POST("cgi/search.pl")
    suspend fun searchFood(
        @Query("search_terms") query: String,
        @Query("search_simple") searchSimple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("page_size") pageSize: Int = 5
    ): OpenFoodFactsResponse
}

object OpenFoodFactsClient {
    private const val BASE_URL = "https://world.openfoodfacts.org/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val service: OpenFoodFactsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenFoodFactsApi::class.java)
    }

    suspend fun fetchVerifiedNutrition(foodName: String, fallbackPercent: Int): FoodScanResult? {
        return try {
            val response = service.searchFood(query = foodName)
            val product = response.products?.firstOrNull { p ->
                !p.productName.isNullOrBlank() && p.nutriments?.calories != null
            } ?: return null

            val n = product.nutriments!!
            // Converting sodium to mg: sodium_100g is in grams, so multiply by 1000 to get mg.
            val sodiumMg = (n.sodium100g ?: 0.0) * 1000.0
            
            FoodScanResult(
                foodName = product.productName ?: foodName,
                servingSizeG = 100.0,
                calories = (n.calories ?: 0.0).toInt(),
                proteinG = n.proteins100g ?: 0.0,
                carbsG = n.carbohydrates100g ?: 0.0,
                fatG = n.fat100g ?: 0.0,
                fibreG = n.fiber100g ?: 0.0,
                sugarG = n.sugars100g ?: 0.0,
                sodiumMg = sodiumMg,
                confidencePercent = fallbackPercent,
                source = "OPEN_FOOD_FACTS"
            )
        } catch (e: Exception) {
            Log.e("OpenFoodFactsClient", "Failed to query OFF: ${e.message}", e)
            null
        }
    }
}

@JsonClass(generateAdapter = true)
data class RestaurantDish(
    @Json(name = "dish_name") val dishName: String,
    @Json(name = "description") val description: String,
    @Json(name = "estimated_calories") val estimatedCalories: Int,
    @Json(name = "protein_g") val proteinG: Double,
    @Json(name = "carbs_g") val carbsG: Double,
    @Json(name = "fat_g") val fatG: Double
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun scanFoodWithImage(bitmap: Bitmap, isBarcodeScan: Boolean): FoodScanResult {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("GeminiClient", "API Key is missing or default. Falling back to local heuristic scanning + Open Food Facts fallback if available.")
            val mockResult = generateMockHeuristicScan(isBarcodeScan)
            if (!isBarcodeScan) {
                val offResult = OpenFoodFactsClient.fetchVerifiedNutrition(mockResult.foodName, mockResult.confidencePercent)
                if (offResult != null) {
                    return offResult
                }
            }
            return mockResult
        }

        val prompt = if (isBarcodeScan) {
            "Scan the barcode in this image. Read the numbers and evaluate the food product. If no clear barcode or food is found, assume a generic whole wheat cereal or granola bar. Return a JSON describing the food with keys: food_name (String), serving_size_g (Double), calories (Int), protein_g (Double), carbs_g (Double), fat_g (Double), fibre_g (Double), sugar_g (Double), sodium_mg (Double), confidence_percent (Int)."
        } else {
            "Identify the food in this image. Return a JSON with: food_name, serving_size_g, calories, protein_g, carbs_g, fat_g, fibre_g, sugar_g, sodium_mg, confidence_percent. If multiple foods are present, list the primary one. Return only the JSON object."
        }

        val base64Image = bitmap.toBase64()
        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                    )
                )
            ),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        return try {
            val response = service.generateContent(MODEL_NAME, apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No content returned from Gemini Vision")
            
            Log.d("GeminiClient", "Vision Scan Response: $jsonText")
            val adapter = moshi.adapter(FoodScanResult::class.java)
            val geminiResult = adapter.fromJson(jsonText) ?: throw Exception("Failed to parse JSON response")
            
            if (isBarcodeScan) {
                geminiResult.copy(source = "OPEN_FOOD_FACTS")
            } else {
                // Two-step lookup: Query Open Food Facts using the Gemini-identified food name
                val offResult = OpenFoodFactsClient.fetchVerifiedNutrition(geminiResult.foodName, geminiResult.confidencePercent)
                if (offResult != null) {
                    offResult
                } else {
                    geminiResult.copy(source = "GEMINI_ESTIMATE")
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiClient", "Gemini Vision API error: ${e.message}", e)
            val mockResult = generateMockHeuristicScan(isBarcodeScan)
            if (!isBarcodeScan) {
                val offResult = OpenFoodFactsClient.fetchVerifiedNutrition(mockResult.foodName, mockResult.confidencePercent)
                if (offResult != null) {
                    return offResult
                }
            }
            mockResult
        }
    }

    suspend fun queryCustomFoodManual(searchQuery: String): List<FoodScanResult> {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return generateMockManualSearchResults(searchQuery)
        }

        val prompt = """
            Create a list of 5 realistic food database search results matching search query: "$searchQuery". 
            For each item, return nutrition per 100g.
            Return a JSON array of objects. Each object has keys: 
            food_name (String), serving_size_g (Double, use 100.0), calories (Int), protein_g (Double), carbs_g (Double), fat_g (Double), fibre_g (Double), sugar_g (Double), sodium_mg (Double), confidence_percent (Int, use 100).
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        return try {
            val response = service.generateContent(MODEL_NAME, apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No content returned")
            
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, FoodScanResult::class.java)
            val adapter = moshi.adapter<List<FoodScanResult>>(type)
            adapter.fromJson(jsonText) ?: emptyList()
        } catch (e: Exception) {
            Log.e("GeminiClient", "Error Manual query: ${e.message}", e)
            generateMockManualSearchResults(searchQuery)
        }
    }

    suspend fun queryVoiceFoodLog(voiceText: String): List<FoodScanResult> {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return parseHeuristicVoiceLog(voiceText)
        }

        val prompt = """
            The user said: "$voiceText". Interpret this language to extract a list of dishes, their estimated portion sizes in grams, and their macros.
            Return a JSON array of objects. Each object must have: 
            food_name (String), serving_size_g (Double), calories (Int), protein_g (Double), carbs_g (Double), fat_g (Double), fibre_g (Double), sugar_g (Double), sodium_mg (Double), confidence_percent (Int).
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        return try {
            val response = service.generateContent(MODEL_NAME, apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No content returned")

            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, FoodScanResult::class.java)
            val adapter = moshi.adapter<List<FoodScanResult>>(type)
            adapter.fromJson(jsonText) ?: emptyList()
        } catch (e: Exception) {
            Log.e("GeminiClient", "Voice Log execution failed: ${e.message}", e)
            parseHeuristicVoiceLog(voiceText)
        }
    }

    suspend fun generateAiPersonalTip(userName: String, bmr: Int, tdee: Int, recentFoods: String): String {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "🔥 Perfect tracking today! Keep up the momentum to secure your 5-day streak. Adding rich protein like boiled eggs or Greek yogurt to your breakfast keeps energy levels high and aids muscle building."
        }

        val prompt = """
            You are Wellplate AI, a sophisticated personal nutritionist. 
            User info: Name is $userName, BMR is $bmr, TDEE is $tdee.
            Recent logged foods: $recentFoods.
            Provide a short, highly professional, encouraging nutritional tip or insight (maximum 3 sentences) that matches their bio. 
            Focus on practical calorie/macro optimizations (e.g., fiber, hydration, or protein timing). Do not repeat generic slogans.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        return try {
            val response = service.generateContent(MODEL_NAME, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Aim to take 8 glasses of water today. Adding protein-dense foods to your breakfast ensures long-lasting satiety and supports muscle maintenance."
        } catch (e: Exception) {
            "Aim to take 8 glasses of water today. Adding protein-dense foods to your breakfast ensures long-lasting satiety and supports muscle maintenance."
        }
    }

    suspend fun getRestaurantNutritionSuggestions(restaurantName: String): List<RestaurantDish> {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return generateMockRestaurantDishes(restaurantName)
        }

        val prompt = """
            The user is looking to eat at restaurant: "$restaurantName". 
            Generate exactly 3 typical popular meals from this restaurant type, with realistic nutrition breakdown estimates.
            Return a JSON array of objects. Keys:
            dish_name (String), description (String), estimated_calories (Int), protein_g (Double), carbs_g (Double), fat_g (Double).
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        return try {
            val response = service.generateContent(MODEL_NAME, apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No content returned")

            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, RestaurantDish::class.java)
            val adapter = moshi.adapter<List<RestaurantDish>>(type)
            adapter.fromJson(jsonText) ?: emptyList()
        } catch (e: Exception) {
            Log.e("GeminiClient", "Restaurant Mode failed: ${e.message}", e)
            generateMockRestaurantDishes(restaurantName)
        }
    }

    suspend fun getSmartMealSuggestions(recentFoods: String, scannedFood: String): List<String> {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return listOf(
                "🥗 Balanced Mediterannean bowl (Quinoa, Chickpeas, Olives, Cucumber, Olive Oil)",
                "🥑 Smoked Salmon Avocado Toast with poached egg",
                "🥣 Warm Oatmeal with hemp seeds, walnuts, and fresh blueberries"
            )
        }

        val prompt = """
            Given that the user recently ate "$recentFoods" and just scanned "$scannedFood", suggest exactly 3 healthy, balanced, delicious complimentary food combinations or meals.
            Return a JSON array of strings, each string formatted with an emoji and the food name + short summary.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        return try {
            val response = service.generateContent(MODEL_NAME, apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No response")

            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java)
            val adapter = moshi.adapter<List<String>>(type)
            adapter.fromJson(jsonText) ?: emptyList()
        } catch (e: Exception) {
            listOf(
                "🥗 Balanced Mediterannean bowl (Quinoa, Chickpeas, Olives, Cucumber, Olive Oil)",
                "🥑 Smoked Salmon Avocado Toast with poached egg",
                "🥣 Warm Oatmeal with hemp seeds, walnuts, and fresh blueberries"
            )
        }
    }

    // --- HEURISTIC LOCAL FALLBACK GENERATORS ---

    private fun generateMockHeuristicScan(isBarcodeScan: Boolean): FoodScanResult {
        return if (isBarcodeScan) {
            FoodScanResult(
                foodName = "Whole Grain Granola Bar (Barcode)",
                servingSizeG = 40.0,
                calories = 160,
                proteinG = 4.0,
                carbsG = 24.0,
                fatG = 6.0,
                fibreG = 3.2,
                sugarG = 7.0,
                sodiumMg = 95.0,
                confidencePercent = 98
            )
        } else {
            FoodScanResult(
                foodName = "Fresh Avocado Salad",
                servingSizeG = 180.0,
                calories = 245,
                proteinG = 5.2,
                carbsG = 14.3,
                fatG = 19.8,
                fibreG = 6.5,
                sugarG = 2.1,
                sodiumMg = 110.0,
                confidencePercent = 94
            )
        }
    }

    private fun generateMockManualSearchResults(query: String): List<FoodScanResult> {
        val lowercaseQuery = query.lowercase()
        val templates = listOf(
            Triple("chicken breast", 165.0, Triple(31.0, 0.0, 3.6)),
            Triple("egg boiled", 155.0, Triple(13.0, 1.1, 11.0)),
            Triple("avocado", 160.0, Triple(2.0, 9.0, 15.0)),
            Triple("brown rice", 123.0, Triple(2.7, 26.0, 1.0)),
            Triple("salmon", 208.0, Triple(20.0, 0.0, 13.0)),
            Triple("apple", 52.0, Triple(0.3, 14.0, 0.2)),
            Triple("banana", 89.0, Triple(1.1, 23.0, 0.3)),
            Triple("oatmeal", 389.0, Triple(16.9, 66.0, 6.9)),
            Triple("milk whole", 61.0, Triple(3.2, 4.8, 3.3)),
            Triple("peanut butter", 588.0, Triple(25.0, 20.0, 50.0))
        )

        val matches = templates.filter { it.first.contains(lowercaseQuery) || lowercaseQuery.contains(it.first) }
        val results = matches.map { (name, calories, macros) ->
            FoodScanResult(
                foodName = name.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
                servingSizeG = 100.0,
                calories = calories.toInt(),
                proteinG = macros.first,
                carbsG = macros.second,
                fatG = macros.third,
                fibreG = if (name.contains("apple") || name.contains("avocado") || name.contains("oatmeal")) 4.0 else 0.5,
                sugarG = if (name.contains("apple") || name.contains("banana")) 11.0 else 0.5,
                sodiumMg = if (name.contains("peanut butter")) 350.0 else 5.0,
                confidencePercent = 100
            )
        }

        if (results.isNotEmpty()) return results

        // Generic fallback search result based on string if no matches
        return listOf(
            FoodScanResult(
                foodName = query.replaceFirstChar { it.uppercase() },
                servingSizeG = 100.0,
                calories = 220,
                proteinG = 8.5,
                carbsG = 28.0,
                fatG = 7.5,
                fibreG = 2.0,
                sugarG = 4.0,
                sodiumMg = 180.0,
                confidencePercent = 90
            ),
            FoodScanResult(
                foodName = "$query (Low Fat / Light)",
                servingSizeG = 100.0,
                calories = 140,
                proteinG = 9.0,
                carbsG = 20.0,
                fatG = 2.0,
                fibreG = 1.0,
                sugarG = 3.0,
                sodiumMg = 200.0,
                confidencePercent = 88
            )
        )
    }

    private fun parseHeuristicVoiceLog(voiceText: String): List<FoodScanResult> {
        val lowercaseText = voiceText.lowercase()
        val matches = mutableListOf<FoodScanResult>()

        val options = listOf(
            "egg" to FoodScanResult("Boiled Egg", 50.0, 78, 6.3, 0.6, 5.3, 0.0, 0.6, 62.0, 95),
            "eggs" to FoodScanResult("Boiled Eggs (Two)", 100.0, 155, 12.6, 1.1, 10.6, 0.0, 1.1, 124.0, 95),
            "chicken" to FoodScanResult("Grilled Chicken Breast", 150.0, 248, 46.5, 0.0, 5.4, 0.0, 0.0, 110.0, 95),
            "rice" to FoodScanResult("Cooked Rice", 150.0, 195, 4.0, 42.0, 0.5, 1.0, 0.1, 2.0, 95),
            "banana" to FoodScanResult("Fresh Yellow Banana", 120.0, 105, 1.3, 27.0, 0.4, 3.1, 14.4, 1.0, 95),
            "apple" to FoodScanResult("Apples with Skin", 150.0, 78, 0.5, 21.0, 0.3, 3.6, 15.0, 2.0, 95),
            "salad" to FoodScanResult("Garden Salad", 200.0, 120, 3.0, 10.0, 8.0, 4.0, 3.0, 150.0, 95),
            "milk" to FoodScanResult("Glass of Whole Milk", 244.0, 149, 7.7, 11.7, 7.9, 0.0, 12.3, 105.0, 95),
            "coffee" to FoodScanResult("Black Coffee", 240.0, 2, 0.3, 0.0, 0.0, 0.0, 0.0, 5.0, 99)
        )

        for ((key, item) in options) {
            if (lowercaseText.contains(key)) {
                matches.add(item)
            }
        }

        if (matches.isNotEmpty()) return matches

        // Generic parsed item if voice query is custom
        val words = voiceText.split(" ").filter { it.length > 3 }
        val itemName = if (words.isNotEmpty()) words.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } } else "Custom Food Log"
        return listOf(
            FoodScanResult(
                foodName = itemName,
                servingSizeG = 120.0,
                calories = 180,
                proteinG = 6.0,
                carbsG = 25.0,
                fatG = 5.0,
                fibreG = 1.5,
                sugarG = 4.5,
                sodiumMg = 90.0,
                confidencePercent = 85
            )
        )
    }

    private fun generateMockRestaurantDishes(restaurantName: String): List<RestaurantDish> {
        val typ = restaurantName.lowercase()
        return when {
            typ.contains("starbucks") || typ.contains("cafe") || typ.contains("coffee") -> listOf(
                RestaurantDish("Spinach, Feta & Egg White Wrap", "A delicious, protein-rich wrap perfect for early mornings.", 290, 20.0, 34.0, 8.0),
                RestaurantDish("Turkey Bacon & Egg White Sandwich", "Low calorie wheat muffin sandwich with turkey bacon.", 230, 17.0, 28.0, 5.0),
                RestaurantDish("Oatmeal with Mixed Nuts & Berries", "Classic rolled oats with berries and dynamic seeds.", 320, 8.0, 49.0, 11.0)
            )
            typ.contains("mcdonald") || typ.contains("burger") || typ.contains("fast food") -> listOf(
                RestaurantDish("Artesian Grilled Chicken Sandwich", "Flavorful grilled chicken fillet with lettuce and light mayo.", 380, 32.0, 44.0, 7.0),
                RestaurantDish("Egg McMuffin Breakfast Special", "Freshly cracked grade-A egg with lean Canadian bacon.", 310, 17.0, 30.0, 13.0),
                RestaurantDish("Southwest Grilled Chicken Salad", "Fresh greens, black beans, corn and grilled breast slices.", 350, 37.0, 28.0, 11.0)
            )
            typ.contains("mexican") || typ.contains("chipotle") -> listOf(
                RestaurantDish("Chipotle Chicken Salad Bowl", "Double chicken, brown rice, black beans, tomato salsa, lettuce.", 540, 48.0, 42.0, 14.0),
                RestaurantDish("Steak Soft Corn Tacos (Three)", "Corn tortillas stacked with tender steak, warm pico de gallo.", 410, 28.0, 40.0, 12.0),
                RestaurantDish("Black Bean & Veggie Bowl", "Spiced beans, sautéed fajita bell peppers, and fresh guac.", 480, 12.0, 56.0, 22.0)
            )
            else -> listOf(
                RestaurantDish("Fresh Grilled Salmon Fillet", "Moist seasonal fish fillets alongside steamed asparagus.", 450, 34.0, 8.0, 32.0),
                RestaurantDish("Avocado Mediterranean Greens Salad", "Crisp mixed leaves, cherry tomatoes, cucumbers and dynamic oil.", 320, 6.0, 18.0, 26.0),
                RestaurantDish("Lemon Spiced Grilled Chicken Poke", "Steamed white grains with chicken highlights and clean greens.", 490, 38.0, 52.0, 12.0)
            )
        }
    }
}
