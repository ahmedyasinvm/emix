package com.emicollect.app.ui.addcustomer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emicollect.app.ui.components.GlassCard
import com.emicollect.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerScreen(
    onBackClick: () -> Unit,
    onCustomerSaved: () -> Unit,
    viewModel: AddCustomerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            Toast.makeText(context, "Customer added successfully!", Toast.LENGTH_SHORT).show()
            onCustomerSaved()
        }
    }

    // Show error if present
    state.error?.let { error ->
        LaunchedEffect(error) {
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Add New Client",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: Personal Info
            SectionLabel(icon = Icons.Default.Person, label = "Customer Details")

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { viewModel.onNameChange(it) },
                        label = { Text("Customer Name *") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = formFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = state.phone,
                        onValueChange = { viewModel.onPhoneChange(it) },
                        label = { Text("Phone Number *") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        colors = formFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = state.address,
                        onValueChange = { viewModel.onAddressChange(it) },
                        label = { Text("Address") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(18.dp)) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = formFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Section: Collection Schedule
            SectionLabel(icon = Icons.Default.Schedule, label = "Collection Schedule")

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Frequency Chips
                    Text(
                        "Collection Frequency",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("Weekly", "Monthly").forEach { freq ->
                            FilterChip(
                                selected = state.frequency == freq,
                                onClick = { viewModel.onFrequencyChange(freq) },
                                label = { Text(freq) },
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

                    // Day of week selection (collectionDay is Int: 1=Sun..7=Sat)
                    if (state.frequency == "Weekly") {
                        Text(
                            "Collection Day",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val days = listOf("Sun" to 1, "Mon" to 2, "Tue" to 3, "Wed" to 4, "Thu" to 5, "Fri" to 6, "Sat" to 7)
                            days.forEach { (dayName, dayValue) ->
                                val isSelected = state.collectionDay == dayValue
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.onCollectionDayChange(dayValue) },
                                    label = {
                                        Text(
                                            dayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldAccent,
                                        selectedLabelColor = GunmetalDark,
                                        containerColor = Color.Transparent,
                                        labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        selectedBorderColor = GoldAccent
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    } else {
                        // Monthly: week number selection
                        Text(
                            "Collection Week",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("1st" to 1, "2nd" to 2, "3rd" to 3, "4th" to 4).forEach { (label, week) ->
                                val isSelected = state.collectionWeek == week
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.onCollectionWeekChange(week) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldAccent,
                                        selectedLabelColor = GunmetalDark,
                                        containerColor = Color.Transparent,
                                        labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        selectedBorderColor = GoldAccent
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Save Button
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = { viewModel.saveCustomer() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
                    contentColor = GunmetalDark
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = GunmetalDark)
                } else {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Save Client", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionLabel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(EmeraldLight, EmeraldPrimary)))
        )
        Icon(icon, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(18.dp))
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun formFieldColors() = OutlinedTextFieldDefaults.colors(
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
