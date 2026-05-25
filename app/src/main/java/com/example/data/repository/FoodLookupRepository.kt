package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.api.GeminiClient
import com.example.data.local.db.CustomFoodEntity
import com.example.data.local.db.WellplateDao
import com.example.data.model.DataSource
import com.example.data.model.NutritionData
import com.example.data.network.NetworkClient
import com.example.data.network.OpenFoodFactsApi

sealed class FoodLookupResult {
    data class Success(val nutrition: NutritionData) : FoodLookupResult()
    data class LowConfidence(val suggestedName: String) : FoodLookupResult()
    data class Error(val message: String) : FoodLookupResult()
}

class FoodLookupRepository(
    private val api: OpenFoodFactsApi = NetworkClient.openFoodFacts,
    private val dao: WellplateDao
) {

    // ── Called after camera scan (Gemini identifies food name) ──────────
    suspend fun lookupByName(foodName: String): NutritionData? {
        try {
            // First check local Room DB for cached estimates
            val cached = dao.getCustomFood(foodName)
            if (cached != null) {
                return NutritionData(
                    foodName = cached.foodName,
                    caloriesPer100g = cached.caloriesPer100g,
                    proteinPer100g = cached.proteinPer100g,
                    carbsPer100g = cached.carbsPer100g,
                    fatPer100g = cached.fatPer100g,
                    fibrePer100g = cached.fibrePer100g,
                    sugarPer100g = cached.sugarPer100g,
                    sodiumPer100g = cached.sodiumPer100g,
                    servingGrams = 100.0,
                    source = if (cached.source == "OPEN_FOOD_FACTS") DataSource.OPEN_FOOD_FACTS else DataSource.GEMINI_ESTIMATE
                )
            }

            // Otherwise, check Open Food Facts API
            val response = api.searchByName(foodName)
            if (response.count > 0 && response.products.isNotEmpty()) {
                val prod = response.products.first()
                val nuts = prod.nutriments
                return NutritionData(
                    foodName = prod.product_name ?: foodName,
                    brandName = prod.brands,
                    caloriesPer100g = nuts?.calories ?: 0.0,
                    proteinPer100g = nuts?.proteins_100g ?: 0.0,
                    carbsPer100g = nuts?.carbohydrates_100g ?: 0.0,
                    fatPer100g = nuts?.fat_100g ?: 0.0,
                    fibrePer100g = nuts?.fibre ?: 0.0,
                    sugarPer100g = nuts?.sugars_100g ?: 0.0,
                    sodiumPer100g = nuts?.sodium_100g ?: 0.0,
                    source = DataSource.OPEN_FOOD_FACTS,
                    imageUrl = prod.image_front_small_url
                )
            }
        } catch (e: Exception) {
            Log.e("FoodLookupRepository", "lookupByName error", e)
        }
        return null
    }

    // ── Called for barcode scan ──────────────────────────────────────────
    suspend fun lookupByBarcode(barcode: String): NutritionData? {
        try {
            val response = api.getByBarcode(barcode)
            if (response.status == 1 && response.product != null) {
                val prod = response.product
                val nuts = prod.nutriments
                return NutritionData(
                    foodName = prod.product_name ?: "Unknown Barcode Item",
                    brandName = prod.brands,
                    caloriesPer100g = nuts?.calories ?: 0.0,
                    proteinPer100g = nuts?.proteins_100g ?: 0.0,
                    carbsPer100g = nuts?.carbohydrates_100g ?: 0.0,
                    fatPer100g = nuts?.fat_100g ?: 0.0,
                    fibrePer100g = nuts?.fibre ?: 0.0,
                    sugarPer100g = nuts?.sugars_100g ?: 0.0,
                    sodiumPer100g = nuts?.sodium_100g ?: 0.0,
                    source = DataSource.OPEN_FOOD_FACTS,
                    imageUrl = prod.image_front_small_url
                )
            }
        } catch (e: Exception) {
            Log.e("FoodLookupRepository", "lookupByBarcode error ($barcode)", e)
        }
        return null
    }

    // ── Full two-step scan flow ──────────────────────────────────────────
    suspend fun lookupFromImage(bitmap: Bitmap): FoodLookupResult {
        return try {
            // Step 1: Gemini identifies the food name from image
            val geminiResult = GeminiClient.scanFoodWithImage(bitmap, isBarcodeScan = false)
            val foodName = geminiResult.foodName

            if (geminiResult.confidencePercent < 50) {
                return FoodLookupResult.LowConfidence(foodName)
            }

            // Step 2: Open Food Facts fetches verified nutrition
            val verified = lookupByName(foodName)
            if (verified != null) {
                FoodLookupResult.Success(verified)
            } else {
                // Step 3: Fall back to Gemini's own nutrition estimate
                val nutrition = NutritionData(
                    foodName = foodName,
                    caloriesPer100g = geminiResult.calories / (geminiResult.servingSizeG / 100.0),
                    proteinPer100g = geminiResult.proteinG / (geminiResult.servingSizeG / 100.0),
                    carbsPer100g = geminiResult.carbsG / (geminiResult.servingSizeG / 100.0),
                    fatPer100g = geminiResult.fatG / (geminiResult.servingSizeG / 100.0),
                    fibrePer100g = geminiResult.fibreG / (geminiResult.servingSizeG / 100.0),
                    sugarPer100g = geminiResult.sugarG / (geminiResult.servingSizeG / 100.0),
                    sodiumPer100g = geminiResult.sodiumMg / (geminiResult.servingSizeG / 100.0),
                    servingGrams = geminiResult.servingSizeG,
                    source = DataSource.GEMINI_ESTIMATE,
                    confidence = geminiResult.confidencePercent
                )

                // If Indian food or typical Indian patterns are detected, save to CustomFoods in Room as GEMINI_CACHED
                val isIndianPattern = isIndianFoodPattern(foodName)
                if (isIndianPattern) {
                    try {
                        dao.insertCustomFood(
                            CustomFoodEntity(
                                foodName = foodName,
                                caloriesPer100g = nutrition.caloriesPer100g,
                                proteinPer100g = nutrition.proteinPer100g,
                                carbsPer100g = nutrition.carbsPer100g,
                                fatPer100g = nutrition.fatPer100g,
                                fibrePer100g = nutrition.fibrePer100g,
                                sugarPer100g = nutrition.sugarPer100g,
                                sodiumPer100g = nutrition.sodiumPer100g,
                                source = "GEMINI_CACHED"
                            )
                        )
                    } catch (dbEx: Exception) {
                        Log.e("FoodLookupRepository", "Failed to cache Indian food in DB", dbEx)
                    }
                }

                FoodLookupResult.Success(nutrition)
            }
        } catch (e: Exception) {
            Log.e("FoodLookupRepository", "lookupFromImage error", e)
            FoodLookupResult.Error("Could not identify food or fetch details. Please try again.")
        }
    }

    // ── Manual text search (for Search & Add Food screen) ───────────────
    suspend fun searchFoods(query: String): List<NutritionData> {
        val list = mutableListOf<NutritionData>()
        try {
            // 1. Check local Room Custom DB first to handle cached Indian alternate spellings/items
            val cachedWord = dao.getCustomFood(query)
            if (cachedWord != null) {
                list.add(
                    NutritionData(
                        foodName = cachedWord.foodName,
                        caloriesPer100g = cachedWord.caloriesPer100g,
                        proteinPer100g = cachedWord.proteinPer100g,
                        carbsPer100g = cachedWord.carbsPer100g,
                        fatPer100g = cachedWord.fatPer100g,
                        fibrePer100g = cachedWord.fibrePer100g,
                        sugarPer100g = cachedWord.sugarPer100g,
                        sodiumPer100g = cachedWord.sodiumPer100g,
                        servingGrams = 100.0,
                        source = DataSource.GEMINI_ESTIMATE
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("FoodLookupRepository", "Failed room cached search check", e)
        }

        try {
            // 2. Search Open Food Facts
            val response = api.searchByName(query)
            if (response.count > 0) {
                response.products.forEach { prod ->
                    val nuts = prod.nutriments
                    list.add(
                        NutritionData(
                            foodName = prod.product_name ?: query,
                            brandName = prod.brands,
                            caloriesPer100g = nuts?.calories ?: 0.0,
                            proteinPer100g = nuts?.proteins_100g ?: 0.0,
                            carbsPer100g = nuts?.carbohydrates_100g ?: 0.0,
                            fatPer100g = nuts?.fat_100g ?: 0.0,
                            fibrePer100g = nuts?.fibre ?: 0.0,
                            sugarPer100g = nuts?.sugars_100g ?: 0.0,
                            sodiumPer100g = nuts?.sodium_100g ?: 0.0,
                            source = DataSource.OPEN_FOOD_FACTS,
                            imageUrl = prod.image_front_small_url
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("FoodLookupRepository", "Search Open Food Facts failed", e)
        }

        // 3. If no Open Food Facts or cached we call Gemini
        if (list.size <= 1) { // includes our 1 cached result
            try {
                // Query Gemini
                val geminiList = GeminiClient.queryCustomFoodManual(query)
                geminiList.forEach { scan ->
                    if (list.none { it.foodName.equals(scan.foodName, ignoreCase = true) }) {
                        val nutrition = NutritionData(
                            foodName = scan.foodName,
                            caloriesPer100g = scan.calories / (scan.servingSizeG / 100.0),
                            proteinPer100g = scan.proteinG / (scan.servingSizeG / 100.0),
                            carbsPer100g = scan.carbsG / (scan.servingSizeG / 100.0),
                            fatPer100g = scan.fatG / (scan.servingSizeG / 100.0),
                            fibrePer100g = scan.fibreG / (scan.servingSizeG / 100.0),
                            sugarPer100g = scan.sugarG / (scan.servingSizeG / 100.0),
                            sodiumPer100g = scan.sodiumMg / (scan.servingSizeG / 100.0),
                            servingGrams = scan.servingSizeG,
                            source = DataSource.GEMINI_ESTIMATE,
                            confidence = scan.confidencePercent
                        )
                        list.add(nutrition)

                        // Cache if Indian Food Pattern
                        if (isIndianFoodPattern(scan.foodName)) {
                            try {
                                dao.insertCustomFood(
                                    CustomFoodEntity(
                                        foodName = scan.foodName,
                                        caloriesPer100g = nutrition.caloriesPer100g,
                                        proteinPer100g = nutrition.proteinPer100g,
                                        carbsPer100g = nutrition.carbsPer100g,
                                        fatPer100g = nutrition.fatPer100g,
                                        fibrePer100g = nutrition.fibrePer100g,
                                        sugarPer100g = nutrition.sugarPer100g,
                                        sodiumPer100g = nutrition.sodiumPer100g,
                                        source = "GEMINI_CACHED"
                                    )
                                )
                            } catch (dbe: Exception) {
                                Log.e("FoodLookupRepository", "Insert error for search cache", dbe)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("FoodLookupRepository", "Gemini query fallback failed", e)
            }
        }

        return list
    }

    private fun isIndianFoodPattern(name: String): Boolean {
        val lowercase = name.lowercase()
        val keywords = listOf(
            "masala", "dosa", "roti", "paneer", "dal", "samosa", "chana", "curry", "rice",
            "paratha", "naan", "idli", "sambhar", "biryani", "raita", "pulao", "kheer",
            "jalebi", "lassi", "gulab jamun", "tikka", "korma", "aloogobi", "bhindi"
        )
        return keywords.any { lowercase.contains(it) }
    }
}
