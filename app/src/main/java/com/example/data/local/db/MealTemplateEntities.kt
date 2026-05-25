package com.example.data.local.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation

// A saved meal template (e.g. "My usual breakfast")
@Entity(tableName = "meal_templates")
data class MealTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,               // user-given name e.g. "Morning oats"
    val mealType: String,           // "Breakfast","Lunch","Dinner","Snacks"
    val totalCalories: Int,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    val isFavourite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis(),
    val useCount: Int = 0           // how many times logged — used for sorting
)

// Individual food items inside a template (one template has many items)
@Entity(
    tableName = "meal_template_items",
    foreignKeys = [ForeignKey(
        entity = MealTemplateEntity::class,
        parentColumns = ["id"],
        childColumns = ["templateId"],
        onDelete = ForeignKey.CASCADE  // delete items when template deleted
    )]
)
data class MealTemplateItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val templateId: Long,
    val foodName: String,
    val brandName: String? = null,
    val servingGrams: Double,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fibrePer100g: Double,
    val dataSource: String          // "OPEN_FOOD_FACTS" or "GEMINI_ESTIMATE"
)

// Relation: template with all its items
data class MealTemplateWithItems(
    @Embedded val template: MealTemplateEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "templateId"
    )
    val items: List<MealTemplateItemEntity>
)
