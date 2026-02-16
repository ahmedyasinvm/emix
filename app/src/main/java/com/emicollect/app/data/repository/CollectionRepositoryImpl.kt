package com.emicollect.app.data.repository

import com.emicollect.app.data.local.dao.CustomerDao
import com.emicollect.app.data.local.dao.LoanDao
import com.emicollect.app.data.local.dao.TransactionDao
import com.emicollect.app.data.local.entity.Customer
import com.emicollect.app.data.local.entity.Loan
import com.emicollect.app.data.local.entity.Transaction
import com.emicollect.app.data.model.CustomerWithDebtStatus
import com.emicollect.app.data.model.SortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CollectionRepositoryImpl @Inject constructor(
    private val customerDao: CustomerDao,
    private val loanDao: LoanDao,
    private val transactionDao: TransactionDao
) : CollectionRepository {

    // Customer operations
    override suspend fun insertCustomer(customer: Customer): Long = customerDao.insertCustomer(customer)
    override suspend fun addCustomer(customer: Customer) { customerDao.insertCustomer(customer) }
    override suspend fun updateCustomer(customer: Customer) = customerDao.updateCustomer(customer)
    override suspend fun deleteCustomer(customer: Customer) = customerDao.deleteCustomer(customer)
    override suspend fun getCustomerById(id: Long): Customer? = customerDao.getCustomerById(id)
    override fun getAllCustomers(): Flow<List<Customer>> = customerDao.getAllCustomers()

    override fun getCustomersSorted(sortOption: SortOption): Flow<List<CustomerWithDebtStatus>> {
        return when (sortOption) {
            SortOption.URGENT -> customerDao.getCustomersSortedByUrgency()
            SortOption.HIGHEST_DEBT -> customerDao.getCustomersSortedByDebt()
            SortOption.NAME_AZ -> customerDao.getCustomersSortedByName()
        }
    }

    override fun searchCustomers(query: String): Flow<List<CustomerWithDebtStatus>> = customerDao.searchCustomers(query)

    override fun getCustomerWithLoans(id: Long): Flow<com.emicollect.app.data.model.CustomerWithLoans> = customerDao.getCustomerWithLoans(id)

    // Loan operations
    override suspend fun insertLoan(loan: Loan): Long = loanDao.insertLoan(loan)
    override suspend fun updateLoan(loan: Loan) = loanDao.updateLoan(loan)
    override suspend fun deleteLoan(loan: Loan) = loanDao.deleteLoan(loan)
    override suspend fun getLoanById(loanId: Long): Loan? = loanDao.getLoanById(loanId)
    override fun getLoansForCustomer(customerId: Long): Flow<List<Loan>> = loanDao.getLoansForCustomer(customerId)
    override suspend fun payInstallment(loanId: Long, amount: Double) {
        val loan = loanDao.getLoanById(loanId) ?: throw IllegalArgumentException("Loan not found")

        if (amount > loan.currentBalance) {
            throw IllegalArgumentException("Amount exceeds current balance")
        }

        val newBalance = loan.currentBalance - amount
        val isClosed = newBalance <= 0.0
        
        // Update next due date by 7 days if still active
        val nextDue = if (!isClosed) {
            loan.nextDueDate + (7 * 24 * 60 * 60 * 1000) 
        } else {
            loan.nextDueDate
        }

        val updatedLoan = loan.copy(
            currentBalance = newBalance,
            isClosed = isClosed,
            nextDueDate = nextDue
        )

        loanDao.updateLoan(updatedLoan)

        val transaction = com.emicollect.app.data.local.entity.Transaction(
            loanId = loanId,
            amountPaid = amount,
            datePaid = System.currentTimeMillis()
        )
        transactionDao.insertTransaction(transaction)
    }

    // Transaction operations
    override suspend fun insertTransaction(transaction: Transaction) = transactionDao.insertTransaction(transaction)
    override suspend fun deleteTransaction(transaction: Transaction) = transactionDao.deleteTransaction(transaction)
    override fun getTransactionsForLoan(loanId: Long): Flow<List<Transaction>> = transactionDao.getTransactionsForLoan(loanId)

    override fun getCollectedToday(): Flow<Double> {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        val endOfDay = calendar.timeInMillis

        return transactionDao.getTotalCollectionBetween(startOfDay, endOfDay).map { it ?: 0.0 }
    }

    override fun getTotalCollection(): Flow<Double> = transactionDao.getTotalCollection().map { it ?: 0.0 }

    override fun countOverdueLoans(timestamp: Long): Flow<Int> = loanDao.countOverdueLoans(timestamp)
}
