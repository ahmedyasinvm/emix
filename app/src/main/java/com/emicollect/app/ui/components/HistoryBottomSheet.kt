package com.emicollect.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emicollect.app.data.local.entity.Transaction
import com.emicollect.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryBottomSheet(
    transactions: List<Transaction>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Payment History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (transactions.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.ReceiptLong,
                    title = "No payments yet",
                    subtitle = "Payment history will appear here"
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(transactions) { transaction ->
                        TransactionRow(transaction)
                        Divider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            Text(
                text = dateFormat.format(Date(transaction.datePaid)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            // Payment mode chip
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (transaction.paymentMode == "GPay") InfoBlueTint else GlassHighlight
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = transaction.paymentMode,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = if (transaction.paymentMode == "GPay") InfoBlue
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
        Text(
            text = "₹${String.format("%.2f", transaction.amountPaid)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = EmeraldLight
        )
    }
}
