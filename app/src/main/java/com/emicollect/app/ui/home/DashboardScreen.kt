package com.emicollect.app.ui.home

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emicollect.app.data.model.SortOption
import com.emicollect.app.ui.components.CustomerItem
import com.emicollect.app.ui.components.GlassCard
import com.emicollect.app.ui.theme.EmeraldPrimary
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
    val context = LocalContext.current

    // ─── Collect UiEvents (messages, restart prompts) ─────────────────────────
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is HomeUiEvent.ShowMessage -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                is HomeUiEvent.RestartRequired -> {
                    Toast.makeText(context, "Data restored. Please restart the application.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ─── Restore-First Safety Dialog ─────────────────────────────────────────
    if (state.showRestorePrompt) {
        AlertDialog(
            onDismissRequest = { /* Prevent accidental dismiss */ },
            title = {
                Text("Existing Backup Detected", color = TextWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "An existing backup was found on Google Drive. Would you like to restore your data now, or start a new session?",
                    color = TextWhite.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.acceptRestoreFromCloud(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) { Text("Restore Data", color = TextWhite) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRestorePrompt() }) {
                    Text("Start New Session", color = TextWhite.copy(alpha = 0.7f))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // ─── Today's Progress Dialog ──────────────────────────────────────────────
    var showTodaysProgress by remember { mutableStateOf(false) }
    if (showTodaysProgress) {
        val todayDueCount = state.customers.count { it.earliestNextDueDate != null && it.earliestNextDueDate!! <= System.currentTimeMillis() }
        AlertDialog(
            onDismissRequest = { showTodaysProgress = false },
            title = { Text("Today's Progress", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Collected Today", style = MaterialTheme.typography.labelMedium, color = TextWhite.copy(alpha = 0.7f))
                            Text(
                                "₹${String.format("%.2f", state.totalCollectionToday)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Due/Overdue Customers", style = MaterialTheme.typography.labelMedium, color = TextWhite.copy(alpha = 0.7f))
                            Text(
                                "$todayDueCount customers",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (todayDueCount > 0) MaterialTheme.colorScheme.error else EmeraldPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTodaysProgress = false }) {
                    Text("Close", color = GoldAccent)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (state.isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            placeholder = { Text("Search clients...", color = TextWhite.copy(alpha = 0.6f)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                cursorColor = GoldAccent,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Search", tint = GoldAccent)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            } else {
                TopAppBar(
                    title = {
                        Text("EMI Collections", fontWeight = FontWeight.Bold, color = TextWhite)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = TextWhite,
                        actionIconContentColor = GoldAccent
                    ),
                    actions = {
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        Box {
                            var expanded by remember { mutableStateOf(false) }
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.List, contentDescription = "Sort")
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(text = { Text("Urgent First") }, onClick = {
                                    viewModel.updateSortOption(SortOption.URGENT); expanded = false
                                })
                                DropdownMenuItem(text = { Text("Highest Debt") }, onClick = {
                                    viewModel.updateSortOption(SortOption.HIGHEST_DEBT); expanded = false
                                })
                                DropdownMenuItem(text = { Text("Name (A-Z)") }, onClick = {
                                    viewModel.updateSortOption(SortOption.NAME_AZ); expanded = false
                                })
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCustomerClick,
                containerColor = GoldAccent,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) { Icon(Icons.Default.Add, contentDescription = "Add Customer") }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GoldAccent)
            }
        } else {
            val displayedCustomers = if (state.showTodaysDueOnly) {
                state.customers.filter { it.earliestNextDueDate != null && it.earliestNextDueDate!! <= System.currentTimeMillis() }
            } else {
                state.customers
            }

            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Stats Card
                item {
                    val activeCount = state.customers.count()
                    val pendingCount = state.customers.count {
                        it.earliestNextDueDate != null && it.earliestNextDueDate!! < System.currentTimeMillis()
                    }
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
                        onTodaysDueClick = { viewModel.toggleTodaysDueFilter() },
                        onTodaysProgressClick = { showTodaysProgress = true },
                        isTodaysDueActive = state.showTodaysDueOnly
                    )
                }

                // Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.showTodaysDueOnly) "Today's Due Clients" else "Your Clients",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        if (state.showTodaysDueOnly) {
                            TextButton(onClick = { viewModel.toggleTodaysDueFilter() }) {
                                Text("Show All", color = GoldAccent, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // 3. Customer List
                if (displayedCustomers.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (state.showTodaysDueOnly) "No customers due today." else "No customers yet. Tap + to add one.",
                                color = TextWhite.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(displayedCustomers, key = { it.customer.id }) { customer ->
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
}

// ─── Quick Action Row ─────────────────────────────────────────────────────────
@Composable
fun QuickActionRow(
    onSearchClick: () -> Unit,
    onTodaysDueClick: () -> Unit,
    onTodaysProgressClick: () -> Unit,
    isTodaysDueActive: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionButton(icon = Icons.Default.Search, label = "Search", onClick = onSearchClick)
        QuickActionButton(
            icon = Icons.Default.Today,
            label = "Today's Due",
            onClick = onTodaysDueClick,
            isActive = isTodaysDueActive
        )
        QuickActionButton(icon = Icons.Default.Assessment, label = "Today's Progress", onClick = onTodaysProgressClick)
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    isActive: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GlassCard(
            modifier = Modifier.size(64.dp).clickable(onClick = onClick)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) EmeraldPrimary else GoldAccent,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = if (isActive) EmeraldPrimary else TextWhite)
    }
}