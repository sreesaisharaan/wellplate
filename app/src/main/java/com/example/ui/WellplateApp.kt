package com.example.ui

import com.example.ui.theme.*
import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.api.FoodScanResult
import com.example.data.api.RestaurantDish
import com.example.data.model.DataSource
import com.example.ui.components.DataSourceBadge
import com.example.data.local.db.MealLogEntity
import com.example.data.local.db.ShoppingIngredientEntity
import com.example.data.local.db.WaterLogEntity
import com.example.data.local.db.WeightLogEntity
import com.example.data.local.pref.UserProfile
import kotlinx.coroutines.launch
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.camera.core.Preview
import androidx.camera.core.ImageCaptureException
import java.util.concurrent.Executors
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted

@Composable
fun WellplateApp(viewModel: WellplateViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    // Determine starting screen based on onboarding status
    LaunchedEffect(userProfile) {
        if (currentScreen == Screen.Splash) {
            if (userProfile.isOnboarded) {
                viewModel.currentScreen.value = Screen.Dashboard
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentScreen != Screen.Splash && currentScreen != Screen.SignUp && currentScreen != Screen.Login && currentScreen != Screen.ForgotPassword && currentScreen != Screen.Onboarding) {
                WellplateBottomBar(
                    currentScreen = currentScreen,
                    onNavigate = { screen -> viewModel.currentScreen.value = screen }
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    slideInHorizontally { width -> width }.togetherWith(slideOutHorizontally { width -> -width })
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    is Screen.Splash -> WelcomeScreen(
                        onGetStarted = { viewModel.currentScreen.value = Screen.SignUp },
                        onHaveAccount = { viewModel.currentScreen.value = Screen.Login }
                    )
                    is Screen.SignUp -> SignUpScreen(viewModel = viewModel)
                    is Screen.Login -> LoginScreen(viewModel = viewModel)
                    is Screen.ForgotPassword -> ForgotPasswordScreen(viewModel = viewModel)
                    is Screen.Onboarding -> OnboardingScreen(viewModel = viewModel)
                    is Screen.Dashboard -> DashboardScreen(viewModel = viewModel)
                    is Screen.Scanner -> FoodScannerScreen(viewModel = viewModel)
                    is Screen.MealDiary -> MealDiaryScreen(viewModel = viewModel)
                    is Screen.SearchAdd -> SearchAddScreen(viewModel = viewModel)
                    is Screen.ProgressInsights -> ProgressInsightsScreen(viewModel = viewModel)
                    is Screen.ProfileSettings -> ProfileSettingsScreen(viewModel = viewModel)
                    is Screen.RestaurantMode -> RestaurantSuggestionsScreen(viewModel = viewModel)
                    is Screen.MealTemplates -> {
                        com.example.ui.templates.MealTemplatesScreen(
                            viewModel = viewModel.mealTemplateViewModel,
                            selectedDate = viewModel.selectedDate.value,
                            onBack = { viewModel.currentScreen.value = Screen.MealDiary }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WellplateOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: androidx.compose.ui.text.TextStyle = androidx.compose.material3.LocalTextStyle.current.copy(color = WellplateTextPrimary),
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        interactionSource = interactionSource,
        shape = shape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = WellplateTextPrimary,
            unfocusedTextColor = WellplateTextPrimary,
            disabledTextColor = WellplateTextHint,
            errorTextColor = Color.Red,
            focusedContainerColor = WellplateInputBg,
            unfocusedContainerColor = WellplateInputBg,
            disabledContainerColor = WellplateInputBg,
            errorContainerColor = WellplateInputBg,
            focusedBorderColor = WellplateGreen,
            unfocusedBorderColor = WellplateInputBorder,
            disabledBorderColor = WellplateInputBorder,
            errorBorderColor = Color.Red,
            focusedLabelColor = WellplateTextBody,
            unfocusedLabelColor = WellplateTextBody,
            focusedPlaceholderColor = WellplateTextHint,
            unfocusedPlaceholderColor = WellplateTextHint
        )
    )
}

@Composable
fun WellplateBottomBar(currentScreen: Screen, onNavigate: (Screen) -> Unit) {
    Column {
        HorizontalDivider(
            thickness = 1.dp,
            color = WellplateBorder
        )
        NavigationBar(
            containerColor = WellplateSurface,
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                selected = currentScreen is Screen.Dashboard,
                onClick = { onNavigate(Screen.Dashboard) },
                icon = { Icon(if (currentScreen is Screen.Dashboard) Icons.Filled.Dashboard else Icons.Outlined.Dashboard, contentDescription = "Dashboard") },
                label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = WellplateGreen,
                    selectedTextColor = WellplateGreen,
                    indicatorColor = WellplateGreenLight,
                    unselectedIconColor = WellplateTextHint,
                    unselectedTextColor = WellplateTextHint
                )
            )
            NavigationBarItem(
                selected = currentScreen is Screen.MealDiary,
                onClick = { onNavigate(Screen.MealDiary) },
                icon = { Icon(if (currentScreen is Screen.MealDiary) Icons.Filled.Book else Icons.Outlined.Book, contentDescription = "Diary") },
                label = { Text("Diary", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = WellplateGreen,
                    selectedTextColor = WellplateGreen,
                    indicatorColor = WellplateGreenLight,
                    unselectedIconColor = WellplateTextHint,
                    unselectedTextColor = WellplateTextHint
                )
            )
            NavigationBarItem(
                selected = currentScreen is Screen.Scanner,
                onClick = { onNavigate(Screen.Scanner) },
                icon = { Icon(if (currentScreen is Screen.Scanner) Icons.Filled.QrCodeScanner else Icons.Outlined.QrCodeScanner, contentDescription = "Scan") },
                label = { Text("Log AI", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = WellplateGreen,
                    selectedTextColor = WellplateGreen,
                    indicatorColor = WellplateGreenLight,
                    unselectedIconColor = WellplateTextHint,
                    unselectedTextColor = WellplateTextHint
                )
            )
            NavigationBarItem(
                selected = currentScreen is Screen.ProgressInsights,
                onClick = { onNavigate(Screen.ProgressInsights) },
                icon = { Icon(if (currentScreen is Screen.ProgressInsights) Icons.Filled.TrendingUp else Icons.Outlined.TrendingUp, contentDescription = "Progress") },
                label = { Text("Charts", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = WellplateGreen,
                    selectedTextColor = WellplateGreen,
                    indicatorColor = WellplateGreenLight,
                    unselectedIconColor = WellplateTextHint,
                    unselectedTextColor = WellplateTextHint
                )
            )
            NavigationBarItem(
                selected = currentScreen is Screen.ProfileSettings,
                onClick = { onNavigate(Screen.ProfileSettings) },
                icon = { Icon(if (currentScreen is Screen.ProfileSettings) Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "Profile") },
                label = { Text("Profile", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = WellplateGreen,
                    selectedTextColor = WellplateGreen,
                    indicatorColor = WellplateGreenLight,
                    unselectedIconColor = WellplateTextHint,
                    unselectedTextColor = WellplateTextHint
                )
            )
        }
    }
}

// ──────────────────────────────────────────────
// SCREEN 1 — SPLASH / WELCOME
// ──────────────────────────────────────────────
@Composable
fun WelcomeScreen(onGetStarted: () -> Unit, onHaveAccount: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(WellplateGreenLight, WellplateBackground)))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(Color.White, shape = CircleShape)
                .border(2.dp, WellplateGreen.copy(alpha = 0.3f), CircleShape)
                .clip(CircleShape)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.wellplate_logo_1779342608858),
                contentDescription = "Wellplate Brand Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Wellplate",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                color = WellplateGreen,
                letterSpacing = 1.sp
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Know what you eat. Own your health.",
            style = MaterialTheme.typography.titleMedium.copy(
                color = WellplateTextBody,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(64.dp))

        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("get_started_btn"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen)
        ) {
            Text("Get Started", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onHaveAccount,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("already_have_account_btn"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = WellplateGreen),
            border = BorderStroke(1.5.dp, WellplateGreen)
        ) {
            Text("I already have an account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ──────────────────────────────────────────────
// SCREENS 2–7 — ONBOARDING FLOW
// ──────────────────────────────────────────────
@Composable
fun OnboardingScreen(viewModel: WellplateViewModel) {
    var step by remember { mutableStateOf(1) }
    val maxSteps = 7

    // Onboarding Form States
    var name by remember { mutableStateOf("") }
    var ageInput by remember { mutableStateOf("") }
    var biologicalSex by remember { mutableStateOf("Male") }
    var heightInput by remember { mutableStateOf("") }
    var heightUnit by remember { mutableStateOf("cm") } // cm or ft
    var weightInput by remember { mutableStateOf("") }
    var weightUnit by remember { mutableStateOf("kg") } // kg or lbs

    var goalWeightInput by remember { mutableStateOf("") }
    var goalType by remember { mutableStateOf("Lose weight") } // Lose weight / Maintain / Gain muscle
    var pace by remember { mutableStateOf("Moderate") } // Slow / Moderate / Fast

    var activityLevel by remember { mutableStateOf("Lightly active") }
    val dietPrefs = remember { mutableStateListOf<String>() }
    val allergies = remember { mutableStateListOf<String>() }

    var hasTriedStep2 by remember { mutableStateOf(false) }
    var hasTriedStep3 by remember { mutableStateOf(false) }

    // Formula calculation local evaluation
    val tempProfile = UserProfile(
        name = name,
        age = ageInput.toIntOrNull() ?: 25,
        sex = biologicalSex,
        height = heightInput.toDoubleOrNull() ?: 170.0,
        heightUnit = heightUnit,
        weight = weightInput.toDoubleOrNull() ?: 70.0,
        weightUnit = weightUnit,
        goalWeight = goalWeightInput.toDoubleOrNull() ?: 65.0,
        goalType = goalType,
        pace = pace,
        activityLevel = activityLevel,
        dietaryPrefs = dietPrefs.toList(),
        allergies = allergies.toList()
    )
    val calculated = remember(step, ageInput, heightInput, weightInput, goalWeightInput, biologicalSex, heightUnit, weightUnit, goalType, pace, activityLevel) { tempProfile.calculateGoals() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WellplateBackground)
            .padding(18.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top step progress bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 1) {
                IconButton(onClick = { step-- }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WellplateGreen)
                }
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }

            Text(
                text = "Step $step of $maxSteps",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = WellplateGreen
                )
            )

            Spacer(modifier = Modifier.size(36.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = { step.toFloat() / maxSteps },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = WellplateGreen,
            trackColor = WellplateGreenLight
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Step Contents
        when (step) {
            1 -> { // Name & profile photo
                Text("Design Your Profile", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium, fontSize = 22.sp, color = WellplateTextPrimary))
                Spacer(modifier = Modifier.height(8.dp))
                Text("What should we call you?", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, color = WellplateTextBody))
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(WellplateGreenLight, CircleShape)
                        .align(Alignment.CenterHorizontally)
                        .border(1.dp, WellplateGreen.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = "Camera Icon", tint = WellplateGreen, modifier = Modifier.size(36.dp))
                }
                Text("Add Profile Photo (Optional)", style = MaterialTheme.typography.bodyMedium.copy(color = WellplateTextHint), modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))

                Spacer(modifier = Modifier.height(32.dp))

                WellplateOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_name_input")
                )
            }
            2 -> { // Basic info
                Text("A Bit About You", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium, fontSize = 22.sp, color = WellplateTextPrimary))
                Spacer(modifier = Modifier.height(20.dp))

                // Age select input field
                Text("Age", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WellplateTextPrimary)
                WellplateOutlinedTextField(
                    value = ageInput,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            ageInput = input
                        }
                    },
                    label = { Text("Age (yrs)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = hasTriedStep2 && (ageInput.isEmpty() || (ageInput.toIntOrNull() ?: 0) !in 10..100)
                )
                if (hasTriedStep2 && (ageInput.isEmpty() || (ageInput.toIntOrNull() ?: 0) !in 10..100)) {
                    Text(
                        text = "Please enter a valid age between 10 and 100",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Biological Sex
                Text("Biological Sex", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WellplateTextPrimary)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val sexOptions = listOf("Male", "Female", "Prefer not to say")
                    sexOptions.forEach { sex ->
                        val selected = biologicalSex == sex
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { biologicalSex = sex },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) WellplateGreenLight else WellplateInputBg
                            ),
                            border = BorderStroke(1.dp, if (selected) WellplateGreen else WellplateInputBorder)
                        ) {
                            Text(
                                text = sex,
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                color = if (selected) WellplateGreenText else WellplateTextBody,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Height Inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Height", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WellplateTextPrimary)
                    // Unit choice toggle widget
                    Row(
                        modifier = Modifier
                            .background(WellplateInputBg, RoundedCornerShape(24.dp))
                            .padding(2.dp)
                    ) {
                        listOf("cm", "ft").forEach { u ->
                            val isSel = heightUnit == u
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSel) WellplateGreen else Color.Transparent)
                                    .clickable { heightUnit = u }
                                    .padding(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Text(u, color = if (isSel) Color.White else WellplateTextBody, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                WellplateOutlinedTextField(
                    value = heightInput,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            heightInput = input
                        }
                    },
                    label = { Text(if (heightUnit == "cm") "Height (cm)" else "Height (ft)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = hasTriedStep2 && (heightInput.isEmpty() || (heightInput.toIntOrNull() ?: 0) !in 50..300)
                )
                if (hasTriedStep2 && (heightInput.isEmpty() || (heightInput.toIntOrNull() ?: 0) !in 50..300)) {
                    Text(
                        text = "Please enter a valid height between 50 and 300 cm",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Current Weight Inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Current Weight", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WellplateTextPrimary)
                    Row(
                        modifier = Modifier
                            .background(WellplateInputBg, RoundedCornerShape(24.dp))
                            .padding(2.dp)
                    ) {
                        listOf("kg", "lbs").forEach { u ->
                            val isSel = weightUnit == u
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSel) WellplateGreen else Color.Transparent)
                                    .clickable { weightUnit = u }
                                    .padding(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Text(u, color = if (isSel) Color.White else WellplateTextBody, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                WellplateOutlinedTextField(
                    value = weightInput,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            weightInput = input
                        }
                    },
                    label = { Text(if (weightUnit == "kg") "Weight (kg)" else "Weight (lbs)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = hasTriedStep2 && (weightInput.isEmpty() || (weightInput.toIntOrNull() ?: 0) !in 20..500)
                )
                if (hasTriedStep2 && (weightInput.isEmpty() || (weightInput.toIntOrNull() ?: 0) !in 20..500)) {
                    Text(
                        text = "Please enter a valid weight between 20 and 500 kg",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
            }
            3 -> { // Goal Weight & Timeline
                Text("Select Your Goal", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium, fontSize = 22.sp, color = WellplateTextPrimary))
                Spacer(modifier = Modifier.height(20.dp))

                Text("What is your Target Weight?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WellplateTextPrimary)
                WellplateOutlinedTextField(
                    value = goalWeightInput,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            goalWeightInput = input
                        }
                    },
                    label = { Text("Goal Weight (${weightUnit})") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = hasTriedStep3 && (goalWeightInput.isEmpty() || (goalWeightInput.toIntOrNull() ?: 0) !in 20..500)
                )
                if (hasTriedStep3 && (goalWeightInput.isEmpty() || (goalWeightInput.toIntOrNull() ?: 0) !in 20..500)) {
                    Text(
                        text = "Please enter a valid goal weight between 20 and 500 kg",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Goal Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WellplateTextPrimary)
                val goals = listOf("Lose weight", "Maintain", "Gain muscle")
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    goals.forEach { g ->
                        val selected = goalType == g
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { goalType = g },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) WellplateGreenLight else WellplateInputBg
                            ),
                            border = BorderStroke(1.5.dp, if (selected) WellplateGreen else WellplateInputBorder)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selected,
                                    onClick = { goalType = g },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = WellplateGreen,
                                        unselectedColor = WellplateTextHint
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(g, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (selected) WellplateGreenText else WellplateTextPrimary)
                            }
                        }
                    }
                }

                if (goalType != "Maintain") {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Pace Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WellplateTextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    val paceOptions = listOf("Slow", "Moderate", "Fast")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        paceOptions.forEach { p ->
                            val selected = pace == p
                            val detail = when (p) {
                                "Slow" -> "0.25 kg/wk"
                                "Moderate" -> "0.50 kg/wk"
                                else -> "0.75 kg/wk"
                            }
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { pace = p },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) WellplateGreenLight else WellplateInputBg
                                ),
                                border = BorderStroke(1.dp, if (selected) WellplateGreen else WellplateInputBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(p, color = if (selected) WellplateGreenText else WellplateTextPrimary, fontWeight = FontWeight.Bold)
                                    Text(detail, fontSize = 10.sp, color = if (selected) WellplateGreenText.copy(alpha = 0.8f) else WellplateTextHint)
                                }
                            }
                        }
                    }
                }
            }
            4 -> { // Activity Level
                Text("Activity Lifestyle", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium, fontSize = 22.sp, color = WellplateTextPrimary))
                Spacer(modifier = Modifier.height(16.dp))

                val activityLevels = listOf(
                    Triple("Sedentary", "Desk work, little or no exercise", Icons.Default.Circle),
                    Triple("Lightly active", "Occasional workouts, light standing work", Icons.Default.DirectionsWalk),
                    Triple("Moderately active", "Gym training 3-5 times/week, active day", Icons.Default.FitnessCenter),
                    Triple("Very active", "Hard training 6-7 times/week, high physical work", Icons.Default.DirectionsRun),
                    Triple("Athlete", "Professional athletic training schedules daily", Icons.Default.SportsBasketball)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activityLevels.forEach { (lev, desc, ico) ->
                        val selected = activityLevel == lev
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activityLevel = lev },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) WellplateGreenLight else WellplateInputBg
                            ),
                            border = BorderStroke(1.5.dp, if (selected) WellplateGreen else WellplateInputBorder)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(ico, contentDescription = null, tint = if (selected) WellplateGreen else WellplateTextHint, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(lev, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (selected) WellplateGreenText else WellplateTextPrimary)
                                    Text(desc, fontSize = 11.sp, color = if (selected) WellplateGreenText.copy(alpha = 0.8f) else WellplateTextBody)
                                }
                            }
                        }
                    }
                }
            }
            5 -> { // Dietary Preferences
                Text("Dietary Habits", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium, fontSize = 22.sp, color = WellplateTextPrimary))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Select any dietary choices", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, color = WellplateTextBody))
                Spacer(modifier = Modifier.height(24.dp))

                val diOptions = listOf("Vegetarian", "Vegan", "Gluten-free", "Dairy-free", "Keto", "Halal", "No restrictions")
                LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(280.dp)) {
                    items(diOptions.size) { index ->
                        val d = diOptions[index]
                        val isSelected = dietPrefs.contains(d)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) {
                                        dietPrefs.remove(d)
                                    } else {
                                        if (d == "No restrictions") {
                                            dietPrefs.clear()
                                            dietPrefs.add(d)
                                        } else {
                                            dietPrefs.remove("No restrictions")
                                            dietPrefs.add(d)
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) WellplateGreenLight else WellplateInputBg
                            ),
                            border = BorderStroke(1.dp, if (isSelected) WellplateGreen else WellplateInputBorder)
                        ) {
                            Text(
                                text = d,
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                color = if (isSelected) WellplateGreenText else WellplateTextBody,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            6 -> { // Allergies
                Text("Any Food Allergies?", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium, fontSize = 22.sp, color = WellplateTextPrimary))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Help us filter ingredients", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, color = WellplateTextBody))
                Spacer(modifier = Modifier.height(24.dp))

                val allergyOptions = listOf("Nuts", "Shellfish", "Eggs", "Soy", "Wheat", "Dairy", "None")
                LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(280.dp)) {
                    items(allergyOptions.size) { index ->
                        val a = allergyOptions[index]
                        val isSelected = allergies.contains(a)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) {
                                        allergies.remove(a)
                                    } else {
                                        if (a == "None") {
                                            allergies.clear()
                                            allergies.add(a)
                                        } else {
                                            allergies.remove("None")
                                            allergies.add(a)
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) WellplateGreenLight else WellplateInputBg
                            ),
                            border = BorderStroke(1.dp, if (isSelected) WellplateGreen else WellplateInputBorder)
                        ) {
                            Text(
                                text = a,
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                color = if (isSelected) WellplateGreenText else WellplateTextBody,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            7 -> { // Results
                Text("Your Nutrition Prescription", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium, fontSize = 22.sp, color = WellplateTextPrimary))
                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WellplateSurface),
                    border = BorderStroke(1.dp, WellplateBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Mifflin-St Jeor Summary", style = MaterialTheme.typography.titleMedium, color = WellplateGreen, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("BMR", fontSize = 12.sp, color = WellplateTextHint)
                                Text("${calculated.bmr} kcal", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WellplateGreen)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("TDEE", fontSize = 12.sp, color = WellplateTextHint)
                                Text("${calculated.tdee} kcal", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WellplateAmber)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = WellplateDivider)

                        Text("Recommended Daily Budget", style = MaterialTheme.typography.bodySmall.copy(color = WellplateTextBody))
                        Text("${calculated.calorieTarget} kcal", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, color = WellplateGreen))

                        Spacer(modifier = Modifier.height(20.dp))

                        // Macro indicators
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MacroBarGroup(label = "Protein", amount = "${calculated.proteinG}g", color = Color(0xFFFF8A65), modifier = Modifier.weight(1f))
                            MacroBarGroup(label = "Carbs", amount = "${calculated.carbsG}g", color = Color(0xFFFFD54F), modifier = Modifier.weight(1f))
                            MacroBarGroup(label = "Fat", amount = "${calculated.fatG}g", color = Color(0xFF4FC3F7), modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 1) {
                OutlinedButton(
                    onClick = { step-- },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WellplateGreen),
                    border = BorderStroke(1.dp, WellplateGreen)
                ) {
                    Text("Previous")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Button(
                onClick = {
                    when (step) {
                        2 -> {
                            hasTriedStep2 = true
                            val isAgeValid = ageInput.isNotEmpty() && (ageInput.toIntOrNull() ?: 0) in 10..100
                            val isHeightValid = heightInput.isNotEmpty() && (heightInput.toIntOrNull() ?: 0) in 50..300
                            val isWeightValid = weightInput.isNotEmpty() && (weightInput.toIntOrNull() ?: 0) in 20..500
                            if (isAgeValid && isHeightValid && isWeightValid) {
                                step++
                            }
                        }
                        3 -> {
                            hasTriedStep3 = true
                            val isGoalValid = goalWeightInput.isNotEmpty() && (goalWeightInput.toIntOrNull() ?: 0) in 20..500
                            if (isGoalValid) {
                                step++
                            }
                        }
                        else -> {
                            if (step < maxSteps) {
                                step++
                            } else {
                                val finalProfile = tempProfile.copy(
                                    isOnboarded = true,
                                    calorieGoal = calculated.calorieTarget,
                                    proteinGoal = calculated.proteinG,
                                    carbsGoal = calculated.carbsG,
                                    fatGoal = calculated.fatG
                                )
                                viewModel.saveUserProfile(finalProfile, navigateToHome = false)
                                viewModel.setOnboarded(true)
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen),
                modifier = Modifier.testTag("onboarding_next_finish_btn")
            ) {
                Text(if (step == maxSteps) "Let's Go!" else "Continue", color = Color.White)
            }
        }
    }
}

@Composable
fun MacroBarGroup(label: String, amount: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = WellplateTextHint, fontWeight = FontWeight.Medium)
        Text(amount, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WellplateTextPrimary)
    }
}

