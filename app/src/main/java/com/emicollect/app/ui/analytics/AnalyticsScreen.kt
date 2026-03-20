package com.emicollect.app.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.emicollect.app.ui.components.EmptyStateView
import com.emicollect.app.ui.components.GlassCard
import com.emicollect.app.ui.theme.*
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val selectedRange by viewModel.selectedRange.collectAsState()
    val customRange by viewModel.customDateRange.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = datePickerState.selectedStartDateMillis
                        val end = datePickerState.selectedEndDateMillis
                        if (start != null && end != null) {
                            viewModel.setCustomDateRange(start, end)
                        }
                        showDatePicker = false
                    }
                ) { Text("Apply", color = EmeraldPrimary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) }
            },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            DateRangePicker(
                state = datePickerState,
                title = { Text("Select Analytics Period", modifier = Modifier.padding(16.dp)) },
                modifier = Modifier.fillMaxHeight(0.8f)
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.PieChart,
                            contentDescription = null,
                            tint = EmeraldLight,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "Analytics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─── Date Range Filter ─────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Predefined ranges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateRange.values().forEach { range ->
                        FilterChip(
                            selected = selectedRange == range,
                            onClick = { 
                                if (range == DateRange.CUSTOM) {
                                    showDatePicker = true
                                } else {
                                    viewModel.setDateRange(range) 
                                }
                            },
                            label = {
                                Text(
                                    range.label,
                                    fontWeight = if (selectedRange == range) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = TextWhite,
                                containerColor = Color.Transparent,
                                labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                
                // Show custom range dates if selected
                if (selectedRange == DateRange.CUSTOM && customRange != null) {
                    val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    val startStr = df.format(Date(customRange!!.first))
                    val endStr = df.format(Date(customRange!!.second))
                    Text(
                        text = "Viewing: $startStr to $endStr",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldLight,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // ─── Key Stat Cards ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalyticStatCard(
                    icon = Icons.Default.AccountBalance,
                    label = if (selectedRange == DateRange.WEEK) "Total Collected" else "Period Collected",
                    value = "₹${String.format("%.0f", state.periodCollected)}",
                    valueColor = EmeraldLight,
                    modifier = Modifier.weight(1f)
                )
                AnalyticStatCard(
                    icon = Icons.Default.Warning,
                    label = "Overdue Clients",
                    value = "${state.overdueCount}",
                    valueColor = ErrorRed,
                    modifier = Modifier.weight(1f)
                )
            }

            // ─── Outstanding ───────────────────────────────────────────
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Outstanding", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(
                            "₹${String.format("%.0f", state.totalOutstanding)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )
                    }
                    Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = ErrorRed.copy(alpha = 0.3f), modifier = Modifier.size(40.dp))
                }
            }

            // ─── Collection Trend Chart ────────────────────────────────
            SectionHeader(icon = Icons.Default.TrendingUp, label = "Collection Trend")
            if (state.weeklyDayAmounts.isNotEmpty() && state.weeklyDayAmounts.sum() > 0f) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .padding(12.dp),
                        factory = { ctx ->
                            BarChart(ctx).apply {
                                description.isEnabled = false
                                legend.isEnabled = false
                                setTouchEnabled(false)
                                setDrawGridBackground(false)
                                axisRight.isEnabled = false
                                axisLeft.apply {
                                    textColor = android.graphics.Color.parseColor("#94A3B8")
                                    textSize = 10f
                                    gridColor = android.graphics.Color.parseColor("#1F2937")
                                    setDrawAxisLine(false)
                                }
                                xAxis.apply {
                                    position = XAxis.XAxisPosition.BOTTOM
                                    textColor = android.graphics.Color.parseColor("#94A3B8")
                                    textSize = 9f
                                    gridColor = android.graphics.Color.parseColor("#1F2937")
                                    setDrawAxisLine(false)
                                    granularity = 1f
                                }
                                setExtraOffsets(8f, 8f, 8f, 8f)
                            }
                        },
                        update = { chart ->
                            val entries = state.weeklyDayAmounts.mapIndexed { idx, amount ->
                                BarEntry(idx.toFloat(), amount)
                            }
                            chart.xAxis.valueFormatter = IndexAxisValueFormatter(state.weeklyDayNames)
                            val dataSet = BarDataSet(entries, "").apply {
                                color = android.graphics.Color.parseColor("#34D399")
                                setDrawValues(false)
                            }
                            chart.data = BarData(dataSet).apply { barWidth = 0.5f }
                            chart.invalidate()
                        }
                    )
                }
            } else {
                EmptyStateView(
                    icon = Icons.Default.BarChart,
                    title = "No data yet",
                    subtitle = "Collection trends for this period will appear here once recorded"
                )
            }

            // ─── Cash vs GPay Pie ──────────────────────────────────────
            SectionHeader(icon = Icons.Default.DonutLarge, label = "Payment Modes")
            if (state.cashTotal > 0 || state.gpayTotal > 0) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AndroidView(
                            modifier = Modifier.size(140.dp),
                            factory = { ctx ->
                                PieChart(ctx).apply {
                                    description.isEnabled = false
                                    legend.isEnabled = false
                                    setTouchEnabled(false)
                                    isDrawHoleEnabled = true
                                    holeRadius = 55f
                                    transparentCircleRadius = 60f
                                    setHoleColor(android.graphics.Color.TRANSPARENT)
                                    setDrawEntryLabels(false)
                                }
                            },
                            update = { chart ->
                                val entries = listOf(
                                    PieEntry(state.cashTotal, "Cash"),
                                    PieEntry(state.gpayTotal, "GPay")
                                )
                                val dataSet = PieDataSet(entries, "").apply {
                                    colors = listOf(
                                        android.graphics.Color.parseColor("#34D399"),
                                        android.graphics.Color.parseColor("#3B82F6")
                                    )
                                    setDrawValues(false)
                                }
                                chart.data = PieData(dataSet)
                                chart.invalidate()
                            }
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            LegendRow(
                                color = EmeraldLight,
                                label = "Cash",
                                value = "₹${String.format("%.0f", state.cashTotal)}"
                            )
                            LegendRow(
                                color = InfoBlue,
                                label = "GPay",
                                value = "₹${String.format("%.0f", state.gpayTotal)}"
                            )
                        }
                    }
                }
            }

            // ─── Overdue List ──────────────────────────────────────────
            if (state.overdueCustomers.isNotEmpty()) {
                SectionHeader(icon = Icons.Default.WarningAmber, label = "Overdue Customers")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.overdueCustomers.forEach { item ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.customer.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val daysText = if (item.lastPaymentDate != null) {
                                        val days = ((System.currentTimeMillis() - item.lastPaymentDate) / (1000 * 60 * 60 * 24)).toInt()
                                        "$days days since last payment"
                                    } else {
                                        "No payments recorded"
                                    }
                                    Text(
                                        text = daysText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ErrorRed
                                    )
                                }
                                Text(
                                    text = "₹${String.format("%.0f", item.totalRemainingDebt)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ErrorRed
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AnalyticStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = valueColor.copy(alpha = 0.7f), modifier = Modifier.size(22.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, label: String) {
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
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}

@Composable
private fun LegendRow(color: Color, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
