package com.example.ui.templates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
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

@Composable
fun TemplateCard(
    template: MealTemplateWithItems,
    onLogClick: () -> Unit,
    onFavouriteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val t = template.template
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = WellplateSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, WellplateBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Top row: name + favourite star
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = t.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = WellplateTextPrimary
                    )
                    Text(
                        text = "${t.mealType} · ${t.totalCalories} kcal",
                        fontSize = 13.sp,
                        color = WellplateTextHint
                    )
                }
                // Favourite toggle
                IconButton(onClick = onFavouriteClick) {
                    Icon(
                        imageVector = if (t.isFavourite) Icons.Default.Star
                                      else Icons.Outlined.StarOutline,
                        contentDescription = "Favourite",
                        tint = if (t.isFavourite) Color(0xFFEF9F27) else WellplateInputBorder,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Macro chips row
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MacroChip("P", "${"%.0f".format(t.totalProtein)}g", Color(0xFFEEEDFE), Color(0xFF3C3489))
                MacroChip("C", "${"%.0f".format(t.totalCarbs)}g",   Color(0xFFEAF3DE), Color(0xFF27500A))
                MacroChip("F", "${"%.0f".format(t.totalFat)}g",     Color(0xFFFAEEDA), Color(0xFF854F0B))
            }

            // Food items preview (show first 3, then "+ N more")
            if (template.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val preview = template.items.take(3)
                preview.forEach { item ->
                    Text(
                        text = "· ${item.foodName} (${item.servingGrams.toInt()}g)",
                        fontSize = 12.sp,
                        color = WellplateTextBody
                    )
                }
                if (template.items.size > 3) {
                    Text(
                        text = "+ ${template.items.size - 3} more items",
                        fontSize = 12.sp,
                        color = WellplateGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom row: Log button + use count + delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onLogClick,
                    colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Log now", fontSize = 13.sp, color = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (t.useCount > 0) {
                    Text(
                        text = "Used ${t.useCount}×",
                        fontSize = 12.sp,
                        color = WellplateTextHint
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFC0392B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete template?", color = WellplateTextPrimary) },
            text  = { Text("\"${t.name}\" will be permanently deleted.", color = WellplateTextBody) },
            confirmButton = {
                TextButton(onClick = { onDeleteClick(); showDeleteConfirm = false }) {
                    Text("Delete", color = Color(0xFFC0392B))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = WellplateTextHint)
                }
            }
        )
    }
}

@Composable
fun MacroChip(label: String, value: String, bg: Color, textColor: Color) {
    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = "$label $value",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
