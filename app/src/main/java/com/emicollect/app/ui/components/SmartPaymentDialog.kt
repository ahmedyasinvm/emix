package com.emicollect.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emicollect.app.ui.theme.*

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    if (isEdit) Icons.Default.Edit else Icons.Default.Payment,
                    contentDescription = null,
                    tint = if (isEdit) GoldAccent else EmeraldLight,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    if (isEdit) "Edit Transaction" else "Receive Payment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // Balance Info
                if (!isEdit) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GradientEmeraldStart.copy(alpha = 0.15f))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Current Balance",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                "₹${String.format("%.2f", currentBalance)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                        }
                    }
                }

                // Date Selection Row
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Payment Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(selectedDate))
                            Text(dateStr, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Icon(Icons.Default.Event, contentDescription = "Change Date", tint = GoldAccent, modifier = Modifier.size(20.dp))
                    }
                }

                // Amount Stepper
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledIconButton(
                        onClick = {
                            val current = amount.toDoubleOrNull() ?: 0.0
                            val newAmount = (current - defaultAmount).coerceAtLeast(0.0)
                            amount = String.format("%.0f", newAmount)
                        },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp))
                    }

                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = GoldAccent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    FilledIconButton(
                        onClick = {
                            val current = amount.toDoubleOrNull() ?: 0.0
                            val base = if (defaultAmount > 0) defaultAmount else 500.0
                            val newAmount = current + base
                            amount = String.format("%.0f", newAmount)
                        },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = EmeraldPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = TextWhite, modifier = Modifier.size(18.dp))
                    }
                }

                // Over-budget warning
                if (isOverBudget) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(WarningAmberTint)
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                            Text(
                                "Exceeds balance (₹${String.format("%.0f", currentBalance)}). Payment will be capped.",
                                style = MaterialTheme.typography.labelMedium,
                                color = WarningAmber
                            )
                        }
                    }
                }

                // Payment Mode Toggle
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Cash" to Icons.Default.Money, "GPay" to Icons.Default.PhoneAndroid).forEach { (mode, icon) ->
                        FilterChip(
                            selected = paymentMode == mode,
                            onClick = { paymentMode = mode },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text(mode, fontWeight = if (paymentMode == mode) FontWeight.Bold else FontWeight.Normal)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldAccent,
                                selectedLabelColor = GunmetalDark,
                                containerColor = Color.Transparent,
                                labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = MaterialTheme.colorScheme.outline,
                                selectedBorderColor = GoldAccent
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOverBudget) WarningAmber else EmeraldPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (isOverBudget) "Confirm (Capped)" else "Confirm Payment",
                    color = if (isOverBudget) GunmetalDark else TextWhite,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    )
}
