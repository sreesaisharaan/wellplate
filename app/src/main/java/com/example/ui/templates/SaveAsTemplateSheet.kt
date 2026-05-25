package com.example.ui.templates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.db.MealLogEntity
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveAsTemplateSheet(
    currentMealType: String,
    currentItems: List<MealLogEntity>,
    viewModel: MealTemplateViewModel,
    onDismiss: () -> Unit
) {
    var templateName by remember { mutableStateOf("") }
    val saveState by viewModel.saveState.collectAsState()

    LaunchedEffect(saveState) {
        if (saveState is SaveState.Success) {
            delay(800)
            onDismiss()
            viewModel.resetSaveState()
        }
    }

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
                text = "Save as template",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = WellplateTextPrimary
            )
            Text(
                text = "${currentItems.size} items · $currentMealType",
                fontSize = 13.sp,
                color = WellplateTextHint,
                modifier = Modifier.padding(top = 2.dp, bottom = 20.dp)
            )

            OutlinedTextField(
                value = templateName,
                onValueChange = { templateName = it },
                label = { Text("Template name") },
                placeholder = { Text("e.g. Morning oats, My lunch combo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WellplateGreen,
                    focusedLabelColor  = WellplateGreen,
                    unfocusedBorderColor = WellplateBorder,
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent
                )
            )

            if (saveState is SaveState.Error) {
                Text(
                    text = (saveState as SaveState.Error).message,
                    color = Color(0xFFC0392B),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.saveCurrentMealAsTemplate(
                        name = templateName,
                        mealType = currentMealType,
                        items = currentItems
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WellplateGreen),
                shape = RoundedCornerShape(12.dp),
                enabled = saveState !is SaveState.Saving
            ) {
                if (saveState is SaveState.Saving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else if (saveState is SaveState.Success) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Saved!", color = Color.White, fontSize = 15.sp)
                } else {
                    Text("Save template", fontSize = 15.sp, color = Color.White)
                }
            }
        }
    }
}
