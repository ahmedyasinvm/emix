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
    DAYS_90("90 Days", 90)
}

/** Intermediate data holder to preserve types in combine transforms. */
private data class RawAnalyticsData(
    val totalCollected: Double,
    val todayCollected: Double,
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

    fun setDateRange(range: DateRange) { _selectedRange.value = range }

    /** Combine all source flows into a single typed raw data object first. */
    private val rawData: Flow<RawAnalyticsData> = combine(
        repository.getTotalCollection(),
        repository.getCollectedToday(),
        repository.countOverdueLoans(System.currentTimeMillis()),
        repository.getAllCustomers(),
        repository.getAllLoans()
    ) { total, today, overdueCount, customers, loans ->
        RawAnalyticsData(
            totalCollected = total,
            todayCollected = today,
            overdueCount = overdueCount,
            customers = customers,
            loans = loans,
            transactions = emptyList() // placeholder, will be combined below
        )
    }.combine(repository.getAllTransactions()) { raw, transactions ->
        raw.copy(transactions = transactions)
    }

    val uiState: StateFlow<AnalyticsUiState> = rawData
        .combine(_selectedRange) { d, range ->
            buildUiState(d, range)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AnalyticsUiState(isLoading = true)
        )

    private fun buildUiState(d: RawAnalyticsData, range: DateRange): AnalyticsUiState {
        val chartLabels = mutableListOf<String>()
        val chartAmounts = mutableListOf<Float>()

        when (range) {
            DateRange.WEEK -> {
                val dayFormatter = java.text.SimpleDateFormat("EEE", Locale.getDefault())
                for (i in 6 downTo 0) {
                    val dayCal = Calendar.getInstance()
                    dayCal.add(Calendar.DAY_OF_YEAR, -i)
                    dayCal.set(Calendar.HOUR_OF_DAY, 0); dayCal.set(Calendar.MINUTE, 0)
                    dayCal.set(Calendar.SECOND, 0); dayCal.set(Calendar.MILLISECOND, 0)
                    val dayStart = dayCal.timeInMillis
                    dayCal.set(Calendar.HOUR_OF_DAY, 23); dayCal.set(Calendar.MINUTE, 59)
                    dayCal.set(Calendar.SECOND, 59); dayCal.set(Calendar.MILLISECOND, 999)
                    val dayEnd = dayCal.timeInMillis

                    val dayTotal = d.transactions.filter { txn ->
                        txn.datePaid in dayStart..dayEnd
                    }.sumOf { it.amountPaid }
                    chartLabels.add(dayFormatter.format(dayCal.time))
                    chartAmounts.add(dayTotal.toFloat())
                }
            }

            DateRange.DAYS_30, DateRange.DAYS_90 -> {
                val weeks = range.days / 7
                val weekFormatter = java.text.SimpleDateFormat("d MMM", Locale.getDefault())
                for (w in weeks - 1 downTo 0) {
                    val endCal = Calendar.getInstance()
                    endCal.add(Calendar.DAY_OF_YEAR, -(w * 7))
                    endCal.set(Calendar.HOUR_OF_DAY, 23); endCal.set(Calendar.MINUTE, 59)
                    endCal.set(Calendar.SECOND, 59); endCal.set(Calendar.MILLISECOND, 999)
                    val weekEnd = endCal.timeInMillis

                    val startCal = Calendar.getInstance()
                    startCal.add(Calendar.DAY_OF_YEAR, -(w * 7 + 6))
                    startCal.set(Calendar.HOUR_OF_DAY, 0); startCal.set(Calendar.MINUTE, 0)
                    startCal.set(Calendar.SECOND, 0); startCal.set(Calendar.MILLISECOND, 0)
                    val weekStart = startCal.timeInMillis

                    val weekTotal = d.transactions.filter { txn ->
                        txn.datePaid in weekStart..weekEnd
                    }.sumOf { it.amountPaid }
                    chartLabels.add(weekFormatter.format(java.util.Date(weekStart)))
                    chartAmounts.add(weekTotal.toFloat())
                }
            }
        }

        val cashTotal = d.transactions.filter { it.paymentMode.equals("Cash", ignoreCase = true) }.sumOf { it.amountPaid }
        val gpayTotal = d.transactions.filter { it.paymentMode.equals("GPay", ignoreCase = true) }.sumOf { it.amountPaid }

        val weekCal = Calendar.getInstance()
        weekCal.add(Calendar.DAY_OF_YEAR, -7)
        weekCal.set(Calendar.HOUR_OF_DAY, 0); weekCal.set(Calendar.MINUTE, 0); weekCal.set(Calendar.SECOND, 0)
        val weekStart = weekCal.timeInMillis
        val weekCollected = d.transactions.filter { it.datePaid >= weekStart }.sumOf { it.amountPaid }
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

        return AnalyticsUiState(
            totalCollected = d.totalCollected,
            todayCollected = d.todayCollected,
            weekCollected = weekCollected,
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
    val totalCollected: Double = 0.0,
    val todayCollected: Double = 0.0,
    val weekCollected: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val overdueCount: Int = 0,
    val overdueCustomers: List<OverdueCustomer> = emptyList(),
    val weeklyDayNames: List<String> = emptyList(),
    val weeklyDayAmounts: List<Float> = emptyList(),
    val cashTotal: Float = 0f,
    val gpayTotal: Float = 0f,
    val isLoading: Boolean = false
)
