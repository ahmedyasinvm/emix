package com.emicollect.app.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emicollect.app.data.local.entity.EmiFrequency
import com.emicollect.app.data.local.entity.Loan
import com.emicollect.app.data.model.CustomerWithLoans
import com.emicollect.app.data.repository.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.emicollect.app.data.local.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

data class CustomerDetailUiState(
    val customerWithLoans: CustomerWithLoans? = null,
    val isLoading: Boolean = true,
    val snackbarMessage: String? = null,
    val defaultCollectionAmount: Double = 500.0
)

@HiltViewModel
class CustomerDetailViewModel @Inject constructor(
    private val repository: CollectionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val customerId: Long = checkNotNull(savedStateHandle["customerId"])

    private val _uiState = MutableStateFlow(CustomerDetailUiState())
    val uiState: StateFlow<CustomerDetailUiState> = _uiState.asStateFlow()
    
    private val _shareReceiptEvent = kotlinx.coroutines.channels.Channel<Triple<String, Double, String>>()
    val shareReceiptEvent = _shareReceiptEvent.receiveAsFlow()

    init {
        loadCustomerDetails()
    }

    private fun loadCustomerDetails() {
        viewModelScope.launch {
            repository.getCustomerWithLoans(customerId)
                .combine(userPreferencesRepository.defaultCollectionAmount) { customer, amount ->
                    Pair(customer, amount)
                }
                .collect { pair ->
                    val customer = pair.first
                    val amount = pair.second
                    _uiState.update { 
                        it.copy(
                            customerWithLoans = customer, 
                            isLoading = false,
                            defaultCollectionAmount = amount
                        ) 
                    }
                }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun addLoan(itemName: String, price: Double, downPayment: Double) {
        viewModelScope.launch {
            val loan = Loan(
                customerId = customerId,
                itemName = itemName,
                totalPrincipal = price,
                downPayment = downPayment,
                currentBalance = price - downPayment,
                emiFrequency = EmiFrequency.WEEKLY, // Defaulting to Weekly for now
                nextDueDate = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000 // 1 week from now
            )
            repository.insertLoan(loan)
        }
    }

    fun processPayment(loanId: Long, amount: Double) {
        viewModelScope.launch {
            try {
                repository.payInstallment(loanId, amount)
                _uiState.update { it.copy(snackbarMessage = "Payment successful") }
                
                // Check if WhatsApp receipt is enabled
                if (userPreferencesRepository.isWhatsAppEnabled.first()) {
                    val loan = repository.getLoanById(loanId)
                    val customer = _uiState.value.customerWithLoans?.customer
                    if (loan != null && customer != null) {
                        _shareReceiptEvent.send(Triple(customer.name, amount, loan.itemName))
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = e.message ?: "Payment failed") }
            }
        }
    }

    fun getTransactionsForLoan(loanId: Long): Flow<List<com.emicollect.app.data.local.entity.Transaction>> {
        return repository.getTransactionsForLoan(loanId)
    }
}
