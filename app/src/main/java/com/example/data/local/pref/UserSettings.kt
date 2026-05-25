package com.example.data.local.pref

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserSettingsPreferences(private val context: Context) {
    // Keys
    companion object {
        val KEY_ONBOARDED = booleanPreferencesKey("onboarded")
        val KEY_NAME = stringPreferencesKey("name")
        val KEY_AGE = intPreferencesKey("age")
        val KEY_SEX = stringPreferencesKey("sex")
        val KEY_HEIGHT = doublePreferencesKey("height")
        val KEY_HEIGHT_UNIT = stringPreferencesKey("height_unit") // cm / ft
        val KEY_WEIGHT = doublePreferencesKey("weight")
        val KEY_WEIGHT_UNIT = stringPreferencesKey("weight_unit") // kg / lbs
        val KEY_GOAL_WEIGHT = doublePreferencesKey("goal_weight")
        val KEY_GOAL_TYPE = stringPreferencesKey("goal_type") // Lose, Maintain, Gain
        val KEY_PACE = stringPreferencesKey("pace") // Slow, Moderate, Fast
        val KEY_ACTIVITY_LEVEL = stringPreferencesKey("activity_level") // Sedentary, Light, Moderate, Very, Athlete
        val KEY_DIETARY_PREFS = stringPreferencesKey("dietary_prefs") // comma separated
        val KEY_ALLERGIES = stringPreferencesKey("allergies") // comma separated
        
        // Calculated Goals
        val KEY_CALORIE_GOAL = intPreferencesKey("calorie_goal")
        val KEY_PROTEIN_GOAL = doublePreferencesKey("protein_goal")
        val KEY_CARBS_GOAL = doublePreferencesKey("carbs_goal")
        val KEY_FAT_GOAL = doublePreferencesKey("fat_goal")
        
        // Extra Preferences
        val KEY_UNITS = stringPreferencesKey("units") // METRIC, IMPERIAL
        val KEY_THEME = stringPreferencesKey("theme") // LIGHT, DARK, SYSTEM
        val KEY_MEAL_REMINDER = booleanPreferencesKey("meal_reminder")
        val KEY_WATER_REMINDER = booleanPreferencesKey("water_reminder")
        val KEY_DAILY_SUMMARY = booleanPreferencesKey("daily_summary")
        val KEY_STREAK = intPreferencesKey("streak")
        val KEY_LAST_ACTIVE_DATE = stringPreferencesKey("last_active_date")
    }

    val onboardedFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDED] ?: false }
    val nameFlow: Flow<String> = context.dataStore.data.map { it[KEY_NAME] ?: "" }
    val calorieGoalFlow: Flow<Int> = context.dataStore.data.map { it[KEY_CALORIE_GOAL] ?: 2000 }
    val proteinGoalFlow: Flow<Double> = context.dataStore.data.map { it[KEY_PROTEIN_GOAL] ?: 130.0 }
    val carbsGoalFlow: Flow<Double> = context.dataStore.data.map { it[KEY_CARBS_GOAL] ?: 220.0 }
    val fatGoalFlow: Flow<Double> = context.dataStore.data.map { it[KEY_FAT_GOAL] ?: 65.0 }
    val unitsFlow: Flow<String> = context.dataStore.data.map { it[KEY_UNITS] ?: "METRIC" }
    val themeFlow: Flow<String> = context.dataStore.data.map { it[KEY_THEME] ?: "SYSTEM" }
    val streakFlow: Flow<Int> = context.dataStore.data.map { it[KEY_STREAK] ?: 5 } 
    
    // Get all user configs as a unified model
    val userProfileFlow: Flow<UserProfile> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
        UserProfile(
            isOnboarded = prefs[KEY_ONBOARDED] ?: false,
            name = prefs[KEY_NAME] ?: "",
            age = prefs[KEY_AGE] ?: 25,
            sex = prefs[KEY_SEX] ?: "Prefer not to say",
            height = prefs[KEY_HEIGHT] ?: 170.0,
            heightUnit = prefs[KEY_HEIGHT_UNIT] ?: "cm",
            weight = prefs[KEY_WEIGHT] ?: 70.0,
            weightUnit = prefs[KEY_WEIGHT_UNIT] ?: "kg",
            goalWeight = prefs[KEY_GOAL_WEIGHT] ?: 65.0,
            goalType = prefs[KEY_GOAL_TYPE] ?: "Maintain",
            pace = prefs[KEY_PACE] ?: "Moderate",
            activityLevel = prefs[KEY_ACTIVITY_LEVEL] ?: "Moderately active",
            dietaryPrefs = (prefs[KEY_DIETARY_PREFS] ?: "").split(",").filter { it.isNotEmpty() },
            allergies = (prefs[KEY_ALLERGIES] ?: "").split(",").filter { it.isNotEmpty() },
            calorieGoal = prefs[KEY_CALORIE_GOAL] ?: 2000,
            proteinGoal = prefs[KEY_PROTEIN_GOAL] ?: 130.0,
            carbsGoal = prefs[KEY_CARBS_GOAL] ?: 220.0,
            fatGoal = prefs[KEY_FAT_GOAL] ?: 65.0,
            units = prefs[KEY_UNITS] ?: "METRIC",
            theme = prefs[KEY_THEME] ?: "SYSTEM",
            mealReminder = prefs[KEY_MEAL_REMINDER] ?: true,
            waterReminder = prefs[KEY_WATER_REMINDER] ?: true,
            dailySummary = prefs[KEY_DAILY_SUMMARY] ?: true,
            streak = prefs[KEY_STREAK] ?: 5,
            lastActiveDate = prefs[KEY_LAST_ACTIVE_DATE] ?: ""
        )
    }

    suspend fun saveProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDED] = profile.isOnboarded
            prefs[KEY_NAME] = profile.name
            prefs[KEY_AGE] = profile.age
            prefs[KEY_SEX] = profile.sex
            prefs[KEY_HEIGHT] = profile.height
            prefs[KEY_HEIGHT_UNIT] = profile.heightUnit
            prefs[KEY_WEIGHT] = profile.weight
            prefs[KEY_WEIGHT_UNIT] = profile.weightUnit
            prefs[KEY_GOAL_WEIGHT] = profile.goalWeight
            prefs[KEY_GOAL_TYPE] = profile.goalType
            prefs[KEY_PACE] = profile.pace
            prefs[KEY_ACTIVITY_LEVEL] = profile.activityLevel
            prefs[KEY_DIETARY_PREFS] = profile.dietaryPrefs.joinToString(",")
            prefs[KEY_ALLERGIES] = profile.allergies.joinToString(",")
            prefs[KEY_CALORIE_GOAL] = profile.calorieGoal
            prefs[KEY_PROTEIN_GOAL] = profile.proteinGoal
            prefs[KEY_CARBS_GOAL] = profile.carbsGoal
            prefs[KEY_FAT_GOAL] = profile.fatGoal
            prefs[KEY_UNITS] = profile.units
            prefs[KEY_THEME] = profile.theme
            prefs[KEY_MEAL_REMINDER] = profile.mealReminder
            prefs[KEY_WATER_REMINDER] = profile.waterReminder
            prefs[KEY_DAILY_SUMMARY] = profile.dailySummary
            prefs[KEY_STREAK] = profile.streak
            prefs[KEY_LAST_ACTIVE_DATE] = profile.lastActiveDate
        }
    }

    suspend fun setOnboarded(onboarded: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDED] = onboarded }
    }
    
    suspend fun updateTheme(theme: String) {
        context.dataStore.edit { it[KEY_THEME] = theme }
    }

    suspend fun updateUnits(units: String) {
        context.dataStore.edit { it[KEY_UNITS] = units }
    }

    suspend fun updateReminders(meal: Boolean, water: Boolean, summary: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MEAL_REMINDER] = meal
            prefs[KEY_WATER_REMINDER] = water
            prefs[KEY_DAILY_SUMMARY] = summary
        }
    }

    suspend fun incrementStreak(today: String) {
        context.dataStore.edit { prefs ->
            val lastActive = prefs[KEY_LAST_ACTIVE_DATE] ?: ""
            if (lastActive != today) {
                val currentStreak = prefs[KEY_STREAK] ?: 5
                prefs[KEY_STREAK] = currentStreak + 1
                prefs[KEY_LAST_ACTIVE_DATE] = today
            }
        }
    }
}

