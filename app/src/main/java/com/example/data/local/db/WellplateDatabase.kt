package com.example.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "meals")
data class MealLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // yyyy-MM-dd
    val mealType: String, // Breakfast, Lunch, Dinner, Snacks
    val foodName: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fibreG: Double,
    val sugarG: Double,
    val sodiumMg: Double,
    val confidencePercent: Int,
    val servingSizeG: Double,
    val quantityMultiplier: Double = 1.0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "water")
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // yyyy-MM-dd
    val amountMl: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "weight")
data class WeightLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // yyyy-MM-dd
    val weightKg: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "shopping_list")
data class ShoppingIngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ingredientName: String,
    val isChecked: Boolean = false,
    val associatedFoodName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_foods")
data class CustomFoodEntity(
    @PrimaryKey val foodName: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fibrePer100g: Double,
    val sugarPer100g: Double = 0.0,
    val sodiumPer100g: Double = 0.0,
    val source: String, // "GEMINI_CACHED" or "USER_CREATED"
    val lastUsed: Long = System.currentTimeMillis()
)

@Dao
interface WellplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomFood(food: CustomFoodEntity)

    @Query("SELECT * FROM custom_foods WHERE foodName = :foodName LIMIT 1")
    suspend fun getCustomFood(foodName: String): CustomFoodEntity?

    @Query("SELECT * FROM meals")
    suspend fun getAllMealsSync(): List<MealLogEntity>

    @Query("SELECT * FROM meals WHERE date = :date ORDER BY timestamp DESC")
    fun getMealsForDate(date: String): Flow<List<MealLogEntity>>

    @Query("SELECT * FROM meals WHERE date = :date")
    suspend fun getMealsForDateSync(date: String): List<MealLogEntity>

    @Query("SELECT * FROM meals ORDER BY timestamp DESC")
    fun getAllMeals(): Flow<List<MealLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<MealLogEntity>)

    @Delete
    suspend fun deleteMeal(meal: MealLogEntity)

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun deleteMealById(id: Int)

    @Query("SELECT * FROM water")
    suspend fun getAllWaterLogsSync(): List<WaterLogEntity>

    @Query("SELECT * FROM water WHERE date = :date")
    fun getWaterLogsForDate(date: String): Flow<List<WaterLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWater(water: WaterLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLogs(water: List<WaterLogEntity>)

    @Query("SELECT * FROM weight")
    suspend fun getAllWeightLogsSync(): List<WeightLogEntity>

    @Query("SELECT * FROM weight ORDER BY date ASC")
    fun getWeightHistory(): Flow<List<WeightLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weight: WeightLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLogs(weight: List<WeightLogEntity>)

    @Query("SELECT * FROM shopping_list")
    suspend fun getAllShoppingListSync(): List<ShoppingIngredientEntity>

    @Query("SELECT * FROM shopping_list ORDER BY timestamp DESC")
    fun getShoppingList(): Flow<List<ShoppingIngredientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingIngredient(item: ShoppingIngredientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingList(items: List<ShoppingIngredientEntity>)

    @Update
    suspend fun updateShoppingIngredient(item: ShoppingIngredientEntity)

    @Delete
    suspend fun deleteShoppingIngredient(item: ShoppingIngredientEntity)

    @Query("DELETE FROM shopping_list")
    suspend fun clearShoppingList()
}

@Database(
    entities = [
        MealLogEntity::class,
        WaterLogEntity::class,
        WeightLogEntity::class,
        ShoppingIngredientEntity::class,
        CustomFoodEntity::class,
        MealTemplateEntity::class,
        MealTemplateItemEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class WellplateDatabase : RoomDatabase() {
    abstract fun dao(): WellplateDao
    abstract fun templateDao(): MealTemplateDao
}
