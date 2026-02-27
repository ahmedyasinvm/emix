package com.emicollect.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emicollect.app.ui.theme.EmeraldPrimary
import com.emicollect.app.ui.theme.GoldAccent
import com.emicollect.app.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartPaymentDialog(
    defaultAmount: Double,
    currentBalance: Double,
    initialDate: Long = System.currentTimeMillis(),
    initialMode: String = "Cash",
    isEdit: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, Long) -> Unit
) {
    var amount by remember(defaultAmount) { mutableStateOf(if (defaultAmount > 0) defaultAmount.toString() else "") }
    var paymentMode by remember { mutableStateOf(initialMode) }
    var selectedDate by remember { mutableStateOf(initialDate) }

    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    val isOverBudget = !isEdit && currentBalance > 0 && parsedAmount > currentBalance
    
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (isEdit) "Edit Transaction" else "Receive Payment", 
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            ) 
        },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                
                // Balance Info (Hide or show differently in edit mode)
                if (!isEdit) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Current Balance", style = MaterialTheme.typography.labelMedium, color = TextWhite.copy(alpha = 0.7f))
                            Text("₹${String.format("%.2f", currentBalance)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = GoldAccent)
                        }
                    }
                }

                // Date Selection Row
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Payment Date", style = MaterialTheme.typography.labelSmall, color = TextWhite.copy(alpha = 0.7f))
                            val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(selectedDate))
                            Text(dateStr, style = MaterialTheme.typography.bodyLarge, color = TextWhite)
                        }
                        Icon(Icons.Default.Event, contentDescription = "Change Date", tint = GoldAccent)
                    }
                }

                // Amount Stepper
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            val current = amount.toDoubleOrNull() ?: 0.0
                            val newAmount = (current - defaultAmount).coerceAtLeast(0.0)
                            amount = String.format("%.0f", newAmount) // Assuming integer steps usually
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }

                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    IconButton(
                        onClick = {
                            val current = amount.toDoubleOrNull() ?: 0.0
                            val base = if (defaultAmount > 0) defaultAmount else 500.0 // Fallback unit
                            val newAmount = current + base
                            amount = String.format("%.0f", newAmount)
                        },
                         colors = IconButtonDefaults.filledIconButtonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = TextWhite)
                    }
                }

                // Over-budget warning
                if (isOverBudget) {
                    Surface(
                        color = GoldAccent.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Amount exceeds balance (₹${String.format("%.2f", currentBalance)}). Payment will be capped.",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldAccent,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Payment Mode Toggle
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val modes = listOf("Cash", "GPay")
                    modes.forEach { mode ->
                        FilterChip(
                            selected = paymentMode == mode,
                            onClick = { paymentMode = mode },
                            label = { Text(mode) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldAccent,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val a = amount.toDoubleOrNull() ?: 0.0
                    if (a > 0) {
                        onConfirm(a, paymentMode, selectedDate)
                    }
                },
                enabled = parsedAmount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = if (isOverBudget) GoldAccent else EmeraldPrimary)
            ) {
                Text(
                    if (isOverBudget) "Confirm (Capped)" else "Confirm Payment",
                    color = if (isOverBudget) MaterialTheme.colorScheme.onSecondary else TextWhite
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextWhite.copy(alpha = 0.7f))
            }
        }
    )
}