data class UserProfile(
    val isOnboarded: Boolean = false,
    val name: String = "",
    val age: Int = 25,
    val sex: String = "Prefer not to say",
    val height: Double = 170.0,
    val heightUnit: String = "cm",
    val weight: Double = 70.0,
    val weightUnit: String = "kg",
    val goalWeight: Double = 65.0,
    val goalType: String = "Maintain",
    val pace: String = "Moderate",
    val activityLevel: String = "Moderately active",
    val dietaryPrefs: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val calorieGoal: Int = 2000,
    val proteinGoal: Double = 130.0,
    val carbsGoal: Double = 220.0,
    val fatGoal: Double = 65.0,
    val units: String = "METRIC",
    val theme: String = "SYSTEM",
    val mealReminder: Boolean = true,
    val waterReminder: Boolean = true,
    val dailySummary: Boolean = true,
    val streak: Int = 5,
    val lastActiveDate: String = ""
) {
    fun calculateGoals(): CalculatedGoals {
        val wtKg = (if (weightUnit == "lbs") weight * 0.45359237 else weight).toInt()
        val htCm = (if (heightUnit == "ft") height * 30.48 else height).toInt()
        val ageVal = age

        val isMale = sex == "Male"

        val actLvl = when (activityLevel) {
            "Sedentary" -> ActivityLevel.SEDENTARY
            "Lightly active" -> ActivityLevel.LIGHTLY_ACTIVE
            "Moderately active" -> ActivityLevel.MODERATELY_ACTIVE
            "Very active" -> ActivityLevel.VERY_ACTIVE
            "Athlete" -> ActivityLevel.ATHLETE
            else -> ActivityLevel.MODERATELY_ACTIVE
        }

        val gType = when (goalType) {
            "Lose weight" -> GoalType.LOSE_WEIGHT
            "Gain muscle" -> GoalType.GAIN_MUSCLE
            else -> GoalType.MAINTAIN
        }

        val p = when (pace) {
            "Slow" -> Pace.SLOW
            "Fast" -> Pace.FAST
            else -> Pace.MODERATE
        }

        val calorieTarget = calculateDailyCalorieTarget(
            weightKg = wtKg,
            heightCm = htCm,
            age = ageVal,
            isMale = isMale,
            activityLevel = actLvl,
            goalType = gType,
            pace = p
        )

        // BMR and TDEE calculation for UI representation
        val bmr = if (isMale) {
            (10.0 * wtKg) + (6.25 * htCm) - (5.0 * ageVal) + 5.0
        } else {
            (10.0 * wtKg) + (6.25 * htCm) - (5.0 * ageVal) - 161.0
        }

        val activityMultiplier = when (actLvl) {
            ActivityLevel.SEDENTARY         -> 1.2
            ActivityLevel.LIGHTLY_ACTIVE    -> 1.375
            ActivityLevel.MODERATELY_ACTIVE -> 1.55
            ActivityLevel.VERY_ACTIVE       -> 1.725
            ActivityLevel.ATHLETE           -> 1.9
        }
        val tdee = bmr * activityMultiplier

        // Macros allocation (30% Protein, 45% Carbs, 25% Fat)
        val proteinG = (calorieTarget * 0.3) / 4.0
        val carbsG = (calorieTarget * 0.45) / 4.0
        val fatG = (calorieTarget * 0.25) / 9.0

        return CalculatedGoals(
            bmr = bmr.toInt(),
            tdee = tdee.toInt(),
            calorieTarget = calorieTarget,
            proteinG = Math.round(proteinG * 10) / 10.0,
            carbsG = Math.round(carbsG * 10) / 10.0,
            fatG = Math.round(fatG * 10) / 10.0
        )
    }
}

