package com.example.data.model

import com.google.gson.annotations.SerializedName

// Where the nutrition data came from
enum class DataSource {
    OPEN_FOOD_FACTS,   // verified — show green badge
    GEMINI_ESTIMATE    // AI estimate — show amber badge
}

// Final nutrition data used everywhere in the app
data class NutritionData(
    val foodName: String,
    val brandName: String? = null,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fibrePer100g: Double,
    val sugarPer100g: Double,
    val sodiumPer100g: Double,
    val servingGrams: Double = 100.0,
    val source: DataSource,
    val confidence: Int = 100,
    val imageUrl: String? = null
) {
    // All values scale automatically with serving size
    val calories get() = scale(caloriesPer100g)
    val protein  get() = scale(proteinPer100g)
    val carbs    get() = scale(carbsPer100g)
    val fat      get() = scale(fatPer100g)
    val fibre    get() = scale(fibrePer100g)
    val sugar    get() = scale(sugarPer100g)
    val sodium   get() = scale(sodiumPer100g)

    private fun scale(value: Double) =
        ((value * servingGrams / 100.0) * 10.0).toLong() / 10.0
}

// Open Food Facts raw API response models
data class OFFSearchResponse(
    val products: List<OFFProduct>,
    val count: Int
)

data class OFFProduct(
    val product_name: String?,
    val brands: String?,
    val image_front_small_url: String?,
    val nutriments: OFFNutriments?,
    val nova_group: Int?,           // food processing level 1-4
    val nutriscore_grade: String?   // a/b/c/d/e nutritional score
)

data class OFFNutriments(
    @SerializedName("energy-kcal_100g")
    val calories: Double?,
    @SerializedName("proteins_100g")
    val proteins_100g: Double?,
    @SerializedName("carbohydrates_100g")
    val carbohydrates_100g: Double?,
    @SerializedName("fat_100g")
    val fat_100g: Double?,
    @SerializedName("fiber_100g")
    val fibre: Double?,
    @SerializedName("sugars_100g")
    val sugars_100g: Double?,
    @SerializedName("sodium_100g")
    val sodium_100g: Double?
)

fun NutritionData.toFoodScanResult(): com.example.data.api.FoodScanResult {
    return com.example.data.api.FoodScanResult(
        foodName = if (brandName.isNullOrBlank()) foodName else "$foodName ($brandName)",
        servingSizeG = servingGrams,
        calories = (caloriesPer100g * servingGrams / 100.0).toInt(),
        proteinG = proteinPer100g * servingGrams / 100.0,
        carbsG = carbsPer100g * servingGrams / 100.0,
        fatG = fatPer100g * servingGrams / 100.0,
        fibreG = fibrePer100g * servingGrams / 100.0,
        sugarG = sugarPer100g * servingGrams / 100.0,
        sodiumMg = sodiumPer100g * servingGrams / 100.0,
        confidencePercent = confidence,
        source = source.name
    )
}

