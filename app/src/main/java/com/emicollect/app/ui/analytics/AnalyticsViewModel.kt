package com.emicollect.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emicollect.app.data.local.entity.Customer
import com.emicollect.app.data.local.entity.Loan
import com.emicollect.app.data.local.entity.Transaction
import com.emicollect.app.data.local.UserPreferencesRepository
import com.emicollect.app.data.repository.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

enum class DateRange(val label: String, val days: Int) {
    WEEK("Week", 7),
    DAYS_30("30 Days", 30),
    DAYS_90("90 Days", 90),
    CUSTOM("Custom", -1)
}

/** Intermediate data holder to preserve types in combine transforms. */
private data class RawAnalyticsData(
    val overdueCount: Int,
    val customers: List<Customer>,
    val loans: List<Loan>,
    val transactions: List<Transaction>
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: CollectionRepository,
    @Suppress("UNUSED_PARAMETER") private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _selectedRange = MutableStateFlow(DateRange.WEEK)
    val selectedRange: StateFlow<DateRange> = _selectedRange.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val customDateRange: StateFlow<Pair<Long, Long>?> = _customDateRange.asStateFlow()

    fun setDateRange(range: DateRange) { _selectedRange.value = range }
    
    fun setCustomDateRange(start: Long, end: Long) {
        _customDateRange.value = start to end
        _selectedRange.value = DateRange.CUSTOM
    }

    /** Combine all source flows into a single typed raw data object first. */
    private val rawData: Flow<RawAnalyticsData> = combine(
        repository.countOverdueLoans(System.currentTimeMillis()),
        repository.getAllCustomers(),
        repository.getAllLoans()
    ) { overdueCount, customers, loans ->
        RawAnalyticsData(
            overdueCount = overdueCount,
            customers = customers,
            loans = loans,
            transactions = emptyList() // placeholder, will be combined below
        )
    }.combine(repository.getAllTransactions()) { raw, transactions ->
        raw.copy(transactions = transactions)
    }

    val uiState: StateFlow<AnalyticsUiState> = rawData
        .combine(_selectedRange) { d, range -> d to range }
        .combine(_customDateRange) { (d, range), customRange ->
            buildUiState(d, range, customRange)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AnalyticsUiState(isLoading = true)
        )

    private fun buildUiState(d: RawAnalyticsData, range: DateRange, customRange: Pair<Long, Long>?): AnalyticsUiState {
        val chartLabels = mutableListOf<String>()
        val chartAmounts = mutableListOf<Float>()

        // 1. Determine period bounds for filtering total collected
        val (periodStart, periodEnd) = when (range) {
            DateRange.WEEK -> {
                val cal = Calendar.getInstance()
                val end = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.timeInMillis to end
            }
            DateRange.DAYS_30 -> {
                val cal = Calendar.getInstance()
                val end = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, -30)
                cal.timeInMillis to end
            }
            DateRange.DAYS_90 -> {
                val cal = Calendar.getInstance()
                val end = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, -90)
                cal.timeInMillis to end
            }
            DateRange.CUSTOM -> {
                customRange ?: (0L to System.currentTimeMillis())
            }
        }

        // 2. Filter transactions for the selected period
        val periodTransactions = d.transactions.filter { it.datePaid in periodStart..periodEnd }
        val periodCollected = periodTransactions.sumOf { it.amountPaid }

        val cashTotal = periodTransactions.filter { it.paymentMode.equals("Cash", ignoreCase = true) }.sumOf { it.amountPaid }
        val gpayTotal = periodTransactions.filter { it.paymentMode.equals("GPay", ignoreCase = true) }.sumOf { it.amountPaid }

        // 3. Build Chart Data
        when (range) {
            DateRange.WEEK -> {
                val dayFormatter = java.text.SimpleDateFormat("EEE", Locale.getDefault())
                for (i in 6 downTo 0) {
                    val dayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                    val startOfDay = dayCal.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                    val endOfDay = dayCal.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis
                    
                    val dayTotal = d.transactions.filter { it.datePaid in startOfDay..endOfDay }.sumOf { it.amountPaid }
                    chartLabels.add(dayFormatter.format(dayCal.time))
                    chartAmounts.add(dayTotal.toFloat())
                }
            }
            DateRange.DAYS_30, DateRange.DAYS_90 -> {
                val weeks = range.days / 7
                val weekFormatter = java.text.SimpleDateFormat("d MMM", Locale.getDefault())
                for (w in weeks - 1 downTo 0) {
                    val endCal = Calendar.getInstance().apply { 
                        add(Calendar.DAY_OF_YEAR, -(w * 7))
                        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
                    }.timeInMillis
                    val startCal = Calendar.getInstance().apply { 
                        add(Calendar.DAY_OF_YEAR, -(w * 7 + 6))
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                    }.timeInMillis

                    val weekTotal = d.transactions.filter { it.datePaid in startCal..endCal }.sumOf { it.amountPaid }
                    chartLabels.add(weekFormatter.format(java.util.Date(startCal)))
                    chartAmounts.add(weekTotal.toFloat())
                }
            }
            DateRange.CUSTOM -> {
                // Determine step size based on duration
                val durationDays = ((periodEnd - periodStart) / (1000 * 60 * 60 * 24)).toInt()
                if (durationDays <= 14) {
                    val dayFormatter = java.text.SimpleDateFormat("dd MMM", Locale.getDefault())
                    for (i in 0..durationDays) {
                        val startOfDay = periodStart + (i * 24 * 60 * 60 * 1000L)
                        val endOfDay = startOfDay + (24 * 60 * 60 * 1000L) - 1
                        val dayTotal = d.transactions.filter { it.datePaid in startOfDay..endOfDay }.sumOf { it.amountPaid }
                        chartLabels.add(dayFormatter.format(java.util.Date(startOfDay)))
                        chartAmounts.add(dayTotal.toFloat())
                    }
                } else {
                    // Just aggregate the custom range as a single block for simplicity or chunks of 7 days
                    val chunks = maxOf(1, durationDays / 7)
                    val chunkMillis = (periodEnd - periodStart) / chunks
                    val formatter = java.text.SimpleDateFormat("dd MMM", Locale.getDefault())
                    for (i in 0 until chunks) {
                        val cs = periodStart + (i * chunkMillis)
                        val ce = if (i == chunks - 1) periodEnd else cs + chunkMillis - 1
                        val total = d.transactions.filter { it.datePaid in cs..ce }.sumOf { it.amountPaid }
                        chartLabels.add(formatter.format(java.util.Date(cs)))
                        chartAmounts.add(total.toFloat())
                    }
                }
            }
        }

        // Outstanding & Overdue logic remains all-time globally
        val totalOutstanding = d.loans.filter { !it.isClosed }.sumOf { it.currentBalance }

        val loansByCustomer = d.loans.groupBy { it.customerId }
        val txnsByLoan = d.transactions.groupBy { it.loanId }
        val overdueList = d.customers.mapNotNull { customer ->
            val activeLoans = (loansByCustomer[customer.id] ?: emptyList()).filter { !it.isClosed }
            if (activeLoans.isEmpty()) return@mapNotNull null
            val allTxns = activeLoans.flatMap { txnsByLoan[it.loanId] ?: emptyList() }
            val lastPaymentDate = allTxns.maxByOrNull { it.datePaid }?.datePaid
            val isOverdue = com.emicollect.app.utils.DueCalculator.isOverdue(lastPaymentDate, customer)
            if (isOverdue) OverdueCustomer(customer, activeLoans.sumOf { it.currentBalance }, lastPaymentDate) else null
        }.sortedByDescending { it.totalRemainingDebt }

        // Today collected (always today, regardless of filter)
        val todayStart = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
        val todayCollected = d.transactions.filter { it.datePaid >= todayStart }.sumOf { it.amountPaid }

        return AnalyticsUiState(
            periodCollected = periodCollected,
            todayCollected = todayCollected,
            totalOutstanding = totalOutstanding,
            overdueCount = d.overdueCount,
            overdueCustomers = overdueList,
            weeklyDayNames = chartLabels,
            weeklyDayAmounts = chartAmounts,
            cashTotal = cashTotal.toFloat(),
            gpayTotal = gpayTotal.toFloat(),
            isLoading = false
        )
    }
}

data class OverdueCustomer(
    val customer: Customer,
    val totalRemainingDebt: Double,
    val lastPaymentDate: Long?
)

data class AnalyticsUiState(
    val periodCollected: Double = 0.0,
    val todayCollected: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val overdueCount: Int = 0,
    val overdueCustomers: List<OverdueCustomer> = emptyList(),
    val weeklyDayNames: List<String> = emptyList(),
    val weeklyDayAmounts: List<Float> = emptyList(),
    val cashTotal: Float = 0f,
    val gpayTotal: Float = 0f,
    val isLoading: Boolean = false
)
