package com.emicollect.app.ui.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emicollect.app.data.model.SortOption
import com.emicollect.app.ui.components.CustomerItem
import com.emicollect.app.ui.components.EmptyStateView
import com.emicollect.app.ui.components.GlassCard
import com.emicollect.app.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddCustomerClick: () -> Unit,
    onCustomerClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    // ─── Collect UiEvents ──────────────────────────────────────
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

    // ─── Restore-First Safety Dialog ──────────────────────────
    if (state.showRestorePrompt) {
        AlertDialog(
            onDismissRequest = { /* Prevent accidental dismiss */ },
            title = {
                Text(
                    "Existing Backup Detected",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "An existing backup was found on Google Drive. Would you like to restore your data now, or start a new session?",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.acceptRestoreFromCloud(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) { Text("Restore Data", color = MaterialTheme.colorScheme.onSurface) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRestorePrompt() }) {
                    Text("Start New Session", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // ─── Today's Progress Dialog ──────────────────────────────
    var showTodaysProgress by remember { mutableStateOf(false) }
    if (showTodaysProgress) {
        val todayDueCount = state.customers.count { it.earliestNextDueDate != null && it.earliestNextDueDate!! <= System.currentTimeMillis() }
        AlertDialog(
            onDismissRequest = { showTodaysProgress = false },
            title = {
                Text(
                    "Today's Progress",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Collected Today",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "₹${String.format("%.2f", state.totalCollectionToday)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight
                            )
                        }
                    }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Due/Overdue Customers",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "$todayDueCount customers",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (todayDueCount > 0) ErrorRed else EmeraldLight
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
                            placeholder = {
                                Text(
                                    "Search clients...",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                cursorColor = GoldAccent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Search", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = getGreeting(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                "EMI Collections",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    ),
                    actions = {
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        Box {
                            var expanded by remember { mutableStateOf(false) }
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Sort")
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Urgent First", color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = { viewModel.updateSortOption(SortOption.URGENT); expanded = false },
                                    leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp)) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Highest Debt", color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = { viewModel.updateSortOption(SortOption.HIGHEST_DEBT); expanded = false },
                                    leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp)) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Name (A-Z)", color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = { viewModel.updateSortOption(SortOption.NAME_AZ); expanded = false },
                                    leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(18.dp)) }
                                )
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddCustomerClick,
                containerColor = GoldAccent,
                contentColor = GunmetalDark,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.PersonAdd, contentDescription = "Add Customer")
                Spacer(Modifier.width(8.dp))
                Text("Add Client", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldLight, strokeWidth = 3.dp)
            }
        } else {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                        slideInVertically(
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            initialOffsetY = { it / 10 }
                        )
            ) {
                val displayedCustomers = if (state.showTodaysDueOnly) {
                    state.customers.filter { it.earliestNextDueDate != null && it.earliestNextDueDate!! <= System.currentTimeMillis() }
                } else {
                    state.customers
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        Spacer(modifier = Modifier.height(4.dp))
                        QuickActionRow(
                            onSearchClick = { viewModel.toggleSearch() },
                            onTodaysDueClick = { viewModel.toggleTodaysDueFilter() },
                            onTodaysProgressClick = { showTodaysProgress = true },
                            isTodaysDueActive = state.showTodaysDueOnly
                        )
                    }

                    // Section Header
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Accent bar
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(18.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(EmeraldLight, EmeraldPrimary)
                                            )
                                        )
                                )
                                Text(
                                    text = if (state.showTodaysDueOnly) "Today's Due" else "Your Clients",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
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
                            EmptyStateView(
                                icon = if (state.showTodaysDueOnly) Icons.Default.EventAvailable else Icons.Outlined.PersonAdd,
                                title = if (state.showTodaysDueOnly) "All caught up!" else "No clients yet",
                                subtitle = if (state.showTodaysDueOnly)
                                    "No customers are due today. Great job!"
                                else
                                    "Tap the button below to add your first client",
                                actionLabel = if (!state.showTodaysDueOnly) "Add Client" else null,
                                onAction = if (!state.showTodaysDueOnly) onAddCustomerClick else null
                            )
                        }
                    } else {
                        items(displayedCustomers, key = { it.customer.id }) { customer ->
                            CustomerItem(
                                name = customer.customer.name,
                                totalDebt = customer.totalRemainingDebt ?: 0.0,
                                nextDueDate = customer.earliestNextDueDate,
                                onClick = { onCustomerClick(customer.customer.id) }
                            )
                        }
                    }

                    // Bottom spacer for FAB
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }
}

// ─── Quick Action Row ─────────────────────────────────────────────
@Composable
fun QuickActionRow(
    onSearchClick: () -> Unit,
    onTodaysDueClick: () -> Unit,
    onTodaysProgressClick: () -> Unit,
    isTodaysDueActive: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionPill(
            icon = Icons.Default.Search,
            label = "Search",
            onClick = onSearchClick,
            modifier = Modifier.weight(1f)
        )
        QuickActionPill(
            icon = Icons.Default.Today,
            label = "Today's Due",
            onClick = onTodaysDueClick,
            isActive = isTodaysDueActive,
            modifier = Modifier.weight(1f)
        )
        QuickActionPill(
            icon = Icons.Default.Assessment,
            label = "Progress",
            onClick = onTodaysProgressClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun QuickActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val bgColor = if (isActive) EmeraldPrimary.copy(alpha = 0.15f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    val contentColor = if (isActive) EmeraldLight
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good Morning ☀️"
        hour < 17 -> "Good Afternoon 🌤"
        else -> "Good Evening 🌙"
    }
}
