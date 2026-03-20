package com.emicollect.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emicollect.app.ui.components.GlassCardElevated
import com.emicollect.app.ui.theme.*

@Composable
fun DashboardStatsCard(
    totalCollected: Double,
    pendingCount: Int,
    activeCount: Int
) {
    GlassCardElevated(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            GradientEmeraldStart,
                            GradientEmeraldEnd
                        )
                    )
                )
        ) {
            // Decorative pattern overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            radius = 400f
                        )
                    )
            )

            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Collection",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = GoldLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldLight,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Collection amount
                Text(
                    text = "₹${String.format("%.2f", totalCollected)}",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White
                )

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatPill(
                        label = "Active",
                        value = "$activeCount",
                        color = EmeraldSoft,
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        label = "Pending",
                        value = "$pendingCount",
                        color = if (pendingCount > 0) WarningAmberSoft else EmeraldSoft,
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        label = "Rate",
                        value = if (activeCount > 0) "${((activeCount - pendingCount).coerceAtLeast(0) * 100) / activeCount}%" else "—",
                        color = EmeraldSoft,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