// ──────────────────────────────────────────────
// SCREEN 8 — HOME / DASHBOARD
// ──────────────────────────────────────────────
@Composable
fun DashboardScreen(viewModel: WellplateViewModel) {
    val profile by viewModel.userProfile.collectAsState()
    val todayMeals by viewModel.mealsForSelectedDate.collectAsState()
    val waterLogs by viewModel.waterLogsForSelectedDate.collectAsState()
    val isVoiceLogging by viewModel.isVoiceLogging.collectAsState()
    val voiceLogResponse by viewModel.voiceLogResponse.collectAsState()
    
    val currentCals = todayMeals.sumOf { it.calories }
    val goalCals = profile.calorieGoal
    val bmr = profile.calculateGoals().bmr
    val tdee = profile.calculateGoals().tdee

    val totalProt = todayMeals.sumOf { it.proteinG }
    val totalCarb = todayMeals.sumOf { it.carbsG }
    val totalFat = todayMeals.sumOf { it.fatG }

    val personalAiTip by viewModel.personalAiTip.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WellplateBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val dateFormatted = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date()).uppercase()
                    Text(
                        text = dateFormatted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = WellplateTextHint,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Good morning, ${profile.name}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = WellplateTextPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .border(2.dp, WellplateGreen, CircleShape)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(WellplateSurface)
                        .clickable { viewModel.currentScreen.value = Screen.ProfileSettings },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile Settings",
                        tint = WellplateGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Today Score Indicator
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = WellplateSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, WellplateBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(WellplateGreenLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = WellplateAmber,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "NUTRITION RATING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = WellplateTextHint,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Wellplate Daily Score",
                                fontSize = 16.sp,
                                color = WellplateGreenText,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(WellplateGreenLight, CircleShape)
                            .size(50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${viewModel.calculateDailyWellplateScore()}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = WellplateGreenText
                        )
                    }
                }
            }
        }

        // Calorie Ring & Donut Chart Card
        item {
            val rCals = goalCals - currentCals
            val isOver = rCals < 0
            val absCals = if (isOver) -rCals else rCals
            val statusLabel = if (isOver) "KCAL OVER" else "KCAL LEFT"
            val indicatorColor = if (isOver) Color.Red else WellplateGreen

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = WellplateSurface),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, WellplateBorder)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(170.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(160.dp)) {
                            val strokeWidth = 10.dp.toPx()
                            // Sleek gray track background
                            drawArc(
                                color = Color(0xFFF5F5F3),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(strokeWidth)
                            )
                            // Elegant progress arc
                            val sweepAngle = if (goalCals > 0) {
                                (currentCals.toFloat() / goalCals.toFloat() * 360f).coerceIn(0f, 360f)
                            } else {
                                0f
                            }
                            drawArc(
                                color = indicatorColor,
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%,d", absCals),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = WellplateTextPrimary,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = statusLabel,
                                fontSize = 10.sp,
                                color = if (isOver) Color.Red else WellplateTextHint,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Macro Progress bars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val maxP = profile.proteinGoal.coerceAtLeast(1.0)
                        val maxC = profile.carbsGoal.coerceAtLeast(1.0)
                        val maxF = profile.fatGoal.coerceAtLeast(1.0)
                        
                        MacroDashboardItem(
                            label = "Protein",
                            current = totalProt,
                            target = maxP,
                            color = WellplateGreen,
                            modifier = Modifier.weight(1f)
                        )
                        MacroDashboardItem(
                            label = "Carbs",
                            current = totalCarb,
                            target = maxC,
                            color = WellplateAmber,
                            modifier = Modifier.weight(1f)
                        )
                        MacroDashboardItem(
                            label = "Fat",
                            current = totalFat,
                            target = maxF,
                            color = WellplateGreenText,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Voice Food logger Quick interface
        item {
            VoiceLoggerComp(
                isVoiceLogging = isVoiceLogging,
                voiceLogResponse = voiceLogResponse,
                onLogSentence = { sentence -> viewModel.performVoiceLogging(sentence) },
                onAddFoodsToMeal = { loggedFoods, mealChoice ->
                    loggedFoods.forEach { food ->
                        viewModel.logMeal(
                            mealChoice,
                            food.foodName,
                            food.calories,
                            food.proteinG,
                            food.carbsG,
                            food.fatG,
                            confidence = food.confidencePercent
                        )
                    }
                    viewModel.voiceLogResponse.value = null
                }
            )
        }

        // Today's Meals Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Meals Logger",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = WellplateGreen,
                    letterSpacing = 0.5.sp
                )
                
                val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snacks")
                val chunkedMeals = mealTypes.chunked(2)
                chunkedMeals.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { mealType ->
                            val typedMeals = todayMeals.filter { it.mealType == mealType }
                            val calsLogged = typedMeals.sumOf { it.calories }
                            val hasLogged = calsLogged > 0
                            val alphaValue = if (hasLogged) 1.0f else 0.6f

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { viewModel.currentScreen.value = Screen.MealDiary }
                                    .testTag("meal_card_$mealType"),
                                colors = CardDefaults.cardColors(containerColor = WellplateSurface.copy(alpha = alphaValue)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, WellplateBorder.copy(alpha = alphaValue))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mealType,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = WellplateTextHint
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (hasLogged) "$calsLogged kcal" else "Add meal",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasLogged) WellplateGreenText else WellplateTextHint
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(WellplateBackground, RoundedCornerShape(10.dp))
                                            .clickable {
                                                viewModel.currentScreen.value = Screen.SearchAdd
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add $mealType",
                                            tint = if (hasLogged) WellplateGreen else WellplateTextHint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Water Tracker Segment
        item {
            val totalWater = waterLogs.sumOf { it.amountMl }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = WellplateSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, WellplateBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFE1F5FE), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = Color(0xFF0288D1),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Hydration",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = WellplateTextPrimary
                            )
                            Text(
                                text = "$totalWater ml of 2000 ml target",
                                fontSize = 12.sp,
                                color = WellplateTextHint
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.logWater(250) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1F5FE)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF0288D1),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+250ml",
                            color = Color(0xFF0288D1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Streak Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = WellplateSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, WellplateBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "🔥",
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Day ${profile.streak} streak!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = WellplateTextPrimary
                            )
                            Text(
                                text = "You're on fire today.",
                                fontSize = 12.sp,
                                color = WellplateTextHint
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.currentScreen.value = Screen.ProgressInsights },
                        colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "VIEW",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // AI Recommendation Tip Card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WellplateGreenLight, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(WellplateGreen, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubble,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WELLPLATE AI TIP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WellplateGreenText,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = personalAiTip,
                        fontSize = 13.sp,
                        color = WellplateTextPrimary,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Restaurant Mode shortcut Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.currentScreen.value = Screen.RestaurantMode },
                colors = CardDefaults.cardColors(containerColor = WellplateGreen),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Restaurant Companion",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Going out? Gemini predicts dish metrics!",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun MacroDashboardItem(label: String, current: Double, target: Double, color: Color, modifier: Modifier = Modifier) {
    val progress = (current / target).toFloat().coerceIn(0f, 1f)
    Column(modifier = modifier.padding(horizontal = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WellplateTextHint)
            Text("${current.toInt()}g / ${target.toInt()}g", fontSize = 10.sp, color = WellplateTextPrimary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = color,
            trackColor = WellplateGreenLight
        )
    }
}

