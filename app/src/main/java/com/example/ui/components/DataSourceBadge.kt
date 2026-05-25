package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DataSource

@Composable
fun DataSourceBadge(source: DataSource, modifier: Modifier = Modifier) {
    val (label, bgColor, textColor, icon) = when (source) {
        DataSource.OPEN_FOOD_FACTS -> Quadruple(
            "Verified",
            Color(0xFFEAF3DE),
            Color(0xFF27500A),
            Icons.Default.CheckCircle
        )
        DataSource.GEMINI_ESTIMATE -> Quadruple(
            "AI estimate",
            Color(0xFFFAEEDA),
            Color(0xFF854F0B),
            Icons.Default.Warning
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}

@Composable
fun DataSourceBadge(source: String, modifier: Modifier = Modifier) {
    val resolvedSource = when (source) {
        "OPEN_FOOD_FACTS" -> DataSource.OPEN_FOOD_FACTS
        else -> DataSource.GEMINI_ESTIMATE
    }
    DataSourceBadge(source = resolvedSource, modifier = modifier)
}

// Helper for destructuring 4 values
data class Quadruple<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
