package com.example.ui.templates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.db.MealTemplateWithItems
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealTemplatesScreen(
    viewModel: MealTemplateViewModel,
    selectedDate: String,
    onBack: () -> Unit
) {
    val templates by viewModel.templates.collectAsState()
    val favourites by viewModel.favourites.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showLogSheet by remember { mutableStateOf<MealTemplateWithItems?>(null) }
    var showCreateSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = WellplateBackground,
        topBar = {
            TopAppBar(
                title = { Text("Meal Templates", color = WellplateTextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = WellplateTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WellplateSurface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.onSearchChanged(it)
                    },
                    placeholder = { Text("Search templates...", color = WellplateTextHint) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = WellplateTextHint) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WellplateGreen,
                        focusedLabelColor  = WellplateGreen,
                        unfocusedBorderColor = WellplateBorder,
                        unfocusedContainerColor = WellplateSurface,
                        focusedContainerColor = WellplateSurface
                    )
                )
            }

            // Favourites section (only show if there are favourites)
            if (favourites.isNotEmpty() && searchQuery.isBlank()) {
                item {
                    SectionHeader(
                        icon = Icons.Default.Star,
                        title = "Favourites",
                        iconTint = Color(0xFFEF9F27)
                    )
                }
                items(favourites, key = { it.template.id }) { template ->
                    TemplateCard(
                        template = template,
                        onLogClick = { showLogSheet = template },
                        onFavouriteClick = {
                            viewModel.toggleFavourite(template.template.id, template.template.isFavourite)
                        },
                        onDeleteClick = { viewModel.deleteTemplate(template.template.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // All templates section
            item {
                SectionHeader(
                    icon = Icons.Default.RestaurantMenu,
                    title = if (searchQuery.isBlank()) "All Templates" else "Results"
                )
            }

            if (templates.isEmpty()) {
                item {
                    EmptyTemplatesState(
                        hasSearch = searchQuery.isNotBlank()
                    )
                }
            } else {
                items(templates, key = { it.template.id }) { template ->
                    TemplateCard(
                        template = template,
                        onLogClick = { showLogSheet = template },
                        onFavouriteClick = {
                            viewModel.toggleFavourite(template.template.id, template.template.isFavourite)
                        },
                        onDeleteClick = { viewModel.deleteTemplate(template.template.id) }
                    )
                }
            }

            // Create new template button
            item {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showCreateSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WellplateGreen),
                    border = BorderStroke(1.dp, WellplateGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create new template")
                }
            }
        }
    }

    // Bottom sheet: choose which meal to log this template to
    showLogSheet?.let { template ->
        LogTemplateSheet(
            template = template,
            onLog = { targetMeal ->
                viewModel.logTemplate(template.template.id, targetMeal, selectedDate)
                showLogSheet = null
            },
            onDismiss = { showLogSheet = null }
        )
    }

    // Bottom sheet: create a new blank template
    if (showCreateSheet) {
        CreateTemplateSheet(
            onSave = { name, mealType ->
                viewModel.saveCurrentMealAsTemplate(name, mealType, emptyList())
                showCreateSheet = false
            },
            onDismiss = { showCreateSheet = false }
        )
    }
}

@Composable
fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    iconTint: Color = WellplateGreen,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = WellplateTextPrimary,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun EmptyTemplatesState(
    hasSearch: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.RestaurantMenu,
                contentDescription = null,
                tint = WellplateInputBorder,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (hasSearch) "No templates matched search" else "No saved templates yet",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = WellplateTextHint
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (hasSearch) "Try a different search query" else "Log daily foods and save as a template!",
                fontSize = 12.sp,
                color = WellplateTextHint
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTemplateSheet(
    onSave: (name: String, mealType: String) -> Unit,
    onDismiss: () -> Unit
) {
    var templateName by remember { mutableStateOf("") }
    var selectedMealType by remember { mutableStateOf("Breakfast") }
    val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snacks")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = WellplateSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Create new template",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = WellplateTextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = templateName,
                onValueChange = { templateName = it },
                label = { Text("Template name") },
                placeholder = { Text("e.g. Daily Protein Shake") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WellplateGreen,
                    focusedLabelColor = WellplateGreen,
                    unfocusedBorderColor = WellplateBorder
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Meal Type", fontSize = 14.sp, color = WellplateTextBody)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mealTypes.forEach { type ->
                    val isSelected = selectedMealType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedMealType = type },
                        label = { Text(type) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WellplateGreenLight,
                            selectedLabelColor = WellplateGreenText
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (templateName.isNotBlank()) {
                        onSave(templateName, selectedMealType)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen),
                shape = RoundedCornerShape(12.dp),
                enabled = templateName.isNotBlank()
            ) {
                Text("Create Template", color = Color.White)
            }
        }
    }
}
