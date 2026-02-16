package com.emicollect.app.data.repository

import com.emicollect.app.data.local.entity.Customer
import com.emicollect.app.data.local.entity.Loan
import com.emicollect.app.data.local.entity.Transaction
import com.emicollect.app.data.model.CustomerWithDebtStatus
import com.emicollect.app.data.model.SortOption
import kotlinx.coroutines.flow.Flow

interface CollectionRepository {
    // Customer operations
    suspend fun insertCustomer(customer: Customer): Long
    suspend fun addCustomer(customer: Customer)
    suspend fun updateCustomer(customer: Customer)
    suspend fun deleteCustomer(customer: Customer)
    suspend fun getCustomerById(id: Long): Customer?
    fun getAllCustomers(): Flow<List<Customer>>
    fun getCustomersSorted(sortOption: SortOption): Flow<List<CustomerWithDebtStatus>>
    fun searchCustomers(query: String): Flow<List<CustomerWithDebtStatus>>
    fun getCustomerWithLoans(id: Long): Flow<com.emicollect.app.data.model.CustomerWithLoans>

    // Loan operations
    suspend fun insertLoan(loan: Loan): Long
    suspend fun updateLoan(loan: Loan)
    suspend fun deleteLoan(loan: Loan)
    suspend fun getLoanById(loanId: Long): Loan?
    fun getLoansForCustomer(customerId: Long): Flow<List<Loan>>
    suspend fun payInstallment(loanId: Long, amount: Double)

    // Transaction operations
    suspend fun insertTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    fun getTransactionsForLoan(loanId: Long): Flow<List<Transaction>>
    fun getCollectedToday(): Flow<Double>
    fun getTotalCollection(): Flow<Double>
    fun countOverdueLoans(timestamp: Long): Flow<Int>
}
