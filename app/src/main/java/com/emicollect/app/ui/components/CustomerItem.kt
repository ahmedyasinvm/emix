package com.emicollect.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emicollect.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CustomerItem(
    name: String,
    totalDebt: Double,
    nextDueDate: Long?,
    onClick: () -> Unit
) {
    val isOverdue = nextDueDate != null && nextDueDate <= System.currentTimeMillis()
    val isDueToday = nextDueDate != null &&
        SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(nextDueDate)) ==
        SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ─── Avatar with initials ───
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = avatarGradient(name)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(2).uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }

            // ─── Info ───
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // Status dot
                    if (isOverdue || isDueToday) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isOverdue) ErrorRed else WarningAmber
                                )
                        )
                    }
                }

                // Due date info
                if (nextDueDate != null) {
                    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    val label = when {
                        isOverdue -> "Overdue: ${dateFormat.format(Date(nextDueDate))}"
                        isDueToday -> "Due Today"
                        else -> "Due: ${dateFormat.format(Date(nextDueDate))}"
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isOverdue -> ErrorRed
                            isDueToday -> WarningAmber
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        }
                    )
                } else {
                    Text(
                        text = "No upcoming dues",
                        style = MaterialTheme.typography.bodySmall,
                        color = SuccessGreen.copy(alpha = 0.7f)
                    )
                }
            }

            // ─── Amount + Chevron ───
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format("%.0f", totalDebt)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (totalDebt > 0) GoldAccent else SuccessGreen
                )
                Text(
                    text = if (totalDebt > 0) "Balance" else "Cleared",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View Details",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Generates a deterministic gradient based on the customer's name
 */
private fun avatarGradient(name: String): List<Color> {
    val palettes = listOf(
        listOf(Color(0xFF064E3B), Color(0xFF0D9488)),  // Emerald → Teal
        listOf(Color(0xFF1E40AF), Color(0xFF3B82F6)),  // Blue
        listOf(Color(0xFF7C3AED), Color(0xFFA78BFA)),  // Purple
        listOf(Color(0xFFBE185D), Color(0xFFF472B6)),  // Pink
        listOf(Color(0xFFD97706), Color(0xFFFBBF24)),  // Amber
        listOf(Color(0xFF059669), Color(0xFF34D399)),  // Green
        listOf(Color(0xFFDC2626), Color(0xFFF87171)),  // Red
        listOf(Color(0xFF4338CA), Color(0xFF818CF8)),  // Indigo
    )
    val index = name.hashCode().let { (it % palettes.size + palettes.size) % palettes.size }
    return palettes[index]
}
