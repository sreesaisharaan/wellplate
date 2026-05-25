package com.example.data.repository

import com.example.data.local.db.MealLogEntity
import com.example.data.local.db.MealTemplateDao
import com.example.data.local.db.MealTemplateEntity
import com.example.data.local.db.MealTemplateItemEntity
import com.example.data.local.db.MealTemplateWithItems
import com.example.data.local.db.WellplateDao
import kotlinx.coroutines.flow.Flow

class MealTemplateRepository(
    private val templateDao: MealTemplateDao,
    private val wellplateDao: WellplateDao
) {

    val allTemplates: Flow<List<MealTemplateWithItems>> = templateDao.getAllTemplates()
    val favourites: Flow<List<MealTemplateWithItems>> = templateDao.getFavourites()

    // Save current meal log as a new template
    suspend fun saveAsTemplate(
        name: String,
        mealType: String,
        items: List<MealLogEntity>
    ): Long {
        val totalCal  = items.sumOf { it.calories }
        val totalProt = items.sumOf { it.proteinG }
        val totalCarb = items.sumOf { it.carbsG }
        val totalFat  = items.sumOf { it.fatG }

        val template = MealTemplateEntity(
            name           = name.trim(),
            mealType       = mealType,
            totalCalories  = totalCal,
            totalProtein   = totalProt,
            totalCarbs     = totalCarb,
            totalFat       = totalFat
        )
        val templateId = templateDao.insertTemplate(template)

        val entityItems = items.map { item ->
            val loggedSg = item.servingSizeG * item.quantityMultiplier
            val sg = if (loggedSg > 0.0) loggedSg else 100.0
            
            MealTemplateItemEntity(
                templateId      = templateId,
                foodName        = item.foodName,
                brandName       = null,
                servingGrams    = sg,
                caloriesPer100g = if (sg > 0) item.calories * 100.0 / sg else item.calories.toDouble(),
                proteinPer100g  = if (sg > 0) item.proteinG * 100.0 / sg else item.proteinG,
                carbsPer100g    = if (sg > 0) item.carbsG * 100.0 / sg else item.carbsG,
                fatPer100g      = if (sg > 0) item.fatG * 100.0 / sg else item.fatG,
                fibrePer100g    = if (sg > 0) item.fibreG * 100.0 / sg else item.fibreG,
                dataSource      = "GEMINI_ESTIMATE"
            )
        }
        templateDao.insertItems(entityItems)
        return templateId
    }

    // Log all items in a template to a meal
    suspend fun logTemplate(
        templateId: Long,
        targetMeal: String,
        selectedDate: String
    ) {
        val template = templateDao.getTemplateById(templateId) ?: return

        // Add each item to the meal log
        template.items.forEach { item ->
            val meal = MealLogEntity(
                date = selectedDate,
                mealType = targetMeal,
                foodName = item.foodName,
                calories = (item.caloriesPer100g * item.servingGrams / 100.0).toInt(),
                proteinG = item.proteinPer100g * item.servingGrams / 100.0,
                carbsG = item.carbsPer100g * item.servingGrams / 100.0,
                fatG = item.fatPer100g * item.servingGrams / 100.0,
                fibreG = item.fibrePer100g * item.servingGrams / 100.0,
                sugarG = 0.0,
                sodiumMg = 0.0,
                confidencePercent = 100,
                servingSizeG = item.servingGrams,
                quantityMultiplier = 1.0,
                timestamp = System.currentTimeMillis()
            )
            wellplateDao.insertMeal(meal)
        }

        // Update last used
        templateDao.markAsUsed(templateId)
    }

    suspend fun toggleFavourite(id: Long, currentValue: Boolean) {
        templateDao.setFavourite(id, !currentValue)
    }

    suspend fun deleteTemplate(id: Long) = templateDao.deleteTemplate(id)

    fun search(query: String): Flow<List<MealTemplateWithItems>> = templateDao.searchTemplates(query)
}
