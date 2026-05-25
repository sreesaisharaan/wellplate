package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.api.FoodScanResult
import com.example.data.api.GeminiClient
import com.example.data.api.RestaurantDish
import com.example.data.repository.FoodLookupRepository
import com.example.data.repository.FoodLookupResult
import com.example.data.repository.MealTemplateRepository
import com.example.ui.templates.MealTemplateViewModel
import com.example.data.model.toFoodScanResult
import com.example.data.model.DataSource
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.barcode.BarcodeScanning
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.example.data.local.db.MealLogEntity
import com.example.data.local.db.ShoppingIngredientEntity
import com.example.data.local.db.WaterLogEntity
import com.example.data.local.db.WeightLogEntity
import com.example.data.local.db.WellplateDatabase
import com.example.data.local.pref.UserProfile
import com.example.data.local.pref.UserSettingsPreferences
import com.example.data.api.SupabaseClient
import com.example.data.local.pref.SessionManager
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class Screen {
    object Splash : Screen()
    object SignUp : Screen()
    object Login : Screen()
    object ForgotPassword : Screen()
    object Onboarding : Screen()
    object Dashboard : Screen()
    object Scanner : Screen()
    object MealDiary : Screen()
    object SearchAdd : Screen()
    object ProgressInsights : Screen()
    object ProfileSettings : Screen()
    object RestaurantMode : Screen()
    object MealTemplates : Screen()
}

sealed interface RestaurantState {
    object Idle : RestaurantState
    object Loading : RestaurantState
    data class Success(val dishes: List<RestaurantDish>) : RestaurantState
    data class Error(val message: String) : RestaurantState
}

class WellplateViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application.applicationContext,
        WellplateDatabase::class.java,
        "wellplate_db"
    ).fallbackToDestructiveMigration().build()

    private val dao = db.dao()
    private val repository = FoodLookupRepository(dao = dao)
    private val prefs = UserSettingsPreferences(application.applicationContext)

    val mealTemplateViewModel = MealTemplateViewModel(
        MealTemplateRepository(db.templateDao(), db.dao())
    )

    // Reactive states
    val userProfile = prefs.userProfileFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserProfile()
    )

    val currentScreen = MutableStateFlow<Screen>(Screen.Splash)
    val selectedDate = MutableStateFlow(getCurrentDateString())

    private val sessionManager = SessionManager(application.applicationContext)
    
    // Auth States
    val authLoading = MutableStateFlow(false)
    val authError = MutableStateFlow<String?>(null)
    val authSuccess = MutableStateFlow(false)

    init {
        checkSessionAndNavigate()
    }

    private fun checkSessionAndNavigate() {
        viewModelScope.launch {
            try {
                // Check local session
                val localSession = try {
                    sessionManager.isLoggedIn()
                } catch (t: Throwable) {
                    Log.e("WellplateViewModel", "Failed to check local session", t)
                    false
                }

                // Check supabase session ONLY if we are configured and localSession is true
                val supabaseSession = if (localSession && SupabaseClient.isConfigured()) {
                    try {
                        SupabaseClient.supabase.auth.currentSessionOrNull() != null
                    } catch (t: Throwable) {
                        Log.e("WellplateViewModel", "Failed to check Supabase session", t)
                        false
                    }
                } else {
                    false
                }

                // If Supabase is configured, we require BOTH local and Supabase session to navigate to Dashboard directly.
                // Otherwise, if Supabase is NOT configured, having localSession is sufficient to go to Dashboard!
                if (localSession) {
                    if (SupabaseClient.isConfigured()) {
                        if (supabaseSession) {
                            currentScreen.value = Screen.Dashboard
                        } else {
                            currentScreen.value = Screen.Splash
                        }
                    } else {
                        currentScreen.value = Screen.Dashboard
                    }
                } else {
                    currentScreen.value = Screen.Splash
                }
            } catch (t: Throwable) {
                Log.e("WellplateViewModel", "Critical error in checkSessionAndNavigate", t)
                currentScreen.value = Screen.Splash
            }
        }
    }

    fun signUp(name: String, email: String, pword: String) {
        viewModelScope.launch {
            authLoading.value = true
            authError.value = null
            try {
                // Call Supabase signUp if configured
                if (SupabaseClient.isConfigured()) {
                    SupabaseClient.supabase.auth.signUpWith(Email) {
                        this.email = email
                        this.password = pword
                    }
                } else {
                    Log.w("WellplateViewModel", "Supabase not configured, signing up in offline-only simulation.")
                }
                
                // Save session local
                sessionManager.saveSession(email = email, name = name, hasOnboarded = false)
                saveUserProfile(UserProfile(name = name, isOnboarded = false), navigateToHome = false)
                
                authSuccess.value = true
                currentScreen.value = Screen.Onboarding
            } catch (t: Throwable) {
                Log.e("WellplateViewModel", "SignUp Error", t)
                authError.value = mapAuthError(t.message ?: "Unknown error")
            } finally {
                authLoading.value = false
            }
        }
    }

    fun login(email: String, pword: String) {
        viewModelScope.launch {
            authLoading.value = true
            authError.value = null
            try {
                // Call Supabase signIn if configured
                var resolvedName = "User"
                if (SupabaseClient.isConfigured()) {
                    SupabaseClient.supabase.auth.signInWith(Email) {
                        this.email = email
                        this.password = pword
                    }
                    val user = SupabaseClient.supabase.auth.currentUserOrNull()
                    resolvedName = user?.userMetadata?.get("name")?.toString() ?: user?.email?.substringBefore("@") ?: "User"
                } else {
                    Log.w("WellplateViewModel", "Supabase not configured, logging in in offline-only simulation.")
                    resolvedName = email.substringBefore("@")
                }
                
                sessionManager.saveSession(email = email, name = resolvedName, hasOnboarded = true)
                saveUserProfile(userProfile.value.copy(name = resolvedName, isOnboarded = true), navigateToHome = false)
                
                authSuccess.value = true
                currentScreen.value = Screen.Dashboard
            } catch (t: Throwable) {
                Log.e("WellplateViewModel", "Login Error", t)
                authError.value = mapAuthError(t.message ?: "Unknown error")
            } finally {
                authLoading.value = false
            }
        }
    }

    fun forgotPassword(email: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            authLoading.value = true
            authError.value = null
            try {
                if (SupabaseClient.isConfigured()) {
                    SupabaseClient.supabase.auth.resetPasswordForEmail(email)
                }
                onSuccess()
            } catch (t: Throwable) {
                Log.e("WellplateViewModel", "ForgotPassword Error", t)
                authError.value = mapAuthError(t.message ?: "Unknown error")
            } finally {
                authLoading.value = false
            }
        }
    }

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                sessionManager.clearSession()
                prefs.setOnboarded(false)
                if (SupabaseClient.isConfigured()) {
                    try {
                        SupabaseClient.supabase.auth.signOut()
                    } catch (t: Throwable) {
                        Log.e("WellplateViewModel", "Supabase signOut failed", t)
                    }
                }
            } catch (t: Throwable) {
                Log.e("WellplateViewModel", "Logout Error", t)
            } finally {
                onComplete()
            }
        }
    }

    private fun mapAuthError(error: String): String {
        return when {
            error.contains("User already registered", ignoreCase = true) || error.contains("already exists", ignoreCase = true) -> 
                "An account with this email already exists. Try logging in."
            error.contains("Password should be at least", ignoreCase = true) || error.contains("weak", ignoreCase = true) -> 
                "Password must be at least 8 characters."
            error.contains("Unable to validate email", ignoreCase = true) || error.contains("invalid email", ignoreCase = true) -> 
                "Please enter a valid email address."
            error.contains("Invalid login credentials", ignoreCase = true) || error.contains("wrong", ignoreCase = true) -> 
                "Wrong email or password. Please try again."
            error.contains("Email not confirmed", ignoreCase = true) || error.contains("confirm", ignoreCase = true) -> 
                "Please check your email and confirm your account first."
            error.contains("Network", ignoreCase = true) || error.contains("connect", ignoreCase = true) || error.contains("timeout", ignoreCase = true) -> 
                "No internet connection. Please check your network."
            else -> "Something went wrong. Please try again."
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val mealsForSelectedDate = selectedDate.flatMapLatest { date ->
        dao.getMealsForDate(date)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val waterLogsForSelectedDate = selectedDate.flatMapLatest { date ->
        dao.getWaterLogsForDate(date)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allMealsFlow = dao.getAllMeals().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val weightHistory = dao.getWeightHistory().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val shoppingList = dao.getShoppingList().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Temp variables for UI operations
    val scannerLoading = MutableStateFlow(false)
    val scannerError = MutableStateFlow<String?>(null)
    val lastScannedFoodResult = MutableStateFlow<FoodScanResult?>(null)
    val lastSearchedCustomFoods = MutableStateFlow<List<FoodScanResult>>(emptyList())
    val voiceLogResponse = MutableStateFlow<List<FoodScanResult>?>(null)

    val menuRestaurantName = MutableStateFlow("")
    val isRestaurantLoading = MutableStateFlow(false)
    val restaurantDishes = MutableStateFlow<List<RestaurantDish>>(emptyList())
    val restaurantState = MutableStateFlow<RestaurantState>(RestaurantState.Idle)

    val isVoiceLogging = MutableStateFlow(false)
    val smartSuggestions = MutableStateFlow<List<String>>(emptyList())
    val personalAiTip = MutableStateFlow("🔥 Keep tracking your nutrients to complete your daily macro targets. Balance protein and dietary fiber for optimal gut health!")

    // Supabase Cloud Synchronization states
    val supabaseSyncStatus = MutableStateFlow<String>("Idle")
    val supabaseLastSyncedTime = MutableStateFlow<String>("Never")

    init {
        // Trigger initial active dates checking and personal advice loading
        viewModelScope.launch {
            try {
                val dateStr = getCurrentDateString()
                prefs.incrementStreak(dateStr)
            } catch (t: Throwable) {
                Log.e("WellplateViewModel", "Failed to auto-increment streak", t)
            }

            try {
                userProfile.collectLatest { profile ->
                    if (profile.name.isNotEmpty()) {
                        loadPersonalTip()
                    }
                }
            } catch (t: Throwable) {
                Log.e("WellplateViewModel", "Failed to subscribe/load personal tip", t)
            }
        }
    }

    fun getCurrentDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun loadPersonalTip() {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = userProfile.value
            val recentMeals = mealsForSelectedDate.value.take(4).joinToString(", ") { it.foodName }
            val goals = profile.calculateGoals()
            val tip = GeminiClient.generateAiPersonalTip(
                userName = profile.name,
                bmr = goals.bmr,
                tdee = goals.tdee,
                recentFoods = recentMeals
            )
            personalAiTip.value = tip
        }
    }

    fun saveUserProfile(profile: UserProfile, navigateToHome: Boolean = true) {
        viewModelScope.launch {
            // Apply MSJ formulation to calculate goals and store
            val calculated = profile.calculateGoals()
            val calorieTarget = calculated.calorieTarget
            if (calorieTarget < 1200 || calorieTarget > 6000) {
                // Something went wrong with inputs — show error and ask user to re-enter details
                android.widget.Toast.makeText(
                    getApplication(),
                    "Could not calculate your calorie target. Please check your details.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            val updatedProfile = profile.copy(
                calorieGoal = calculated.calorieTarget,
                proteinGoal = calculated.proteinG,
                carbsGoal = calculated.carbsG,
                fatGoal = calculated.fatG
            )
            prefs.saveProfile(updatedProfile)
            if (navigateToHome) {
                currentScreen.value = Screen.Onboarding // Goes to onboarding results step then home
            }
        }
    }

    fun setOnboarded(onboarded: Boolean) {
        viewModelScope.launch {
            prefs.setOnboarded(onboarded)
            currentScreen.value = if (onboarded) Screen.Scanner else Screen.Splash
        }
    }

    // Database Log Operations
    fun logMeal(
        mealType: String,
        foodName: String,
        calories: Int,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        fibreG: Double = 0.0,
        sugarG: Double = 0.0,
        sodiumMg: Double = 0.0,
        confidence: Int = 100,
        servingSizeG: Double = 100.0,
        multiplier: Double = 1.0
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val meal = MealLogEntity(
                date = selectedDate.value,
                mealType = mealType,
                foodName = foodName,
                calories = (calories * multiplier).toInt(),
                proteinG = proteinG * multiplier,
                carbsG = carbsG * multiplier,
                fatG = fatG * multiplier,
                fibreG = fibreG * multiplier,
                sugarG = sugarG * multiplier,
                sodiumMg = sodiumMg * multiplier,
                confidencePercent = confidence,
                servingSizeG = servingSizeG,
                quantityMultiplier = multiplier
            )
            dao.insertMeal(meal)
            loadPersonalTip()
            triggerSmartSuggestions(foodName)
        }
    }

    fun deleteMeal(meal: MealLogEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteMeal(meal)
            loadPersonalTip()
        }
    }

    fun logWater(amountMl: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val water = WaterLogEntity(
                date = selectedDate.value,
                amountMl = amountMl
            )
            dao.insertWater(water)
        }
    }

    fun logWeight(weightKg: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            // Save inside database as history
            val weight = WeightLogEntity(
                date = selectedDate.value,
                weightKg = weightKg
            )
            dao.insertWeight(weight)
            
            // Also update current active weight inside DataStore settings
            val currentProfile = userProfile.value
            prefs.saveProfile(currentProfile.copy(weight = weightKg))
        }
    }

    // Shopping List Operations
    fun addIngredientToShoppingList(ingredientName: String, associatedFood: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertShoppingIngredient(
                ShoppingIngredientEntity(
                    ingredientName = ingredientName,
                    associatedFoodName = associatedFood
                )
            )
        }
    }

    fun parseWholeIngredientsToShopping(foodName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Smart simple parsing or manual heuristics for common plates
            val ingredients = when(foodName.lowercase()) {
                "salad" -> listOf("Lettuce", "Cherry Tomatoes", "Cucumber", "Olive Oil")
                "oatmeal" -> listOf("Rolled Oats", "Almonds", "Soy Milk", "Blueberries")
                "boiled eggs" -> listOf("Eggs")
                "sandwich" -> listOf("Whole Wheat Bread", "Turkey Breast Slice", "Lettuce", "Tomato")
                "chicken breast" -> listOf("Chicken Breast", "Olive oil", "Lemon")
                "masala dosa" -> listOf("Rice batter", "Potato", "Onion", "Mustard seeds")
                "pasta" -> listOf("Penne Pasta", "Marinara Sauce", "Parmesan Cheese")
                "smoothie" -> listOf("Banana", "Strawberry", "Greek Yogurt", "Spinach")
                else -> listOf(foodName)
            }
            ingredients.forEach { name ->
                dao.insertShoppingIngredient(
                    ShoppingIngredientEntity(
                        ingredientName = name,
                        associatedFoodName = foodName
                    )
                )
            }
        }
    }

    fun toggleShoppingItem(item: ShoppingIngredientEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateShoppingIngredient(item.copy(isChecked = !item.isChecked))
        }
    }

    fun deleteShoppingItem(item: ShoppingIngredientEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteShoppingIngredient(item)
        }
    }

    fun clearShoppingList() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearShoppingList()
        }
    }

    // AI Operations
    private suspend fun scanBarcodeFromBitmap(bitmap: Bitmap): String? = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val detected = barcodes.firstOrNull()?.rawValue
                    continuation.resume(detected)
                }
                .addOnFailureListener { e ->
                    Log.e("WellplateViewModel", "Barcode scanning failed", e)
                    continuation.resume(null)
                }
        } catch (e: Exception) {
            Log.e("WellplateViewModel", "Error setting up barcode scan", e)
            continuation.resume(null)
        }
    }

    fun scanCameraImage(bitmap: Bitmap, isBarcode: Boolean) {
        viewModelScope.launch {
            scannerLoading.value = true
            scannerError.value = null
            lastScannedFoodResult.value = null

            try {
                if (isBarcode) {
                    val barcodeStr = scanBarcodeFromBitmap(bitmap)
                    if (barcodeStr.isNullOrBlank()) {
                        scannerError.value = "No barcode visible. Try closer or in better light!"
                        return@launch
                    }

                    // Query repository by barcode
                    val verifiedNutrition = repository.lookupByBarcode(barcodeStr)
                    if (verifiedNutrition != null) {
                        lastScannedFoodResult.value = verifiedNutrition.toFoodScanResult()
                    } else {
                        // Let Gemini estimate the name/nutrition based on barcode & overall product packaging image
                        try {
                            val geminiResult = GeminiClient.scanFoodWithImage(bitmap, isBarcodeScan = true)
                            lastScannedFoodResult.value = geminiResult.copy(source = "GEMINI_ESTIMATE")
                        } catch (e: Exception) {
                            scannerError.value = "Barcode $barcodeStr not recognized. Gemini estimation failed."
                        }
                    }
                } else {
                    val lookupResult = repository.lookupFromImage(bitmap)
                    when (lookupResult) {
                        is FoodLookupResult.Success -> {
                            lastScannedFoodResult.value = lookupResult.nutrition.toFoodScanResult()
                        }
                        is FoodLookupResult.LowConfidence -> {
                            scannerError.value = "Low confidence match: '${lookupResult.suggestedName}'. Try manual search for precision!"
                        }
                        is FoodLookupResult.Error -> {
                            scannerError.value = lookupResult.message
                        }
                    }
                }
            } catch (e: Exception) {
                scannerError.value = "Hmm, I couldn't identify that. Try better lighting or search manually."
                Log.e("WellplateViewModel", "Camera scan failed: ${e.message}", e)
            } finally {
                scannerLoading.value = false
            }
        }
    }

    fun performManualSearch(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                lastSearchedCustomFoods.value = emptyList()
                return@launch
            }
            try {
                val nutritionList = repository.searchFoods(query)
                lastSearchedCustomFoods.value = nutritionList.map { it.toFoodScanResult() }
            } catch (e: Exception) {
                Log.e("WellplateViewModel", "Manual search failed: ${e.message}", e)
                lastSearchedCustomFoods.value = emptyList()
            }
        }
    }

    fun performVoiceLogging(msg: String) {
        viewModelScope.launch {
            isVoiceLogging.value = true
            voiceLogResponse.value = null
            try {
                val results = GeminiClient.queryVoiceFoodLog(msg)
                voiceLogResponse.value = results
            } catch (e: Exception) {
                Log.e("WellplateViewModel", "Voice decoding failure: ${e.message}", e)
            } finally {
                isVoiceLogging.value = false
            }
        }
    }

    fun runRestaurantQuery(name: String) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch
            isRestaurantLoading.value = true
            restaurantState.value = RestaurantState.Loading
            try {
                val dishes = GeminiClient.getRestaurantNutritionSuggestions(name)
                restaurantDishes.value = dishes
                if (dishes.isEmpty()) {
                    restaurantState.value = RestaurantState.Error("No recommendations found for '$name'. Try another popular venue.")
                } else {
                    restaurantState.value = RestaurantState.Success(dishes)
                }
            } catch (e: Exception) {
                Log.e("WellplateViewModel", "Restaurant lookup error", e)
                restaurantState.value = RestaurantState.Error(e.message ?: "Failed to lookup restaurant nutrition guides.")
            } finally {
                isRestaurantLoading.value = false
            }
        }
    }

    private fun triggerSmartSuggestions(foodIdentified: String) {
        viewModelScope.launch {
            val recent = mealsForSelectedDate.value.take(3).joinToString(", ") { it.foodName }
            val suggestions = GeminiClient.getSmartMealSuggestions(recent, foodIdentified)
            smartSuggestions.value = suggestions
        }
    }

    // Calculate overall dynamic wellplate score (0 to 100)
    fun calculateDailyWellplateScore(): Int {
        val meals = mealsForSelectedDate.value
        val profile = userProfile.value
        if (meals.isEmpty()) return 0

        val totalLoggedCalories = meals.sumOf { it.calories }
        val calorieGoal = profile.calorieGoal

        // 1. Calorie achievement score (up to 40 pts)
        val calorieRatio = if (calorieGoal > 0) totalLoggedCalories.toDouble() / calorieGoal else 0.0
        val calorieScore = if (calorieRatio in 0.85..1.15) {
            40
        } else {
            // Smoothly decrease score if outside target range
            (40 - Math.abs(calorieRatio - 1.0) * 80).toInt().coerceAtLeast(10)
        }

        // 2. Water score (up to 20 pts)
        val waterLogSum = waterLogsForSelectedDate.value.sumOf { it.amountMl }
        // Let's assume water target is 2000ml (8 glasses)
        val waterRatio = waterLogSum.toDouble() / 2000.0
        val waterScore = (waterRatio * 20).toInt().coerceIn(0, 20)

        // 3. Macro split balance score (up to 30 pts)
        val totalProtein = meals.sumOf { it.proteinG }
        val totalCarbs = meals.sumOf { it.carbsG }
        val totalFat = meals.sumOf { it.fatG }
        val totalGrams = totalProtein + totalCarbs + totalFat

        val macroScore = if (totalGrams > 0) {
            // Target percentages: P: 25-35%, C: 40-50%, F: 20-30%
            val pPct = totalProtein / totalGrams
            val cPct = totalCarbs / totalGrams
            val fPct = totalFat / totalGrams

            var balancePts = 30
            if (pPct !in 0.20..0.40) balancePts -= 10
            if (cPct !in 0.35..0.55) balancePts -= 10
            if (fPct !in 0.15..0.35) balancePts -= 10
            balancePts.coerceAtLeast(10)
        } else {
            0
        }

        // 4. Regularity reward (up to 10 points)
        // Check how many meal categories are logged
        val loggedTypesStr = meals.map { it.mealType }.distinct()
        val regularityScore = (loggedTypesStr.size * 2.5).toInt().coerceAtLeast(2)

        return (calorieScore + waterScore + macroScore + regularityScore).coerceIn(0, 100)
    }

    // Edit Settings Toggles
    fun updateUnitsSetting(units: String) {
        viewModelScope.launch {
            prefs.updateUnits(units)
        }
    }

    fun updateReminders(meal: Boolean, water: Boolean, summary: Boolean) {
        viewModelScope.launch {
            prefs.updateReminders(meal, water, summary)
        }
    }

    fun updateTheme(themeStr: String) {
        viewModelScope.launch {
            prefs.updateTheme(themeStr)
        }
    }

    // —————————————————————————————————————————————
    // Supabase Sync Operations
    // —————————————————————————————————————————————
    fun syncWithSupabase() {
        supabaseSyncStatus.value = "Syncing..."
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!SupabaseClient.isConfigured()) {
                    supabaseSyncStatus.value = "Error: Supabase parameters are missing or default placeholders."
                    return@launch
                }

                val meals = dao.getAllMealsSync()
                val water = dao.getAllWaterLogsSync()
                val weights = dao.getAllWeightLogsSync()
                val shopping = dao.getAllShoppingListSync()

                val success = SupabaseClient.syncLocalToRemote(meals, water, weights, shopping)
                if (success) {
                    supabaseSyncStatus.value = "Cloud Sync Success"
                    val currentTime = SimpleDateFormat("HH:mm:ss (MMM dd)", Locale.getDefault()).format(Date())
                    supabaseLastSyncedTime.value = currentTime
                } else {
                    supabaseSyncStatus.value = "Cloud Sync Failed"
                }
            } catch (t: Throwable) {
                Log.e("WellplateViewModel", "Supabase sync failed", t)
                supabaseSyncStatus.value = "Sync Exception: ${t.message}"
            }
        }
    }

    fun restoreFromSupabase() {
        supabaseSyncStatus.value = "Restoring..."
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!SupabaseClient.isConfigured()) {
                    supabaseSyncStatus.value = "Error: Supabase parameters are missing or default placeholders."
                    return@launch
                }

                val remoteMeals = SupabaseClient.fetchRemoteMeals()
                val remoteWater = SupabaseClient.fetchRemoteWater()
                val remoteWeight = SupabaseClient.fetchRemoteWeight()
                val remoteShopping = SupabaseClient.fetchRemoteShopping()

                if (remoteMeals.isNotEmpty()) {
                    val entities = remoteMeals.map { m ->
                        MealLogEntity(
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
                    dao.insertMeals(entities)
                }

                if (remoteWater.isNotEmpty()) {
                    val entities = remoteWater.map { w ->
                        WaterLogEntity(
                            date = w.date,
                            amountMl = w.amountMl,
                            timestamp = w.timestamp
                        )
                    }
                    dao.insertWaterLogs(entities)
                }

                if (remoteWeight.isNotEmpty()) {
                    val entities = remoteWeight.map { w ->
                        WeightLogEntity(
                            date = w.date,
                            weightKg = w.weightKg,
                            timestamp = w.timestamp
                        )
                    }
                    dao.insertWeightLogs(entities)
                }

                if (remoteShopping.isNotEmpty()) {
                    val entities = remoteShopping.map { item ->
                        ShoppingIngredientEntity(
                            ingredientName = item.ingredientName,
                            isChecked = item.isChecked,
                            associatedFoodName = item.associatedFoodName,
                            timestamp = item.timestamp
                        )
                    }
                    dao.insertShoppingList(entities)
                }

                supabaseSyncStatus.value = "Cloud Restore Complete"
                val currentTime = SimpleDateFormat("HH:mm:ss (MMM dd)", Locale.getDefault()).format(Date())
                supabaseLastSyncedTime.value = currentTime
                loadPersonalTip()
            } catch (t: Throwable) {
                Log.e("WellplateViewModel", "Supabase restore failed", t)
                supabaseSyncStatus.value = "Restore Failed: ${t.message}"
            }
        }
    }
}