@Composable
fun VoiceLoggerComp(
    isVoiceLogging: Boolean,
    voiceLogResponse: List<FoodScanResult>?,
    onLogSentence: (String) -> Unit,
    onAddFoodsToMeal: (List<FoodScanResult>, String) -> Unit
) {
    var rawTextMsg by remember { mutableStateOf("") }
    var selectedMealCategory by remember { mutableStateOf("Breakfast") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = WellplateSurface),
        border = BorderStroke(1.dp, WellplateBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Voice & Natural Language Logger", fontWeight = FontWeight.Bold, color = WellplateGreen, fontSize = 14.sp)
            Text("Simply type what you ate and let Gemini analyze!", fontSize = 11.sp, color = WellplateTextHint)
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                WellplateOutlinedTextField(
                    value = rawTextMsg,
                    onValueChange = { rawTextMsg = it },
                    placeholder = { Text("E.g. I had two boiled eggs and a black coffee") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (rawTextMsg.isNotBlank()) {
                            onLogSentence(rawTextMsg)
                            rawTextMsg = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen),
                    modifier = Modifier.size(54.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Listen", tint = Color.White)
                }
            }

            if (isVoiceLogging) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = WellplateGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gemini is digesting sentence...", fontSize = 12.sp, color = WellplateTextHint)
                }
            }

            voiceLogResponse?.let { results ->
                Spacer(modifier = Modifier.height(16.dp))
                Text("Gemini Decoctions Found:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WellplateTextPrimary)

                results.forEach { result ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = WellplateBackground),
                        border = BorderStroke(0.5.dp, WellplateBorder)
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(result.foodName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WellplateTextPrimary) },
                            supportingContent = { Text("Macros: P: ${result.proteinG}g | C: ${result.carbsG}g | F: ${result.fatG}g", color = WellplateTextBody) },
                            trailingContent = { Text("${result.calories} kcal", color = WellplateGreen, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Select meal destination
                Text("Save results to:", fontSize = 11.sp, color = WellplateTextHint)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val meals = listOf("Breakfast", "Lunch", "Dinner", "Snacks")
                    meals.forEach { category ->
                        val isPicked = selectedMealCategory == category
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPicked) WellplateGreen else WellplateInputBg)
                                .clickable { selectedMealCategory = category }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(category, fontSize = 11.sp, color = if (isPicked) Color.White else WellplateTextBody, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onAddFoodsToMeal(results, selectedMealCategory) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen)
                ) {
                    Text("Confirm & Log in Diary", color = Color.White)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// SCREEN 9 — FOOD SCANNER
// ──────────────────────────────────────────────
@OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun FoodScannerScreen(viewModel: WellplateViewModel) {
    val loading by viewModel.scannerLoading.collectAsState()
    val error by viewModel.scannerError.collectAsState()
    val result by viewModel.lastScannedFoodResult.collectAsState()
    val smartSuggestions by viewModel.smartSuggestions.collectAsState()

    var activeTab by remember { mutableStateOf("Camera Scan") } // Camera Scan / Barcode Scan
    var servingMultiplier by remember { mutableStateOf(1.0) }
    var mealTypeSelection by remember { mutableStateOf("Breakfast") }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Holder for CameraX ImageCapture use case
    val imageCaptureRef = remember { mutableStateOf<ImageCapture?>(null) }

    // Request camera permission with Accompanist API
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WellplateBackground)
            .padding(18.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("AI Scanner", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = WellplateGreen))
        Spacer(modifier = Modifier.height(12.dp))

        if (!cameraPermissionState.status.isGranted) {
            // Permission Block View
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = WellplateSurface),
                border = BorderStroke(1.dp, WellplateBorder)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera Access Required",
                        tint = WellplateGreen,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = WellplateTextPrimary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To analyze meals, identify foods, and scan nutritional barcodes instantly, please enable camera access.",
                        fontSize = 13.sp,
                        color = WellplateTextBody,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant Camera Permission", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Emulator Resilient Toggle: Demo Sim vs Real Camera
            var useMockCamera by remember { mutableStateOf(false) }
            var customQueryString by remember { mutableStateOf("") }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WellplateInputBg, RoundedCornerShape(20.dp))
                    .padding(4.dp)
            ) {
                listOf("Active Camera Device", "Demo / Testing").forEach { mode ->
                    val isSel = (mode.contains("Active") && !useMockCamera) || (mode.contains("Demo") && useMockCamera)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSel) WellplateGreen else Color.Transparent)
                            .clickable { useMockCamera = mode.contains("Demo") }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode,
                            color = if (isSel) Color.White else WellplateTextHint,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (useMockCamera) {
                // —————————————————————————————————————————————
                // COMPONENT A: MOCK SIMULATION MODE (EMULATOR SAFE)
                // —————————————————————————————————————————————
                Text(
                    text = "Demo Simulation Mode",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = WellplateGreen
                )
                Text(
                    text = "Instantly test the AI Vision scanning outputs without camera hardware issues. Select a preset or type any custom item.",
                    fontSize = 12.sp,
                    color = WellplateTextBody,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Grid of 4 beautiful interactive preset foods
                val simulatedPresets = listOf(
                    com.example.data.api.FoodScanResult(
                        foodName = "Masala Dosa Plate",
                        servingSizeG = 250.0,
                        calories = 420,
                        proteinG = 8.4,
                        carbsG = 65.2,
                        fatG = 12.6,
                        fibreG = 4.5,
                        sugarG = 3.2,
                        sodiumMg = 540.0,
                        confidencePercent = 98,
                        source = "GEMINI_SIMULATED"
                    ),
                    com.example.data.api.FoodScanResult(
                        foodName = "Avocado Chicken Salad",
                        servingSizeG = 350.0,
                        calories = 380,
                        proteinG = 28.5,
                        carbsG = 8.2,
                        fatG = 26.4,
                        fibreG = 5.2,
                        sugarG = 1.6,
                        sodiumMg = 680.0,
                        confidencePercent = 95,
                        source = "GEMINI_SIMULATED"
                    ),
                    com.example.data.api.FoodScanResult(
                        foodName = "Greek Berry Grain Bowl",
                        servingSizeG = 300.0,
                        calories = 310,
                        proteinG = 14.1,
                        carbsG = 48.0,
                        fatG = 6.8,
                        fibreG = 7.1,
                        sugarG = 12.4,
                        sodiumMg = 110.0,
                        confidencePercent = 96,
                        source = "GEMINI_SIMULATED"
                    ),
                    com.example.data.api.FoodScanResult(
                        foodName = "Crunchy Granola Bar",
                        servingSizeG = 42.0,
                        calories = 180,
                        proteinG = 4.2,
                        carbsG = 24.5,
                        fatG = 8.1,
                        fibreG = 3.0,
                        sugarG = 8.2,
                        sodiumMg = 90.0,
                        confidencePercent = 92,
                        source = "OPEN_FOOD_FACTS"
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    simulatedPresets.chunked(2).forEach { rowPresets ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowPresets.forEach { item ->
                                val emoji = when {
                                    item.foodName.contains("Dosa") -> "🍳 dosa"
                                    item.foodName.contains("Salad") -> "🥗 salad"
                                    item.foodName.contains("Greek") -> "🍓 bowl"
                                    else -> "🍫 bar"
                                }
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            viewModel.scannerLoading.value = true
                                            viewModel.scannerError.value = null
                                            viewModel.lastScannedFoodResult.value = null
                                            coroutineScope.launch {
                                                kotlinx.coroutines.delay(1000)
                                                viewModel.lastScannedFoodResult.value = item
                                                viewModel.scannerLoading.value = false
                                            }
                                        },
                                    colors = CardDefaults.cardColors(containerColor = WellplateSurface),
                                    border = BorderStroke(1.dp, WellplateBorder)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(emoji, fontSize = 28.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(item.foodName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WellplateTextPrimary)
                                        Text("${item.calories} Kcal", fontSize = 11.sp, color = WellplateTextHint)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom food generator field (triggers real Gemini nutrition estimation)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WellplateInputBg),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Simulate Custom Item Scan (Dynamic AI Estimation)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WellplateTextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = customQueryString,
                                onValueChange = { customQueryString = it },
                                placeholder = { Text("e.g., Pepperoni Pizza slice", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = WellplateBorder,
                                    focusedBorderColor = WellplateGreen,
                                    unfocusedContainerColor = WellplateSurface,
                                    focusedContainerColor = WellplateSurface
                                ),
                                shape = RoundedCornerShape(10.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                            )

                            Button(
                                onClick = {
                                    val q = customQueryString.trim()
                                    if (q.isNotEmpty()) {
                                        viewModel.scannerLoading.value = true
                                        viewModel.scannerError.value = null
                                        viewModel.lastScannedFoodResult.value = null
                                        coroutineScope.launch {
                                            try {
                                                kotlinx.coroutines.delay(1000)
                                                val results = com.example.data.api.GeminiClient.queryCustomFoodManual(q)
                                                if (results.isNotEmpty()) {
                                                    viewModel.lastScannedFoodResult.value = results.first()
                                                } else {
                                                    viewModel.scannerError.value = "Couldn't estimate nutritional value for that food. Try another query."
                                                }
                                            } catch (e: Exception) {
                                                viewModel.scannerError.value = "Simulation Scan Error: ${e.message}"
                                            } finally {
                                                viewModel.scannerLoading.value = false
                                            }
                                        }
                                    }
                                },
                                enabled = customQueryString.isNotBlank() && !loading,
                                colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Scan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Loading Overlay for simulation mode
                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(WellplateSurface)
                            .border(1.dp, WellplateGreen.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = WellplateGreen, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Simulating scanner and querying Gemini...", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WellplateGreen)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

            } else {
                // —————————————————————————————————————————————
                // COMPONENT B: STABLE CAMERA SCAN WITH TEXTUREVIEW fallback and ASYNC cleanup
                // —————————————————————————————————————————————
                // Tabs: Camera Scan vs Barcode Scan
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WellplateInputBg, RoundedCornerShape(24.dp))
                        .padding(4.dp)
                ) {
                    listOf("Camera Scan", "Barcode Scan").forEach { tab ->
                        val isSel = activeTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSel) WellplateGreen else Color.Transparent)
                                .clickable { activeTab = tab }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (tab.contains("Camera")) Icons.Default.CameraAlt else Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = if (isSel) Color.White else WellplateTextHint,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tab, color = if (isSel) Color.White else WellplateTextHint, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Real Camera Preview with non-blocking unbind implementation
                val cameraProviderRef = remember { mutableStateOf<ProcessCameraProvider?>(null) }

                androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                    onDispose {
                        try {
                            cameraProviderRef.value?.unbindAll()
                        } catch (e: Exception) {
                            Log.e("CameraX", "Cleaning up camera failed during dispose", e)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, WellplateGreen.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            }
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    cameraProviderRef.value = cameraProvider

                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }

                                    val imageCapture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                        .build()
                                    imageCaptureRef.value = imageCapture

                                    cameraProvider.unbindAll()
                                    
                                    val selector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                                        CameraSelector.DEFAULT_BACK_CAMERA
                                    } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                                        CameraSelector.DEFAULT_FRONT_CAMERA
                                    } else {
                                        null
                                    }

                                    if (selector != null) {
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            selector,
                                            preview,
                                            imageCapture
                                        )
                                    } else {
                                        viewModel.scannerError.value = "No camera hardware detected. Switched to Demo / Testing Mode is recommended."
                                    }
                                } catch (e: Throwable) {
                                    Log.e("CameraX", "Failed to initialize camera or bind lifecycle", e)
                                    viewModel.scannerError.value = "Camera initialization failed: ${e.message}. Switch to Demo / Testing above."
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // High Contrast Reticle Target Overlay
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .border(2.dp, WellplateGreen, RoundedCornerShape(16.dp))
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            Text(
                                text = if (activeTab == "Camera Scan") "Align plate here" else "Position barcode here",
                                color = Color.White.copy(alpha = 0.82f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }

                    // Loading overlay while calling Gemini
                    if (loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.72f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                CircularProgressIndicator(color = WellplateGreen, strokeWidth = 3.dp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Gemini is sifting pixels...",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Capturing scan trigger button
                if (!loading && result == null) {
                    Button(
                        onClick = {
                            val imageCapture = imageCaptureRef.value
                            if (imageCapture != null) {
                                val executor = Executors.newSingleThreadExecutor()
                                imageCapture.takePicture(
                                    executor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            try {
                                                val buffer = image.planes[0].buffer
                                                val bytes = ByteArray(buffer.remaining())
                                                buffer.get(bytes)
                                                
                                                // 1. Memory-safe subsample decoding
                                                val options = BitmapFactory.Options().apply {
                                                    inJustDecodeBounds = true
                                                }
                                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                                                options.inSampleSize = calculateInSampleSize(options, 1024, 1024)
                                                options.inJustDecodeBounds = false
                                                
                                                val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                                                if (rawBitmap != null) {
                                                    // 2. Rotate memory-safely
                                                    val matrix = android.graphics.Matrix().apply {
                                                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                                                    }
                                                    val rotatedBitmap = Bitmap.createBitmap(
                                                        rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
                                                    )
                                                    
                                                    if (rawBitmap != rotatedBitmap) {
                                                        rawBitmap.recycle()
                                                    }
                                                    
                                                    // 3. Strictly constraint sizes to avoid OOM Base64 conversion
                                                    var finalBitmap = rotatedBitmap
                                                    val maxDim = 1024
                                                    if (finalBitmap.width > maxDim || finalBitmap.height > maxDim) {
                                                        val aspectRatio = finalBitmap.width.toFloat() / finalBitmap.height.toFloat()
                                                        val (newWidth, newHeight) = if (finalBitmap.width > finalBitmap.height) {
                                                            maxDim to (maxDim / aspectRatio).toInt()
                                                        } else {
                                                            (maxDim * aspectRatio).toInt() to maxDim
                                                        }
                                                        val scaled = Bitmap.createScaledBitmap(finalBitmap, newWidth, newHeight, true)
                                                        if (scaled != finalBitmap) {
                                                            finalBitmap.recycle()
                                                            finalBitmap = scaled
                                                        }
                                                    }

                                                    coroutineScope.launch {
                                                        viewModel.scanCameraImage(finalBitmap, activeTab == "Barcode Scan")
                                                    }
                                                } else {
                                                    viewModel.scannerError.value = "Failed to process image data."
                                                }
                                            } catch (e: Exception) {
                                                Log.e("CameraX", "Error parsing camera image", e)
                                                viewModel.scannerError.value = "Image decoding error: ${e.message}"
                                            } finally {
                                                image.close()
                                                executor.shutdown()
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            Log.e("CameraX", "Trigger capture failed", exception)
                                            viewModel.scannerError.value = "Camera capture failed: ${exception.message}"
                                            executor.shutdown()
                                        }
                                    }
                                )
                            } else {
                                viewModel.scannerError.value = "Camera is still initializing. Please wait."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Scan button", tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyze Now with Gemini Vision", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Error display card
            error?.let { err ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE8E8)),
                    border = BorderStroke(1.dp, Color(0xFFF8B4B4))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(err, color = Color(0xFF9B1C1C), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Scanning output results
            result?.let { food ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = WellplateSurface),
                    border = BorderStroke(1.5.dp, WellplateGreen)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = food.foodName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = WellplateTextPrimary),
                                        modifier = Modifier.weight(1f, fill = false),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    DataSourceBadge(source = food.source ?: "GEMINI_ESTIMATE")
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                        text = "Confidence: ${food.confidencePercent}% match",
                                        color = WellplateGreenText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "${(food.calories * servingMultiplier).toInt()} kcal",
                                style = MaterialTheme.typography.titleLarge.copy(color = WellplateGreen, fontWeight = FontWeight.ExtraBold),
                                modifier = Modifier.padding(start = 8.dp),
                                maxLines = 1
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = WellplateDivider)

                        PortionVisualizerComp()

                        Spacer(modifier = Modifier.height(12.dp))

                        // Servings Modifier Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Portion Multiplier:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WellplateTextPrimary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (servingMultiplier > 0.25) servingMultiplier -= 0.25 }) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = WellplateGreen)
                                }
                                Text(
                                    text = "${(servingMultiplier * 100).toInt()}% (${(food.servingSizeG * servingMultiplier).toInt()}g)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = WellplateTextPrimary
                                )
                                IconButton(onClick = { servingMultiplier += 0.25 }) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = WellplateGreen)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Scaled Nutrition badging
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            NutritionMiniBadge(name = "Protein", value = "${String.format("%.1f", food.proteinG * servingMultiplier)}g", col = Color(0xFFFF8A65))
                            NutritionMiniBadge(name = "Carbs", value = "${String.format("%.1f", food.carbsG * servingMultiplier)}g", col = Color(0xFFFFD54F))
                            NutritionMiniBadge(name = "Fat", value = "${String.format("%.1f", food.fatG * servingMultiplier)}g", col = Color(0xFF4FC3F7))
                            NutritionMiniBadge(name = "Fibre", value = "${String.format("%.1f", food.fibreG * servingMultiplier)}g", col = Color(0xFF81C784))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Meal Choice diary categorization targets
                        Text("Add to diary segment:", fontSize = 12.sp, color = WellplateTextHint, fontWeight = FontWeight.SemiBold)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { cat ->
                                val active = mealTypeSelection == cat
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) WellplateGreen else WellplateInputBg)
                                        .clickable { mealTypeSelection = cat }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(cat, fontSize = 11.sp, color = if (active) Color.White else WellplateTextBody, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.logMeal(
                                        mealTypeSelection,
                                        food.foodName,
                                        food.calories,
                                        food.proteinG,
                                        food.carbsG,
                                        food.fatG,
                                        food.fibreG,
                                        food.sugarG,
                                        food.sodiumMg,
                                        food.confidencePercent,
                                        food.servingSizeG,
                                        servingMultiplier
                                    )
                                    viewModel.parseWholeIngredientsToShopping(food.foodName)
                                    viewModel.lastScannedFoodResult.value = null
                                    viewModel.currentScreen.value = Screen.Dashboard
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen)
                            ) {
                                Text("Add to $mealTypeSelection", color = Color.White)
                            }

                            OutlinedButton(
                                onClick = { viewModel.lastScannedFoodResult.value = null },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = WellplateGreen),
                                border = BorderStroke(1.dp, WellplateGreen)
                            ) {
                                Text("Scan Again")
                            }
                        }
                    }
                }
            }

            // Smart advice recommendations of combos
            if (smartSuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WellplateGreenLight),
                    border = BorderStroke(1.dp, WellplateBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("💡 Smart Combos of Wellplate Suggestions", fontWeight = FontWeight.Bold, color = WellplateGreenText, fontSize = 14.sp)
                        Text("Complimentary plates recommended by Gemini system:", fontSize = 11.sp, color = WellplateTextHint)
                        Spacer(modifier = Modifier.height(8.dp))
                        smartSuggestions.forEach { s ->
                            Text(
                                text = s,
                                fontSize = 12.sp,
                                color = WellplateTextBody,
                                modifier = Modifier.padding(vertical = 4.dp),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PortionVisualizerComp() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = WellplateAmberLight),
        border = BorderStroke(1.dp, WellplateAmber)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("✊ Portion Guide Visual Assistant", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WellplateAmber)
            Spacer(modifier = Modifier.height(4.dp))
            Text("• Hand Palm = ~85g of steak, meat, or salmon target\n• Rounded Fist = ~1 cup of cereal, rice, or broccoli\n• Flat Thumb = ~1 tablespoon of oil, butter, or almonds", fontSize = 11.sp, color = WellplateTextBody, lineHeight = 15.sp)
        }
    }
}

