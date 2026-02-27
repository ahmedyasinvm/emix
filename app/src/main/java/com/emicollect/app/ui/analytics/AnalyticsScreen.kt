package com.emicollect.app.ui.analytics

import android.graphics.Color as AColor
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.emicollect.app.ui.theme.EmeraldLight
import com.emicollect.app.ui.theme.EmeraldPrimary
import com.emicollect.app.ui.theme.GoldAccent
import com.emicollect.app.ui.theme.TextWhite
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val selectedRange by viewModel.selectedRange.collectAsState()

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GoldAccent)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ─── HEADER ───
            Text(
                text = "Analytics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            // ═══════ KEY STATS CARDS (3 side-by-side) ═══════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Today",
                    value = "₹${String.format("%.0f", state.todayCollected)}",
                    icon = Icons.Default.CalendarToday,
                    gradientColors = listOf(
                        Color(0xFF059669), Color(0xFF10B981)
                    )
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "This Week",
                    value = "₹${String.format("%.0f", state.weekCollected)}",
                    icon = Icons.Default.TrendingUp,
                    gradientColors = listOf(
                        Color(0xFF2563EB), Color(0xFF3B82F6)
                    )
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Outstanding",
                    value = "₹${String.format("%.0f", state.totalOutstanding)}",
                    icon = Icons.Default.AccountBalanceWallet,
                    gradientColors = listOf(
                        Color(0xFFDC2626), Color(0xFFEF4444)
                    )
                )
            }

            // ═══════ DATE RANGE SELECTOR ═══════
            val rangeOptions = DateRange.values().toList()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rangeOptions.forEach { range ->
                    if (selectedRange == range) {
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) { Text(range.label, style = MaterialTheme.typography.labelMedium, color = TextWhite) }
                    } else {
                        OutlinedButton(onClick = { viewModel.setDateRange(range) }) {
                            Text(range.label, style = MaterialTheme.typography.labelMedium, color = TextWhite.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            // ═══════ COLLECTIONS BAR CHART ═══════
            val chartSectionTitle = when (selectedRange) {
                DateRange.WEEK -> "Collections — Last 7 Days"
                DateRange.DAYS_30 -> "Collections — Last 30 Days (Weekly)"
                DateRange.DAYS_90 -> "Collections — Last 90 Days (Weekly)"
            }
            ChartSection(title = chartSectionTitle) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    factory = { context ->
                        BarChart(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            // Style: Clean fintech look
                            description.isEnabled = false
                            legend.isEnabled = false
                            setDrawGridBackground(false)
                            setDrawBorders(false)
                            setDrawValueAboveBar(true)
                            setFitBars(true)
                            setScaleEnabled(false)
                            setPinchZoom(false)
                            setBackgroundColor(AColor.TRANSPARENT)
                            animateY(800)
                            setExtraOffsets(8f, 16f, 8f, 8f)

                            // Axis: Remove grid, keep bottom labels
                            axisLeft.apply {
                                setDrawGridLines(false)
                                setDrawAxisLine(false)
                                setDrawLabels(false)
                            }
                            axisRight.isEnabled = false
                            xAxis.apply {
                                position = XAxis.XAxisPosition.BOTTOM
                                setDrawGridLines(false)
                                setDrawAxisLine(false)
                                granularity = 1f
                                textColor = AColor.parseColor("#94A3B8")
                                textSize = 11f
                                valueFormatter = IndexAxisValueFormatter(state.weeklyDayNames)
                            }

                            // Highlight on tap
                            isHighlightPerTapEnabled = true
                            isHighlightPerDragEnabled = false

                            // Data
                            val entries = state.weeklyDayAmounts.mapIndexed { i, amt ->
                                BarEntry(i.toFloat(), amt)
                            }
                            val dataSet = BarDataSet(entries, "Collections").apply {
                                color = AColor.parseColor("#059669")
                                valueTextColor = AColor.parseColor("#A7F3D0")
                                valueTextSize = 10f
                                valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        return if (value > 0) "₹${String.format("%.0f", value)}" else ""
                                    }
                                }
                            }
                            data = BarData(dataSet).apply {
                                barWidth = 0.6f
                            }
                            invalidate()
                        }
                    },
                    update = { chart ->
                        val entries = state.weeklyDayAmounts.mapIndexed { i, amt ->
                            BarEntry(i.toFloat(), amt)
                        }
                        val dataSet = BarDataSet(entries, "Collections").apply {
                            color = AColor.parseColor("#059669")
                            valueTextColor = AColor.parseColor("#A7F3D0")
                            valueTextSize = 10f
                            valueFormatter = object : ValueFormatter() {
                                override fun getFormattedValue(value: Float): String {
                                    return if (value > 0) "₹${String.format("%.0f", value)}" else ""
                                }
                            }
                        }
                        chart.data = BarData(dataSet).apply { barWidth = 0.6f }
                        chart.xAxis.valueFormatter = IndexAxisValueFormatter(state.weeklyDayNames)
                        chart.invalidate()
                    }
                )
            }

            // ═══════ PAYMENT MODES PIE CHART ═══════
            val totalPayments = state.cashTotal + state.gpayTotal
            ChartSection(title = "💳 Payment Modes") {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    factory = { context ->
                        PieChart(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            // Donut style
                            isDrawHoleEnabled = true
                            holeRadius = 58f
                            transparentCircleRadius = 62f
                            setHoleColor(AColor.parseColor("#1A1F2E"))
                            setTransparentCircleColor(AColor.parseColor("#1A1F2E"))
                            setTransparentCircleAlpha(80)

                            // Center text
                            setDrawCenterText(true)
                            centerText = "Total\n₹${String.format("%.0f", totalPayments)}"
                            setCenterTextColor(AColor.WHITE)
                            setCenterTextSize(16f)
                            setCenterTextTypeface(Typeface.DEFAULT_BOLD)

                            // Style
                            description.isEnabled = false
                            setDrawEntryLabels(false)
                            setBackgroundColor(AColor.TRANSPARENT)
                            animateY(800)

                            // Legend
                            legend.apply {
                                isEnabled = true
                                textColor = AColor.parseColor("#94A3B8")
                                textSize = 13f
                                formSize = 12f
                                xEntrySpace = 20f
                            }

                            // Data
                            val entries = mutableListOf<PieEntry>()
                            if (state.cashTotal > 0) entries.add(PieEntry(state.cashTotal, "Cash"))
                            if (state.gpayTotal > 0) entries.add(PieEntry(state.gpayTotal, "GPay"))
                            if (entries.isEmpty()) entries.add(PieEntry(1f, "No Data"))

                            val colors = mutableListOf<Int>()
                            if (state.cashTotal > 0) colors.add(AColor.parseColor("#FFD700"))
                            if (state.gpayTotal > 0) colors.add(AColor.parseColor("#059669"))
                            if (colors.isEmpty()) colors.add(AColor.parseColor("#334155"))

                            val dataSet = PieDataSet(entries, "").apply {
                                this.colors = colors
                                sliceSpace = 3f
                                valueTextColor = AColor.WHITE
                                valueTextSize = 14f
                                valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        return "₹${String.format("%.0f", value)}"
                                    }
                                }
                            }
                            data = PieData(dataSet)
                            invalidate()
                        }
                    },
                    update = { chart ->
                        val entries = mutableListOf<PieEntry>()
                        if (state.cashTotal > 0) entries.add(PieEntry(state.cashTotal, "Cash"))
                        if (state.gpayTotal > 0) entries.add(PieEntry(state.gpayTotal, "GPay"))
                        if (entries.isEmpty()) entries.add(PieEntry(1f, "No Data"))

                        val colors = mutableListOf<Int>()
                        if (state.cashTotal > 0) colors.add(AColor.parseColor("#FFD700"))
                        if (state.gpayTotal > 0) colors.add(AColor.parseColor("#059669"))
                        if (colors.isEmpty()) colors.add(AColor.parseColor("#334155"))

                        val dataSet = PieDataSet(entries, "").apply {
                            this.colors = colors
                            sliceSpace = 3f
                            valueTextColor = AColor.WHITE
                            valueTextSize = 14f
                            valueFormatter = object : ValueFormatter() {
                                override fun getFormattedValue(value: Float): String {
                                    return "₹${String.format("%.0f", value)}"
                                }
                            }
                        }
                        chart.centerText = "Total\n₹${String.format("%.0f", totalPayments)}"
                        chart.data = PieData(dataSet)
                        chart.invalidate()
                    }
                )
            }

            // ═══════ OVERDUE LIST ═══════
            Text(
                text = "⚠️ Overdue List",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )

            if (state.overdueCustomers.isEmpty()) {
                Text(
                    text = "No overdue accounts. All settled!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextWhite.copy(alpha = 0.5f)
                )
            } else {
                state.overdueCustomers.forEach { overdue ->
                    com.emicollect.app.ui.components.GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    overdue.customer.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Pending: ₹${String.format("%.2f", overdue.totalRemainingDebt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextWhite.copy(alpha = 0.7f)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Overdue",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                                val dateStr = overdue.lastPaymentDate?.let {
                                    java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(java.util.Date(it))
                                } ?: "Never Paid"
                                Text(
                                    text = "Due since: $dateStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ═══ Glassmorphism Stat Card ═══
@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    gradientColors: List<Color>
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(gradientColors)
            )
            .padding(14.dp)
    ) {
        Column {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp
            )
        }
    }
}

// ═══ Chart Section Wrapper ═══
@Composable
private fun ChartSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A1F2E))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )
        content()
    }
}
