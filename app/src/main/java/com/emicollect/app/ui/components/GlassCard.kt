package com.emicollect.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emicollect.app.ui.theme.GlassBorder
import com.emicollect.app.ui.theme.GlassHighlight
import com.emicollect.app.ui.theme.GlassSurface

// ═══════════════════════════════════════════════════════════════════
// EMIX v4.0 — Premium Glassmorphism Card
// ═══════════════════════════════════════════════════════════════════

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = !MaterialTheme.colorScheme.background.luminance().let { it > 0.5f }

    Surface(
        modifier = modifier,
        color = if (isDark)
            GlassSurface
        else
            Color.White.copy(alpha = 0.85f),
        shape = RoundedCornerShape(cornerRadius),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = if (isDark)
                    listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.04f)
                    )
                else
                    listOf(
                        Color.Black.copy(alpha = 0.06f),
                        Color.Black.copy(alpha = 0.02f)
                    )
            )
        ),
        shadowElevation = if (isDark) 0.dp else 2.dp,
        tonalElevation = 0.dp
    ) {
        // Inner highlight gradient overlay at top
        Box {
            // Subtle highlight at top edge for glass effect
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                if (isDark) GlassHighlight else Color.Transparent,
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = 120f
                        )
                    )
            )
            Column(content = content)
        }
    }
}

/**
 * Elevated variant with a subtle glow/shadow for emphasis.
 * Use for hero cards, stats, or primary action containers.
 */
@Composable
fun GlassCardElevated(
    modifier: Modifier = Modifier,
    glowColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
    cornerRadius: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = !MaterialTheme.colorScheme.background.luminance().let { it > 0.5f }

    Surface(
        modifier = modifier,
        color = if (isDark)
            Color(0xFF1A2332).copy(alpha = 0.8f)
        else
            Color.White.copy(alpha = 0.9f),
        shape = RoundedCornerShape(cornerRadius),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = if (isDark)
                    listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.03f)
                    )
                else
                    listOf(
                        Color.Black.copy(alpha = 0.08f),
                        Color.Black.copy(alpha = 0.02f)
                    )
            )
        ),
        shadowElevation = if (isDark) 8.dp else 4.dp,
        tonalElevation = 2.dp
    ) {
        Box {
            // Top highlight
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                if (isDark) Color.White.copy(alpha = 0.08f)
                                else Color.Transparent,
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = 100f
                        )
                    )
            )
            Column(content = content)
        }
    }
}

// Helper to check luminance
private fun Color.luminance(): Float {
    val r = red * 0.2126f
    val g = green * 0.7152f
    val b = blue * 0.0722f
    return r + g + b
}
