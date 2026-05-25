package com.example.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MealTemplateDao {

    // Get all templates, favourites first, then sorted by most recently used
    @Transaction
    @Query("""
        SELECT * FROM meal_templates
        ORDER BY isFavourite DESC, lastUsedAt DESC
    """)
    fun getAllTemplates(): Flow<List<MealTemplateWithItems>>

    // Get only favourites
    @Transaction
    @Query("SELECT * FROM meal_templates WHERE isFavourite = 1 ORDER BY lastUsedAt DESC")
    fun getFavourites(): Flow<List<MealTemplateWithItems>>

    // Search by name
    @Transaction
    @Query("SELECT * FROM meal_templates WHERE name LIKE '%' || :query || '%' ORDER BY lastUsedAt DESC")
    fun searchTemplates(query: String): Flow<List<MealTemplateWithItems>>

    // Insert new template — returns the new template ID
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: MealTemplateEntity): Long

    // Insert all items for a template
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<MealTemplateItemEntity>)

    // Toggle favourite
    @Query("UPDATE meal_templates SET isFavourite = :isFav WHERE id = :id")
    suspend fun setFavourite(id: Long, isFav: Boolean)

    // Update last used timestamp and increment use count when logged
    @Query("""
        UPDATE meal_templates
        SET lastUsedAt = :timestamp, useCount = useCount + 1
        WHERE id = :id
    """)
    suspend fun markAsUsed(id: Long, timestamp: Long = System.currentTimeMillis())

    // Delete template (CASCADE deletes items automatically)
    @Query("DELETE FROM meal_templates WHERE id = :id")
    suspend fun deleteTemplate(id: Long)

    // Get single template with items
    @Transaction
    @Query("SELECT * FROM meal_templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): MealTemplateWithItems?
}
