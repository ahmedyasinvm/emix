package com.emicollect.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emicollect.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, Long, String) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var downPayment by remember { mutableStateOf("") }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var paymentMode by remember { mutableStateOf("Cash") }

    val parsedPrice = price.toDoubleOrNull() ?: 0.0
    val parsedDown = downPayment.toDoubleOrNull() ?: 0.0
    val remainingBalance = (parsedPrice - parsedDown).coerceAtLeast(0.0)

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
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
                    Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = EmeraldLight,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "Add New Loan",
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
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name") },
                    leadingIcon = { Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = loanFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Date Selection
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
                            Text("Start Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(selectedDate))
                            Text(dateStr, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date", tint = GoldAccent, modifier = Modifier.size(20.dp))
                    }
                }

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Total Price (₹)") },
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = loanFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = downPayment,
                    onValueChange = { downPayment = it },
                    label = { Text("Down Payment (₹)") },
                    leadingIcon = { Icon(Icons.Default.Savings, contentDescription = null, tint = EmeraldMuted, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = loanFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Balance Preview
                if (parsedPrice > 0) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Remaining Balance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(
                                "₹${String.format("%.0f", remainingBalance)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (remainingBalance > 0) GoldAccent else SuccessGreen
                            )
                        }
                    }
                }

                // Payment Mode
                Text("Down Payment Mode", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = TextWhite,
                                containerColor = Color.Transparent,
                                labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = MaterialTheme.colorScheme.outline,
                                selectedBorderColor = EmeraldPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            val p = price.toDoubleOrNull() ?: 0.0
            val d = downPayment.toDoubleOrNull() ?: 0.0
            val isValid = itemName.isNotBlank() && p > 0 && d >= 0 && d <= p

            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(itemName, p, d, selectedDate, paymentMode)
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add Loan", color = TextWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    )
}

@Composable
private fun loanFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedBorderColor = EmeraldPrimary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = EmeraldLight,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
    cursorColor = GoldAccent
)