enum class ActivityLevel {
    SEDENTARY, LIGHTLY_ACTIVE, MODERATELY_ACTIVE, VERY_ACTIVE, ATHLETE
}

enum class GoalType {
    LOSE_WEIGHT, MAINTAIN, GAIN_MUSCLE
}

enum class Pace {
    SLOW, MODERATE, FAST
}

fun calculateDailyCalorieTarget(
    weightKg: Int,
    heightCm: Int,
    age: Int,
    isMale: Boolean,
    activityLevel: ActivityLevel,
    goalType: GoalType,
    pace: Pace
): Int {

    // Step 1: BMR using Mifflin-St Jeor formula
    val bmr = if (isMale) {
        (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age) + 5.0
    } else {
        (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age) - 161.0
    }

    // Step 2: TDEE = BMR × activity multiplier
    val activityMultiplier = when (activityLevel) {
        ActivityLevel.SEDENTARY         -> 1.2
        ActivityLevel.LIGHTLY_ACTIVE    -> 1.375
        ActivityLevel.MODERATELY_ACTIVE -> 1.55
        ActivityLevel.VERY_ACTIVE       -> 1.725
        ActivityLevel.ATHLETE           -> 1.9
    }
    val tdee = bmr * activityMultiplier

    // Step 3: Adjust for goal and pace
    // Pace = how many kg per week the user wants to change
    // 1 kg of body fat ≈ 7,700 kcal, so per day:
    // 0.25 kg/week → 275 kcal/day deficit or surplus
    // 0.5  kg/week → 550 kcal/day
    // 0.75 kg/week → 825 kcal/day
    val dailyAdjustment = when (pace) {
        Pace.SLOW     -> 275.0
        Pace.MODERATE -> 550.0
        Pace.FAST     -> 825.0
    }

    val dailyTarget = when (goalType) {
        GoalType.LOSE_WEIGHT -> tdee - dailyAdjustment
        GoalType.MAINTAIN    -> tdee
        GoalType.GAIN_MUSCLE -> tdee + dailyAdjustment
    }

    // Step 4: Safety clamp — never go below 1,200 kcal (dangerous)
    return dailyTarget.coerceAtLeast(1200.0).toInt()
}

data class CalculatedGoals(
    val bmr: Int,
    val tdee: Int,
    val calorieTarget: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double
)
