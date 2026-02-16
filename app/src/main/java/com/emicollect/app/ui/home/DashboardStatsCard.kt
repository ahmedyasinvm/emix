package com.emicollect.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emicollect.app.ui.theme.EmeraldLight
import com.emicollect.app.ui.theme.EmeraldPrimary
import com.emicollect.app.ui.theme.GoldAccent
import com.emicollect.app.ui.theme.TextWhite

@Composable
fun DashboardStatsCard(
    totalCollected: Double,
    pendingCount: Int,
    activeCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(EmeraldPrimary, EmeraldLight)
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Collected Today",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite.copy(alpha = 0.8f)
                )
                
                Text(
                    text = "₹${String.format("%.2f", totalCollected)}",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = GoldAccent
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                   Column {
                       Text(
                           text = "Active",
                           style = MaterialTheme.typography.labelMedium,
                           color = TextWhite.copy(alpha = 0.7f)
                       )
                       Text(
                           text = "$activeCount Clients",
                           style = MaterialTheme.typography.titleSmall,
                           fontWeight = FontWeight.SemiBold,
                           color = TextWhite
                       )
                   }
                   
                    Column {
                       Text(
                           text = "Pending",
                           style = MaterialTheme.typography.labelMedium,
                           color = TextWhite.copy(alpha = 0.7f)
                       )
                       Text(
                           text = "$pendingCount Due",
                           style = MaterialTheme.typography.titleSmall,
                           fontWeight = FontWeight.SemiBold,
                           color = TextWhite
                       )
                   }
                }
            }
        }
    }
}
