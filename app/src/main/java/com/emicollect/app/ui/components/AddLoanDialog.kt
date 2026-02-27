package com.emicollect.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import com.emicollect.app.ui.theme.EmeraldPrimary
import com.emicollect.app.ui.theme.TextWhite

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
            Text(
                "Add New Loan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name") },
                    modifier = Modifier.fillMaxWidth(),
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
                
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Start Date", style = MaterialTheme.typography.labelSmall, color = TextWhite.copy(alpha = 0.7f))
                            val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(selectedDate))
                            Text(dateStr, style = MaterialTheme.typography.bodyLarge, color = TextWhite)
                        }
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date", tint = com.emicollect.app.ui.theme.GoldAccent)
                    }
                }

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Total Price") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                OutlinedTextField(
                    value = downPayment,
                    onValueChange = { downPayment = it },
                    label = { Text("Down Payment") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Payment Mode Toggle
                Text("Down Payment Mode", style = MaterialTheme.typography.labelMedium, color = TextWhite.copy(alpha = 0.7f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("Cash", "GPay").forEach { mode ->
                        FilterChip(
                            selected = paymentMode == mode,
                            onClick = { paymentMode = mode },
                            label = { Text(mode, fontWeight = if (paymentMode == mode) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = TextWhite,
                                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                labelColor = TextWhite.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = price.toDoubleOrNull() ?: 0.0
                    val d = downPayment.toDoubleOrNull() ?: 0.0
                    if (itemName.isNotBlank() && p > 0) {
                        onConfirm(itemName, p, d, selectedDate, paymentMode)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Add Loan", color = TextWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextWhite.copy(alpha = 0.7f))
            }
        }
    )
}
