package com.emicollect.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange // For Analytics
import androidx.compose.material.icons.filled.Share // For Scan placeholder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.List // For Sort
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emicollect.app.data.model.SortOption
import com.emicollect.app.ui.components.CustomerItem
import com.emicollect.app.ui.components.GlassCard
import com.emicollect.app.ui.theme.GoldAccent
import com.emicollect.app.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddCustomerClick: () -> Unit,
    onCustomerClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }

    if (showSettingsDialog) {
        val isWhatsAppEnabled by viewModel.isWhatsAppEnabled.collectAsState(initial = true)
        val context = androidx.compose.ui.platform.LocalContext.current
        com.emicollect.app.ui.components.SettingsDialog(
            isWhatsAppEnabled = isWhatsAppEnabled,
            onToggleWhatsApp = { viewModel.setWhatsAppEnabled(it) },
            onBackupData = { viewModel.performBackup(context) },
            onRestoreFromCloud = { viewModel.restoreFromCloud(context) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "EMI Collections", 
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = TextWhite,
                    actionIconContentColor = GoldAccent
                ),
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    Box {
                        var expanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.List, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Urgent First") },
                                onClick = { 
                                    viewModel.updateSortOption(SortOption.URGENT)
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Highest Debt") },
                                onClick = { 
                                    viewModel.updateSortOption(SortOption.HIGHEST_DEBT)
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (A-Z)") },
                                onClick = { 
                                    viewModel.updateSortOption(SortOption.NAME_AZ)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCustomerClick,
                containerColor = GoldAccent,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Customer")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GoldAccent)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Stats Card
                item {
                    val activeCount = state.customers.count() // Simple count for now
                    val pendingCount = state.customers.count { it.earliestNextDueDate != null && it.earliestNextDueDate!! < System.currentTimeMillis() }
                    
                    DashboardStatsCard(
                        totalCollected = state.totalCollectionToday,
                        pendingCount = pendingCount,
                        activeCount = activeCount
                    )
                }

                // 2. Quick Actions
                item {
                    QuickActionRow(
                        onSearchClick = { viewModel.toggleSearch() },
                        onScanClick = { /* Scan QR */ },
                        onAnalyticsClick = { /* Analytics */ }
                    )
                }

                // Section Header
                item {
                    Text(
                        text = "Your Clients",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                // 3. Customer List
                items(state.customers) { customer ->
                    CustomerItem(
                        name = customer.customer.name, 
                        totalDebt = customer.totalRemainingDebt ?: 0.0,
                        nextDueDate = customer.earliestNextDueDate,
                        onClick = { onCustomerClick(customer.customer.id) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun QuickActionRow(
    onSearchClick: () -> Unit,
    onScanClick: () -> Unit,
    onAnalyticsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionButton(icon = Icons.Default.Search, label = "Search", onClick = onSearchClick)
        QuickActionButton(icon = Icons.Default.Share, label = "Scan QR", onClick = onScanClick)
        QuickActionButton(icon = Icons.Default.DateRange, label = "Analytics", onClick = onAnalyticsClick)
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    label: String, 
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GlassCard(
            modifier = Modifier.size(64.dp).clickable(onClick = onClick)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon, 
                    contentDescription = label,
                    tint = GoldAccent,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextWhite)
    }
}