// ──────────────────────────────────────────────
// SCREEN 10 — MEAL LOG / DIARY
// ──────────────────────────────────────────────
@Composable
fun MealDiaryScreen(viewModel: WellplateViewModel) {
    val meals by viewModel.mealsForSelectedDate.collectAsState()
    val activeDate by viewModel.selectedDate.collectAsState()
    
    val profile by viewModel.userProfile.collectAsState()
    val calorieGoal = profile.calorieGoal

    var showSaveTemplateSheet by remember { mutableStateOf(false) }
    var selectedMealType by remember { mutableStateOf("") }
    var selectedMealItems by remember { mutableStateOf<List<com.example.data.local.db.MealLogEntity>>(emptyList()) }

    // Calendar strip calculations
    val calendarDays = remember {
        val daysList = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -3)
        for (i in 0..6) {
            daysList.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        daysList
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WellplateBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Nutrition Journal", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = WellplateGreen))
            TextButton(
                onClick = { viewModel.currentScreen.value = Screen.MealTemplates },
                colors = ButtonDefaults.textButtonColors(contentColor = WellplateGreen)
            ) {
                Icon(
                    imageVector = Icons.Default.RestaurantMenu,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Templates", fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Calendar Strip header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            calendarDays.forEach { date ->
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                val dayNameStr = SimpleDateFormat("EEE", Locale.getDefault()).format(date)
                val dayNumStr = SimpleDateFormat("d", Locale.getDefault()).format(date)
                val isSelected = activeDate == dateStr

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .clickable { viewModel.selectedDate.value = dateStr },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) WellplateGreen else WellplateSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isSelected) WellplateGreen else WellplateBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(dayNameStr, fontSize = 10.sp, color = if (isSelected) Color.White.copy(alpha = 0.8f) else WellplateTextHint)
                        Text(dayNumStr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else WellplateTextPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Meal logging displays grouped
        val categories = listOf("Breakfast", "Lunch", "Dinner", "Snacks")
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            categories.forEach { category ->
                val typedMeals = meals.filter { it.mealType == category }
                val typedCalories = typedMeals.sumOf { it.calories }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(category, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = WellplateGreenText)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$typedCalories kcal", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WellplateTextHint)
                            if (typedMeals.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        selectedMealType = category
                                        selectedMealItems = typedMeals
                                        showSaveTemplateSheet = true
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BookmarkBorder,
                                        contentDescription = "Save as template",
                                        tint = WellplateGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (typedMeals.isEmpty()) {
                    item {
                        Card(
                             modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = WellplateSurface),
                            border = BorderStroke(0.5.dp, WellplateBorder)
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = { Text("No meals logged yet", fontSize = 13.sp, color = WellplateTextHint) },
                                trailingContent = {
                                    IconButton(onClick = { viewModel.currentScreen.value = Screen.SearchAdd }) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = WellplateGreen)
                                    }
                                }
                            )
                        }
                    }
                } else {
                    items(typedMeals) { meal ->
                        // Swipe action swipeable list wrapper
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = WellplateSurface),
                            border = BorderStroke(1.dp, WellplateBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(meal.foodName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WellplateTextPrimary)
                                    Text("Serving: ${(meal.servingSizeG * meal.quantityMultiplier).toInt()}g | P: ${String.format("%.1f", meal.proteinG)}g, C: ${String.format("%.1f", meal.carbsG)}g, F: ${String.format("%.1f", meal.fatG)}g", fontSize = 11.sp, color = WellplateTextBody)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${meal.calories} kcal", fontWeight = FontWeight.ExtraBold, color = WellplateGreen, fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                                    IconButton(onClick = { viewModel.deleteMeal(meal) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = WellplateTextHint)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Summary card totals
            item {
                val totalCals = meals.sumOf { it.calories }
                val totalProt = meals.sumOf { it.proteinG }
                val totalCarb = meals.sumOf { it.carbsG }
                val totalFat = meals.sumOf { it.fatG }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WellplateGreenLight),
                    border = BorderStroke(1.dp, WellplateBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Day totals summary:", fontWeight = FontWeight.Bold, color = WellplateGreenText, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Calories: $totalCals of $calorieGoal kcal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WellplateTextPrimary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Protein: ${totalProt.toInt()}g", fontSize = 11.sp, color = WellplateTextBody)
                            Text("Carbs: ${totalCarb.toInt()}g", fontSize = 11.sp, color = WellplateTextBody)
                            Text("Fat: ${totalFat.toInt()}g", fontSize = 11.sp, color = WellplateTextBody)
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showSaveTemplateSheet) {
        com.example.ui.templates.SaveAsTemplateSheet(
            currentMealType = selectedMealType,
            currentItems = selectedMealItems,
            viewModel = viewModel.mealTemplateViewModel,
            onDismiss = { showSaveTemplateSheet = false }
        )
    }
}

// ──────────────────────────────────────────────
// SCREEN 11 — SEARCH & ADD FOOD MANUALLY
// ──────────────────────────────────────────────
@Composable
fun SearchAddScreen(viewModel: WellplateViewModel) {
    var query by remember { mutableStateOf("") }
    val searchResults by viewModel.lastSearchedCustomFoods.collectAsState()
    val templatesState by viewModel.mealTemplateViewModel.templates.collectAsState()

    var customName by remember { mutableStateOf("") }
    var customCalories by remember { mutableStateOf("") }
    var customProtein by remember { mutableStateOf("") }
    var customCarbs by remember { mutableStateOf("") }
    var customFat by remember { mutableStateOf("") }
    var selectedMeal by remember { mutableStateOf("Breakfast") }

    var manualActiveResultSheet by remember { mutableStateOf<FoodScanResult?>(null) }
    var sheetPortionInputMultiplier by remember { mutableStateOf(1.0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WellplateBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Log Custom Foods", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = WellplateGreen))
        Spacer(modifier = Modifier.height(14.dp))

        // Large beautiful search bar autocomplete
        WellplateOutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.performManualSearch(it)
            },
            placeholder = { Text("Search ingredients via Gemini system...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = WellplateTextHint) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (templatesState.isNotEmpty() && query.isBlank()) {
            Text("Quick Add Templates to $selectedMeal:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WellplateTextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { cat ->
                    val isSel = selectedMeal == cat
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) WellplateGreen else WellplateInputBg)
                            .clickable { selectedMeal = cat }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(cat, fontSize = 11.sp, color = if (isSel) Color.White else WellplateTextBody, fontWeight = FontWeight.Bold)
                    }
                }
            }
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(templatesState.take(5)) { template ->
                    QuickTemplateChip(
                        name = template.template.name,
                        calories = template.template.totalCalories,
                        onClick = {
                            viewModel.mealTemplateViewModel.logTemplate(
                                templateId = template.template.id,
                                targetMeal = selectedMeal,
                                selectedDate = viewModel.selectedDate.value
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (searchResults.isNotEmpty()) {
            Text("Search Results based on 100g portions:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WellplateTextPrimary)
            searchResults.forEach { result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { manualActiveResultSheet = result },
                    colors = CardDefaults.cardColors(containerColor = WellplateSurface),
                    border = BorderStroke(1.dp, WellplateBorder)
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(result.foodName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WellplateTextPrimary) },
                        supportingContent = { Text("P: ${result.proteinG}g | C: ${result.carbsG}g | F: ${result.fatG}g", color = WellplateTextBody) },
                        trailingContent = { Text("${result.calories} kcal", fontWeight = FontWeight.ExtraBold, color = WellplateGreenText) }
                    )
                }
            }
        }

        // Selected ingredient detail configuration form sheet popup
        manualActiveResultSheet?.let { result ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = WellplateAmberLight),
                border = BorderStroke(1.dp, WellplateAmber)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add: ${result.foodName}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = WellplateAmber)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Portion size (grams):", fontSize = 12.sp, color = WellplateTextPrimary)
                        Row {
                            IconButton(onClick = { if (sheetPortionInputMultiplier > 0.2) sheetPortionInputMultiplier -= 0.25 }) {
                                Icon(Icons.Default.Remove, contentDescription = null, tint = WellplateAmber)
                            }
                            Text("${(result.servingSizeG * sheetPortionInputMultiplier).toInt()}g", fontWeight = FontWeight.Bold, color = WellplateTextPrimary, modifier = Modifier.align(Alignment.CenterVertically))
                            IconButton(onClick = { sheetPortionInputMultiplier += 0.25 }) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = WellplateAmber)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { cat ->
                            val isSel = selectedMeal == cat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) WellplateGreen else WellplateInputBg)
                                    .clickable { selectedMeal = cat }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                  Text(cat, fontSize = 11.sp, color = if (isSel) Color.White else WellplateTextBody, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.logMeal(
                                    selectedMeal,
                                    result.foodName,
                                    result.calories,
                                    result.proteinG,
                                    result.carbsG,
                                    result.fatG,
                                    confidence = 100,
                                    servingSizeG = result.servingSizeG,
                                    multiplier = sheetPortionInputMultiplier
                                )
                                manualActiveResultSheet = null
                                viewModel.currentScreen.value = Screen.Dashboard
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Confirm Add", color = Color.White)
                        }
                        OutlinedButton(
                            onClick = { manualActiveResultSheet = null },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WellplateGreen),
                            border = BorderStroke(1.dp, WellplateGreen)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // "Create custom food" section
        Text("Or Create Recipe / Custom Food", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WellplateGreen)
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = WellplateSurface),
            border = BorderStroke(1.dp, WellplateBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                WellplateOutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Food Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WellplateOutlinedTextField(
                        value = customCalories,
                        onValueChange = { customCalories = it },
                        label = { Text("Calories (kcal)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    WellplateOutlinedTextField(
                        value = customProtein,
                        onValueChange = { customProtein = it },
                        label = { Text("Protein (g)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WellplateOutlinedTextField(
                        value = customCarbs,
                        onValueChange = { customCarbs = it },
                        label = { Text("Carbs (g)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    WellplateOutlinedTextField(
                        value = customFat,
                        onValueChange = { customFat = it },
                        label = { Text("Fat (g)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text("Add to Diary group:", fontSize = 11.sp, color = WellplateTextHint)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { choice ->
                        val active = selectedMeal == choice
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) WellplateGreen else WellplateInputBg)
                                .clickable { selectedMeal = choice }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(choice, fontSize = 11.sp, color = if (active) Color.White else WellplateTextBody, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val cals = customCalories.toIntOrNull() ?: 100
                        val prot = customProtein.toDoubleOrNull() ?: 0.0
                        val carbs = customCarbs.toDoubleOrNull() ?: 0.0
                        val fat = customFat.toDoubleOrNull() ?: 0.0

                        if (customName.isNotBlank()) {
                            viewModel.logMeal(
                                selectedMeal,
                                customName,
                                cals,
                                prot,
                                carbs,
                                fat,
                                confidence = 100
                            )
                            // reset
                            customName = ""
                            customCalories = ""
                            customProtein = ""
                            customCarbs = ""
                            customFat = ""
                            viewModel.currentScreen.value = Screen.Dashboard
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen)
                ) {
                    Text("Save Custom Food", color = Color.White)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// SCREEN 12 — PROGRESS & INSIGHTS
// ──────────────────────────────────────────────
@Composable
fun ProgressInsightsScreen(viewModel: WellplateViewModel) {
    val allMeals by viewModel.allMealsFlow.collectAsState()
    val weightHist by viewModel.weightHistory.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val groceryList by viewModel.shoppingList.collectAsState()

    var weightInputText by remember { mutableStateOf("") }
    var scaleUnits by remember { mutableStateOf("kg") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WellplateBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Analytics & Shopping", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = WellplateGreen))
        Spacer(modifier = Modifier.height(16.dp))

        // Week stats bar graph drawing Canvas (Weekly calorie chart)
        Text("Weekly Calorie History", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WellplateGreenText)
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = WellplateSurface),
            border = BorderStroke(1.dp, WellplateBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Static drawn custom Canvas bar chart for last 7 days calories
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    val maxVal = 2500f
                    val barWidth = 32.dp.toPx()
                    val space = 18.dp.toPx()
                    val days = listOf("Wed", "Thu", "Fri", "Sat", "Sun", "Mon", "Tue")
                    val mockVals = listOf(1650f, 1850f, 2100f, 1500f, 2250f, 1780f, 1950f)

                    mockVals.forEachIndexed { idx, value ->
                        val pct = value / maxVal
                        val barHeight = size.height * pct * 0.75f
                        val x = idx * (barWidth + space) + space
                        val y = size.height - barHeight - 20.dp.toPx()

                        // Bar
                        drawRoundRect(
                            color = if (idx == mockVals.size - 1) WellplateGreen else WellplateAmber,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = listOf("Wed", "Thu", "Fri", "Sat", "Sun", "Mon", "Tue")
                    days.forEach { day ->
                        Text(day, fontSize = 10.sp, color = WellplateTextHint, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Weight tracking history section
        Text("Weight Log Manager", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WellplateGreenText)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = WellplateSurface),
            border = BorderStroke(1.dp, WellplateBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WellplateOutlinedTextField(
                        value = weightInputText,
                        onValueChange = { weightInputText = it },
                        placeholder = { Text("E.g. 72.5") },
                        label = { Text("Log Today's Weight") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val doubleW = weightInputText.toDoubleOrNull()
                            if (doubleW != null) {
                                viewModel.logWeight(doubleW)
                                weightInputText = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen)
                    ) {
                        Text("Log", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Canvas bezier graph drawing
                Text("Weight Trend over time (${profile.weightUnit})", fontSize = 11.sp, color = WellplateTextHint, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    val path = Path()
                    path.moveTo(0f, size.height * 0.5f)
                    path.quadraticTo(size.width * 0.25f, size.height * 0.4f, size.width * 0.5f, size.height * 0.6f)
                    path.quadraticTo(size.width * 0.75f, size.height * 0.2f, size.width, size.height * 0.3f)

                    drawPath(
                        path = path,
                        color = WellplateGreen,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw reference target lines
                    drawLine(
                        color = WellplateAmber.copy(alpha = 0.3f),
                        start = Offset(0f, size.height * 0.35f),
                        end = Offset(size.width, size.height * 0.35f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("May 14", fontSize = 9.sp, color = WellplateTextHint)
                    Text("Current Goal: ${profile.goalWeight} ${profile.weightUnit}", fontSize = 10.sp, color = WellplateAmber, fontWeight = FontWeight.Bold)
                    Text("Today", fontSize = 9.sp, color = WellplateTextHint)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Shopping Ingredients List Category (Extra 5)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🛒 Wellness Grocery List", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WellplateGreenText)
            if (groceryList.isNotEmpty()) {
                Text(
                    "Clear All",
                    fontSize = 12.sp,
                    color = WellplateAmber,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.clearShoppingList() }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (groceryList.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = WellplateSurface),
                border = BorderStroke(1.dp, WellplateBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ShoppingBasket, contentDescription = null, tint = WellplateTextHint, modifier = Modifier.size(36.dp))
                    Text("Your shopping list is empty.", fontSize = 12.sp, color = WellplateTextHint, fontWeight = FontWeight.SemiBold)
                    Text("Add food items on Scanner screen to see ingredients dynamically mapped!", fontSize = 10.sp, color = WellplateTextHint, modifier = Modifier.padding(top = 4.dp), textAlign = TextAlign.Center)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                groceryList.forEach { ingredient ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (ingredient.isChecked) WellplateInputBg else WellplateSurface),
                        border = BorderStroke(1.dp, WellplateBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = ingredient.isChecked,
                                    onCheckedChange = { viewModel.toggleShoppingItem(ingredient) },
                                    colors = CheckboxDefaults.colors(checkedColor = WellplateGreen)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text(
                                        text = ingredient.ingredientName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        textDecoration = if (ingredient.isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                        color = if (ingredient.isChecked) WellplateTextHint else WellplateTextPrimary
                                    )
                                    if (ingredient.associatedFoodName.isNotEmpty()) {
                                        Text("From: ${ingredient.associatedFoodName}", fontSize = 10.sp, color = WellplateTextBody)
                                    }
                                }
                            }

                            IconButton(onClick = { viewModel.deleteShoppingItem(ingredient) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Delete Item", tint = WellplateTextHint)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ──────────────────────────────────────────────
// SCREEN 13 — PROFILE & SETTINGS
// ──────────────────────────────────────────────
// SCREEN 13 — PROFILE & SETTINGS
// ──────────────────────────────────────────────
@Composable
fun ProfileSettingsScreen(viewModel: WellplateViewModel) {
    val profile by viewModel.userProfile.collectAsState()
    
    var showEditDetailsDialog by remember { mutableStateOf(false) }

    var editName by remember { mutableStateOf(profile.name) }
    var editAge by remember { mutableStateOf(profile.age) }
    var editWeight by remember { mutableStateOf(profile.weight) }
    var editGoalWeight by remember { mutableStateOf(profile.goalWeight) }
    var showSignOutConfirmationDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WellplateBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Profile & Settings", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = WellplateGreen))

        // User profile summarizer Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = WellplateSurface),
            border = BorderStroke(1.dp, WellplateBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(WellplateGreenLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (profile.name.isNotEmpty()) profile.name.take(1).uppercase() else "W",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WellplateGreenText
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = WellplateTextPrimary)
                Text("Bio: ${profile.sex} | ${profile.age} yrs | ${profile.height.toInt()} ${profile.heightUnit}", fontSize = 12.sp, color = WellplateTextBody)

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        editName = profile.name
                        editAge = profile.age
                        editWeight = profile.weight
                        editGoalWeight = profile.goalWeight
                        showEditDetailsDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Personal Details", color = Color.White)
                }
            }
        }

        // Calorie details recalculator CTA
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = WellplateSurface),
            border = BorderStroke(1.dp, WellplateBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Current Active Targets:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WellplateTextPrimary)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Daily Calorie Budget:", fontSize = 13.sp, color = WellplateTextBody)
                    Text("${profile.calorieGoal} kcal", fontWeight = FontWeight.Bold, color = WellplateGreen)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Goal Type / Pace:", fontSize = 13.sp, color = WellplateTextBody)
                    Text("${profile.goalType} / ${profile.pace}", fontWeight = FontWeight.SemiBold, color = WellplateTextHint)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        // Re-trigger calculations based on profile inputs
                        viewModel.saveUserProfile(profile, navigateToHome = false)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Recalculate Calorie Target", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Metric/Imperial units settings selector toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = WellplateSurface),
            border = BorderStroke(1.dp, WellplateBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("App Preferences", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WellplateTextPrimary)
                Spacer(modifier = Modifier.height(12.dp))

                // Units toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Display Units System:", color = WellplateTextBody)
                    Row(
                        modifier = Modifier
                            .background(WellplateInputBg, RoundedCornerShape(20.dp))
                            .padding(2.dp)
                    ) {
                        listOf("METRIC", "IMPERIAL").forEach { unitSystem ->
                            val active = profile.units == unitSystem
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (active) WellplateGreen else Color.Transparent)
                                    .clickable { viewModel.updateUnitsSetting(unitSystem) }
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(unitSystem, color = if (active) Color.White else WellplateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Theme choice toggle (Light / Dark / System)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Theme Mode:", color = WellplateTextBody, modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .background(WellplateInputBg, RoundedCornerShape(20.dp))
                            .padding(2.dp)
                            .widthIn(max = 220.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf("LIGHT", "DARK", "SYSTEM").forEach { stateTheme ->
                            val active = profile.theme == stateTheme
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .background(if (active) WellplateGreen else Color.Transparent)
                                    .clickable { viewModel.updateTheme(stateTheme) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stateTheme,
                                    color = if (active) Color.White else WellplateTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Notification active reminders switches (Extra 7)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = WellplateSurface),
            border = BorderStroke(1.dp, WellplateBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Notification Reminder Settings", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WellplateTextPrimary)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Hydration Water Reminder", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WellplateTextPrimary)
                        Text("Remind to drink water every 2 hours", fontSize = 11.sp, color = WellplateTextBody)
                    }
                    Switch(
                        checked = profile.waterReminder,
                        onCheckedChange = { viewModel.updateReminders(profile.mealReminder, it, profile.dailySummary) },
                        colors = SwitchDefaults.colors(checkedThumbColor = WellplateGreen, checkedTrackColor = WellplateGreen.copy(alpha = 0.5f))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Meal Logging Reminder", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WellplateTextPrimary)
                        Text("Remind to log breakfast, lunch & dinner", fontSize = 11.sp, color = WellplateTextBody)
                    }
                    Switch(
                        checked = profile.mealReminder,
                        onCheckedChange = { viewModel.updateReminders(it, profile.waterReminder, profile.dailySummary) },
                        colors = SwitchDefaults.colors(checkedThumbColor = WellplateGreen, checkedTrackColor = WellplateGreen.copy(alpha = 0.5f))
                    )
                }
            }
        }

        // Supabase Cloud Sync Card
        val syncStatus by viewModel.supabaseSyncStatus.collectAsState()
        val lastSyncedTime by viewModel.supabaseLastSyncedTime.collectAsState()
        val isSupabaseConfigured = com.example.data.api.SupabaseClient.isConfigured()

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = WellplateSurface),
            border = BorderStroke(1.5.dp, if (isSupabaseConfigured) WellplateGreen else Color.Gray.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = "Supabase",
                            tint = if (isSupabaseConfigured) WellplateGreen else WellplateTextHint,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Supabase Cloud Sync",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = WellplateTextPrimary
                        )
                    }
                    
                    // Connected/Disconnected Badge
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSupabaseConfigured) WellplateGreenLight else Color.Red.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isSupabaseConfigured) "Configured" else "Unconfigured",
                            color = if (isSupabaseConfigured) WellplateGreenText else Color.Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = WellplateDivider)
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Status Code:", fontSize = 12.sp, color = WellplateTextBody)
                        Text(
                            text = syncStatus,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = when {
                                syncStatus.contains("Success") || syncStatus.contains("Complete") -> WellplateGreenText
                                syncStatus.contains("Error") || syncStatus.contains("Failed") -> Color.Red
                                syncStatus.contains("Syncing") || syncStatus.contains("Restoring") -> WellplateAmber
                                else -> WellplateTextHint
                            }
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Last Backup/Restore:", fontSize = 12.sp, color = WellplateTextBody)
                        Text(
                            text = lastSyncedTime,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = WellplateTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.syncWithSupabase() },
                        enabled = isSupabaseConfigured && !syncStatus.contains("ing..."),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Backup", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.restoreFromSupabase() },
                        enabled = isSupabaseConfigured && !syncStatus.contains("ing..."),
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, if (isSupabaseConfigured) WellplateGreen else Color.Gray),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WellplateGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isSupabaseConfigured) WellplateGreen else Color.Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // About card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = WellplateSurface),
            border = BorderStroke(1.dp, WellplateBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("About Wellplate AI", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WellplateGreenText)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Active Session Version 1.0.0 (Google AI Studio)", fontSize = 11.sp, color = WellplateTextBody)
                Text("Powered by Gemini 3.5 Flash & Gemini 3.1 Pro Models", fontSize = 10.sp, color = WellplateTextHint)
            }
        }

        // Sign out button
        OutlinedButton(
            onClick = {
                showSignOutConfirmationDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("signout_btn"),
            border = BorderStroke(1.5.dp, Color(0xFFC0392B)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC0392B)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Sign Out", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showSignOutConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmationDialog = false },
            title = { Text("Sign Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirmationDialog = false
                        viewModel.signOut {
                            viewModel.currentScreen.value = Screen.Splash
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0392B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSignOutConfirmationDialog = false },
                    border = BorderStroke(1.dp, WellplateGreen),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WellplateGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal dialogue popup for editing basic fields
    if (showEditDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showEditDetailsDialog = false },
            title = { Text("Edit Personal Details", color = WellplateTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    WellplateOutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Name") })
                    WellplateOutlinedTextField(
                        value = editAge.toString(),
                        onValueChange = { editAge = it.toIntOrNull() ?: editAge },
                        label = { Text("Age (yrs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    WellplateOutlinedTextField(
                        value = editWeight.toString(),
                        onValueChange = { editWeight = it.toDoubleOrNull() ?: editWeight },
                        label = { Text("Weight (${profile.weightUnit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    WellplateOutlinedTextField(
                        value = editGoalWeight.toString(),
                        onValueChange = { editGoalWeight = it.toDoubleOrNull() ?: editGoalWeight },
                        label = { Text("Goal Weight (${profile.weightUnit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveUserProfile(
                            profile.copy(
                                name = editName,
                                age = editAge,
                                weight = editWeight,
                                goalWeight = editGoalWeight
                            ),
                            navigateToHome = false
                        )
                        showEditDetailsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen)
                ) {
                    Text("Save Changes", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showEditDetailsDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WellplateGreen),
                    border = BorderStroke(1.dp, WellplateGreen)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ──────────────────────────────────────────────
// EXTRA SCREEN — RESTAURANT Companion (Extra 2)
// ──────────────────────────────────────────────
@Composable
fun RestaurantSuggestionsScreen(viewModel: WellplateViewModel) {
    val nameQuery by viewModel.menuRestaurantName.collectAsState()
    val state by viewModel.restaurantState.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var selectedMealCategory by remember { mutableStateOf("Lunch") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WellplateBackground)
            .padding(18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.currentScreen.value = Screen.Dashboard }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WellplateGreen)
            }
            Text("Restaurant Guide Companion", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = WellplateGreen))
        }

        Text("Type in any dining venue, dines, or restaurant location. Gemini will fetch and predict macro nutrition ratings for popular selections nearby!", fontSize = 12.sp, color = WellplateTextBody)

        WellplateOutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            placeholder = { Text("E.g. Chipotle, Starbucks, Olive Garden...") },
            label = { Text("Venue / Cafe Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (textInput.isNotBlank()) {
                    viewModel.menuRestaurantName.value = textInput
                    viewModel.runRestaurantQuery(textInput)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Lookup Restaurant with Gemini AI", color = Color.White)
        }

        when (state) {
            is RestaurantState.Idle -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = WellplateTextHint,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Search a restaurant above to view smart menu advice.",
                            fontSize = 13.sp,
                            color = WellplateTextHint,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            is RestaurantState.Loading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(32.dp)
                ) {
                    CircularProgressIndicator(color = WellplateGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Sifting menus with Gemini...", fontSize = 13.sp, color = WellplateTextHint)
                }
            }
            is RestaurantState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Failed to Fetch Menu suggestions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = (state as RestaurantState.Error).message,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            is RestaurantState.Success -> {
                val listMeals = (state as RestaurantState.Success).dishes
                if (listMeals.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No popular meals found. Try another venue name.", color = WellplateTextHint, fontSize = 13.sp)
                    }
                } else {
                    Text("Popular items detected at ${viewModel.menuRestaurantName.value}:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WellplateTextPrimary)

                    listMeals.forEach { dish ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WellplateSurface),
                    border = BorderStroke(1.dp, WellplateBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(dish.dishName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WellplateTextPrimary, modifier = Modifier.weight(1f))
                            Text("${dish.estimatedCalories} kcal", fontWeight = FontWeight.ExtraBold, color = WellplateGreen)
                        }
                        Text(dish.description, fontSize = 11.sp, color = WellplateTextBody, modifier = Modifier.padding(vertical = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("P: ${dish.proteinG}g", fontSize = 10.sp, color = WellplateTextHint)
                            Text("C: ${dish.carbsG}g", fontSize = 10.sp, color = WellplateTextHint)
                            Text("F: ${dish.fatG}g", fontSize = 10.sp, color = WellplateTextHint)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Log directly to diary segment:", fontSize = 10.sp, color = WellplateTextHint)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { cat ->
                                    val isSel = selectedMealCategory == cat
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) WellplateGreen else WellplateInputBg)
                                            .clickable { selectedMealCategory = cat }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(cat, fontSize = 9.sp, color = if (isSel) Color.White else WellplateTextBody, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                viewModel.logMeal(
                                    selectedMealCategory,
                                    dish.dishName,
                                    dish.estimatedCalories,
                                    dish.proteinG,
                                    dish.carbsG,
                                    dish.fatG,
                                    confidence = 88
                                )
                                viewModel.currentScreen.value = Screen.Dashboard
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen)
                        ) {
                            Text("Confirm & Log: $selectedMealCategory", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
            }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun NutritionMiniBadge(name: String, value: String, col: Color) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = col.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(name, fontSize = 11.sp, color = col, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 12.sp, color = WellplateTextPrimary, fontWeight = FontWeight.ExtraBold)
        }
    }
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2

        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

// ──────────────────────────────────────────────
// SUPABASE AUTHENTICATION SCREENS
// ──────────────────────────────────────────────

@Composable
fun SignUpScreen(viewModel: WellplateViewModel) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val loading by viewModel.authLoading.collectAsState()
    val error by viewModel.authError.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(WellplateGreenLight, WellplateBackground)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back Icon Button on top left
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(
                onClick = { viewModel.currentScreen.value = Screen.Splash },
                modifier = Modifier.testTag("signup_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = WellplateGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Brand Logo Header
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(WellplateSurface, shape = CircleShape)
                .border(1.5.dp, WellplateGreen.copy(alpha = 0.2f), CircleShape)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Eco,
                contentDescription = "Wellplate Logo",
                tint = WellplateGreen,
                modifier = Modifier.size(36.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Create Your Account",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = WellplateGreen
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Start tracking and owning your health goals.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = WellplateTextBody
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (error != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE8E8)),
                border = BorderStroke(1.dp, Color(0xFFF8B4B4)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = error ?: "",
                    color = Color(0xFFC0392B),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        // Fields Column
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Full Name Input
            WellplateOutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                placeholder = { Text("Enter your full name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = WellplateTextHint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("signup_name_input"),
                shape = RoundedCornerShape(12.dp)
            )
            
            // Email Input
            WellplateOutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                placeholder = { Text("Enter your email address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = WellplateTextHint) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth().testTag("signup_email_input"),
                shape = RoundedCornerShape(12.dp)
            )
            
            // Password Input
            WellplateOutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                placeholder = { Text("Enter your password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = WellplateTextHint) },
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().testTag("signup_password_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Button
        Button(
            onClick = {
                if (name.isBlank() || email.isBlank() || password.isBlank()) {
                    viewModel.authError.value = "Please fill in all fields."
                } else if (password.length < 8) {
                    viewModel.authError.value = "Password must be at least 8 characters."
                } else {
                    viewModel.signUp(name = name.trim(), email = email.trim(), pword = password)
                }
            },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("signup_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen)
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Sign Up", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Redirection text link to Login
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already have an account? ", color = WellplateTextBody, fontSize = 14.sp)
            Text(
                text = "Log In",
                color = WellplateGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { viewModel.currentScreen.value = Screen.Login }
                    .testTag("go_to_login_btn")
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun LoginScreen(viewModel: WellplateViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val loading by viewModel.authLoading.collectAsState()
    val error by viewModel.authError.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(WellplateGreenLight, WellplateBackground)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back Icon Button on top left
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(
                onClick = { viewModel.currentScreen.value = Screen.Splash },
                modifier = Modifier.testTag("login_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = WellplateGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Brand Logo Header
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(WellplateSurface, shape = CircleShape)
                .border(1.5.dp, WellplateGreen.copy(alpha = 0.2f), CircleShape)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Eco,
                contentDescription = "Wellplate Logo",
                tint = WellplateGreen,
                modifier = Modifier.size(36.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = WellplateGreen
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Log in to continue your wellness journey.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = WellplateTextBody
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (error != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE8E8)),
                border = BorderStroke(1.dp, Color(0xFFF8B4B4)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = error ?: "",
                    color = Color(0xFFC0392B),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        // Fields Column
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Email Input
            WellplateOutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                placeholder = { Text("Enter your email address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = WellplateTextHint) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth().testTag("login_email_input"),
                shape = RoundedCornerShape(12.dp)
            )
            
            // Password Input
            Column {
                WellplateOutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    placeholder = { Text("Enter your password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = WellplateTextHint) },
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().testTag("login_password_input"),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Forgot Password link under password input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Forgot Password?",
                        color = WellplateGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { viewModel.currentScreen.value = Screen.ForgotPassword }
                            .testTag("forgot_password_link")
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Button
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    viewModel.authError.value = "Please fill in all fields."
                } else {
                    viewModel.login(email = email.trim(), pword = password)
                }
            },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("login_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen)
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Log In", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Redirection text link to SignUp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Don't have an account? ", color = WellplateTextBody, fontSize = 14.sp)
            Text(
                text = "Sign Up",
                color = WellplateGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { viewModel.currentScreen.value = Screen.SignUp }
                    .testTag("go_to_signup_btn")
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ForgotPasswordScreen(viewModel: WellplateViewModel) {
    var email by remember { mutableStateOf("") }
    
    val loading by viewModel.authLoading.collectAsState()
    val error by viewModel.authError.collectAsState()
    var isSubmittedSuccessfully by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(WellplateGreenLight, WellplateBackground)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back Icon Button on top left
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(
                onClick = { viewModel.currentScreen.value = Screen.Login },
                modifier = Modifier.testTag("forgot_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = WellplateGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Lock Reset Logo Header
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(WellplateSurface, shape = CircleShape)
                .border(1.5.dp, WellplateGreen.copy(alpha = 0.2f), CircleShape)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Reset Password Logo",
                tint = WellplateGreen,
                modifier = Modifier.size(36.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Reset Password",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = WellplateGreen
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Enter your email address to receive a secure password reset link.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = WellplateTextBody
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (isSubmittedSuccessfully) {
            // Green success banner as per specs
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFDEF7EC)),
                border = BorderStroke(1.dp, Color(0xFF31C48D)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "A password reset link has been sent to your email. Please check your inbox.",
                    color = Color(0xFF03543F),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else if (error != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE8E8)),
                border = BorderStroke(1.dp, Color(0xFFF8B4B4)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = error ?: "",
                    color = Color(0xFFC0392B),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        if (!isSubmittedSuccessfully) {
            // Email Input
            WellplateOutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                placeholder = { Text("Enter your registered email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = WellplateTextHint) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth().testTag("forgot_email_input"),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Button
            Button(
                onClick = {
                    if (email.isBlank()) {
                        viewModel.authError.value = "Please enter your email address."
                    } else {
                        viewModel.forgotPassword(email = email.trim()) {
                            isSubmittedSuccessfully = true
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("reset_password_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen)
            ) {
                if (loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Send Reset Link", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        } else {
            // Button to return to Login
            Button(
                onClick = { viewModel.currentScreen.value = Screen.Login },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen)
            ) {
                Text("Return to Log In", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun QuickTemplateChip(name: String, calories: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color(0xFFEAF3DE),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, WellplateGreen)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF27500A),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text("$calories kcal", fontSize = 11.sp, color = WellplateGreen)
        }
    }
}
