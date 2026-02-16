package com.emicollect.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emicollect.app.data.repository.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: CollectionRepository
) : ViewModel() {

    val uiState: StateFlow<AnalyticsUiState> = combine(
        repository.getTotalCollection(),
        repository.getCollectedToday(),
        repository.countOverdueLoans(System.currentTimeMillis())
    ) { total, today, overdue ->
        AnalyticsUiState(
            totalCollected = total,
            todayCollected = today,
            overdueCount = overdue,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState(isLoading = true)
    )
}

data class AnalyticsUiState(
    val totalCollected: Double = 0.0,
    val todayCollected: Double = 0.0,
    val overdueCount: Int = 0,
    val isLoading: Boolean = false
)
