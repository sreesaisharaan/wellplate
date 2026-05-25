package com.example.ui.templates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.db.MealTemplateWithItems
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogTemplateSheet(
    template: MealTemplateWithItems,
    onLog: (mealType: String) -> Unit,
    onDismiss: () -> Unit
) {
    val meals = listOf("Breakfast", "Lunch", "Dinner", "Snacks")
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WellplateSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Log \"${template.template.name}\"",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = WellplateTextPrimary
            )
            Text(
                text = "${template.template.totalCalories} kcal · ${template.items.size} items",
                fontSize = 13.sp,
                color = WellplateTextHint,
                modifier = Modifier.padding(top = 2.dp, bottom = 20.dp)
            )

            Text(
                text = "Add to which meal?",
                fontSize = 14.sp,
                color = WellplateTextBody,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            meals.forEach { meal ->
                val icon = when (meal) {
                    "Breakfast" -> Icons.Default.WbSunny
                    "Lunch"     -> Icons.Default.LightMode
                    "Dinner"    -> Icons.Default.NightsStay
                    else        -> Icons.Default.LocalCafe
                }
                OutlinedButton(
                    onClick = { onLog(meal) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WellplateTextPrimary),
                    border = BorderStroke(0.5.dp, WellplateBorder)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = WellplateGreen
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(meal, fontSize = 15.sp, color = WellplateTextPrimary)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = WellplateTextHint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
