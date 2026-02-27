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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

sealed class LedgerItem {
    abstract val timestamp: Long
    
    data class LoanCreated(val loan: com.emicollect.app.data.local.entity.Loan) : LedgerItem() {
        override val timestamp: Long = loan.startDate
    }
    
    data class TransactionItem(val transaction: com.emicollect.app.data.local.entity.Transaction, val itemName: String) : LedgerItem() {
        override val timestamp: Long = transaction.datePaid
    }
}

data class CustomerDetailUiState(
    val customerWithLoans: com.emicollect.app.data.model.CustomerWithLoans? = null,
    val masterLedger: List<LedgerItem> = emptyList(),
    val isLoading: Boolean = true,
    val snackbarMessage: String? = null,
    val defaultCollectionAmount: Double = 500.0
)

sealed class CustomerDetailUiEvent {
    object NavigateBack : CustomerDetailUiEvent()
}

@HiltViewModel
class CustomerDetailViewModel @Inject constructor(
    private val repository: CollectionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val customerId: Long = checkNotNull(savedStateHandle["customerId"])

    private val _uiState = MutableStateFlow(CustomerDetailUiState())
    val uiState: StateFlow<CustomerDetailUiState> = _uiState.asStateFlow()

    private val _uiEvent = kotlinx.coroutines.channels.Channel<CustomerDetailUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val _shareReceiptEvent = kotlinx.coroutines.channels.Channel<Triple<String, Double, String>>()
    val shareReceiptEvent = _shareReceiptEvent.receiveAsFlow()

    init {
        loadCustomerDetails()
    }

    private fun loadCustomerDetails() {
        viewModelScope.launch {
            repository.getCustomerWithLoans(customerId)
                .combine(repository.getTransactionsForCustomer(customerId)) { customerWithLoans, transactions ->
                    val loans = customerWithLoans.loans
                    val ledgerItems = mutableListOf<LedgerItem>()
                    
                    // Add Loan creation events
                    loans.forEach { loan ->
                        ledgerItems.add(LedgerItem.LoanCreated(loan))
                    }
                    
                    // Add Transaction events
                    val loanMap = loans.associate { it.loanId to it.itemName }
                    transactions.forEach { txn ->
                        ledgerItems.add(LedgerItem.TransactionItem(txn, loanMap[txn.loanId] ?: "Unknown Item"))
                    }
                    
                    // Sort descending (latest first)
                    val sortedLedger = ledgerItems.sortedByDescending { it.timestamp }
                    
                    Triple(customerWithLoans, sortedLedger, 500.0) // 500.0 as placeholder for default amount
                }
                .combine(userPreferencesRepository.defaultCollectionAmount) { triple, amount ->
                    triple.copy(third = amount)
                }
                .collect { (customerWithLoans, sortedLedger, amount) ->
                    _uiState.update { 
                        it.copy(
                            customerWithLoans = customerWithLoans,
                            masterLedger = sortedLedger,
                            isLoading = false,
                            defaultCollectionAmount = amount
                        ) 
                    }
                }
        }
    }

    // Helper extension to make the copy easier for the Triple
    private fun <A, B, C> Triple<A, B, C>.copy(third: C): Triple<A, B, C> = Triple(first, second, third)

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun editCustomer(name: String, phone: String, address: String) {
        viewModelScope.launch {
            val existing = _uiState.value.customerWithLoans?.customer ?: return@launch
            val updated = existing.copy(name = name.trim(), phone = phone.trim(), address = address.trim())
            repository.updateCustomer(updated)
            _uiState.update { it.copy(snackbarMessage = "Customer details updated.") }
        }
    }

    fun deleteCustomer() {
        viewModelScope.launch {
            try {
                val customer = _uiState.value.customerWithLoans?.customer ?: return@launch
                // Delete all loans (transactions cascade via FK or we delete manually)
                val loans = _uiState.value.customerWithLoans?.loans ?: emptyList()
                loans.forEach { loan -> repository.deleteLoan(loan) }
                repository.deleteCustomer(customer)
                _uiEvent.send(CustomerDetailUiEvent.NavigateBack)
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Delete failed: ${e.message}") }
            }
        }
    }

    fun deleteLoan(loan: com.emicollect.app.data.local.entity.Loan) {
        viewModelScope.launch {
            try {
                repository.deleteLoan(loan)
                _uiState.update { it.copy(snackbarMessage = "Loan deleted.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Failed to delete loan: ${e.message}") }
            }
        }
    }

    fun addLoan(itemName: String, price: Double, downPayment: Double, startDate: Long = System.currentTimeMillis(), paymentMode: String = "Cash") {
        viewModelScope.launch {
            val loan = Loan(
                customerId = customerId,
                itemName = itemName,
                totalPrincipal = price,
                downPayment = downPayment,
                currentBalance = price - downPayment,
                emiFrequency = EmiFrequency.WEEKLY,
                startDate = startDate,
                nextDueDate = startDate + 7 * 24 * 60 * 60 * 1000
            )
            val loanId = repository.insertLoan(loan)
            
            if (downPayment > 0) {
                val transaction = com.emicollect.app.data.local.entity.Transaction(
                    loanId = loanId,
                    amountPaid = downPayment,
                    paymentMode = paymentMode,
                    datePaid = startDate,
                    type = com.emicollect.app.data.local.entity.TransactionType.DOWN_PAYMENT
                )
                repository.insertTransaction(transaction)
            }
        }
    }

    fun processPayment(loanId: Long, amount: Double, paymentMode: String = "Cash", date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            try {
                val loan = repository.getLoanById(loanId) ?: return@launch
                // Cap payment at current balance — never go negative
                val actualPayment = minOf(amount, loan.currentBalance)
                if (actualPayment <= 0) {
                    _uiState.update { it.copy(snackbarMessage = "Loan is already fully paid") }
                    return@launch
                }
                val newBalance = loan.currentBalance - actualPayment
                val updatedLoan = loan.copy(
                    currentBalance = newBalance,
                    isClosed = newBalance <= 0
                )
                repository.updateLoan(updatedLoan)
                
                val transaction = com.emicollect.app.data.local.entity.Transaction(
                    loanId = loanId,
                    amountPaid = actualPayment,
                    paymentMode = paymentMode,
                    datePaid = date
                )
                repository.insertTransaction(transaction)

                val msg = if (actualPayment < amount) {
                    "Payment of ₹${String.format("%.2f", actualPayment)} applied (capped at remaining balance)"
                } else {
                    "Payment successful"
                }
                _uiState.update { it.copy(snackbarMessage = msg) }
                
                if (userPreferencesRepository.isWhatsAppEnabled.first()) {
                    val customer = _uiState.value.customerWithLoans?.customer
                    if (customer != null) {
                        _shareReceiptEvent.send(Triple(customer.name, actualPayment, loan.itemName))
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = e.message ?: "Payment failed") }
            }
        }
    }

    fun updateTransaction(transaction: com.emicollect.app.data.local.entity.Transaction, newAmount: Double, newMode: String, newDate: Long) {
        viewModelScope.launch {
            try {
                val loan = repository.getLoanById(transaction.loanId) ?: return@launch
                // Adjust loan balance: revert old amount, apply new amount
                val adjustedBalance = loan.currentBalance + transaction.amountPaid - newAmount
                repository.updateLoan(loan.copy(currentBalance = adjustedBalance, isClosed = adjustedBalance <= 0))
                
                val updatedTxn = transaction.copy(
                    amountPaid = newAmount,
                    paymentMode = newMode,
                    datePaid = newDate
                )
                repository.updateTransaction(updatedTxn)
                _uiState.update { it.copy(snackbarMessage = "Transaction updated") }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Update failed: ${e.message}") }
            }
        }
    }

    fun processTotalPayment(amount: Double, paymentMode: String = "Cash", date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            try {
                var remainingAmount = amount
                val loans = _uiState.value.customerWithLoans?.loans?.filter { !it.isClosed }?.sortedBy { it.startDate } ?: emptyList()
                
                if (loans.isEmpty()) {
                    _uiState.update { it.copy(snackbarMessage = "No active loans to pay") }
                    return@launch
                }

                val summary = StringBuilder()
                for (loan in loans) {
                    if (remainingAmount <= 0) break
                    
                    val paymentForThisLoan = minOf(remainingAmount, loan.currentBalance)
                    val newBalance = loan.currentBalance - paymentForThisLoan
                    val isClosed = newBalance <= 0
                    
                    repository.updateLoan(loan.copy(currentBalance = newBalance, isClosed = isClosed))
                    
                    val transaction = com.emicollect.app.data.local.entity.Transaction(
                        loanId = loan.loanId,
                        amountPaid = paymentForThisLoan,
                        paymentMode = paymentMode,
                        datePaid = date,
                        type = com.emicollect.app.data.local.entity.TransactionType.PAYMENT
                    )
                    repository.insertTransaction(transaction)
                    
                    if (isClosed) {
                        summary.append("Paid off ${loan.itemName} completely. ")
                    } else {
                        summary.append("Applied ₹${String.format("%.2f", paymentForThisLoan)} to ${loan.itemName}. ")
                    }
                    
                    remainingAmount -= paymentForThisLoan
                }

                _uiState.update { it.copy(snackbarMessage = summary.toString().trim()) }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Partial payment failed: ${e.message}") }
            }
        }
    }

    fun getTransactionsForLoan(loanId: Long): Flow<List<com.emicollect.app.data.local.entity.Transaction>> {
        return repository.getTransactionsForLoan(loanId)
    }

    fun shareStatement(context: android.content.Context, isCombined: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Fetch all transactions for this customer
                val transactions = repository.getTransactionsForCustomer(customerId).first()
                val customerWithLoans = _uiState.value.customerWithLoans
                val customer = customerWithLoans?.customer
                val loans = customerWithLoans?.loans ?: emptyList()
                
                // Calculate Totals
                val totalPrincipal = loans.sumOf { it.totalPrincipal }
                val remainingBalance = loans.sumOf { it.currentBalance }
                val loanNames = loans.associate { it.loanId to it.itemName }

                if (customer != null) {
                    // Generate Statement Image
                    val businessName = userPreferencesRepository.getBusinessNameSync()
                    val uri = withContext(Dispatchers.IO) {
                        com.emicollect.app.utils.ReceiptGenerator.generateStatement(
                            context, 
                            customer, 
                            transactions,
                            loans,
                            isCombined,
                            businessName = businessName
                        )
                    }
                    
                    // Share
                    if (uri != null) {
                        com.emicollect.app.utils.ShareUtils.shareImage(context, uri)
                    } else {
                        _uiState.update { it.copy(snackbarMessage = "Failed to generate statement") }
                    }
                } else {
                    _uiState.update { it.copy(snackbarMessage = "Customer data not loaded") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Error: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
