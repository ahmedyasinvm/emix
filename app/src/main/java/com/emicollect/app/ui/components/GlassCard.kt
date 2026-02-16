package com.emicollect.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF1F2937).copy(alpha = 0.7f), // Semi-transparent Gunmetal
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF374151).copy(alpha = 0.5f)) // Subtle border
    ) {
        androidx.compose.foundation.layout.Column(content = content)
    }
}
