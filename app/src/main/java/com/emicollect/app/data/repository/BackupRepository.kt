package com.emicollect.app.data.repository

import com.emicollect.app.data.local.dao.CustomerDao
import com.emicollect.app.data.local.dao.LoanDao
import com.emicollect.app.data.local.dao.TransactionDao
import com.emicollect.app.data.model.BackupData
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import androidx.room.withTransaction
import javax.inject.Inject

class BackupRepository @Inject constructor(
    private val customerDao: CustomerDao,
    private val loanDao: LoanDao,
    private val transactionDao: TransactionDao,
    private val database: com.emicollect.app.data.local.AppDatabase
) {
    suspend fun createBackupJson(): String {
        val customers = customerDao.getAllCustomers().first()
        val loans = loanDao.getAllLoans().first()
        val transactions = transactionDao.getAllTransactions().first()

        val backupData = BackupData(
            customers = customers,
            loans = loans,
            transactions = transactions
        )

        val gson = Gson()
        return gson.toJson(backupData)
    }

    suspend fun restoreBackup(json: String) {
        val gson = Gson()
        val backupData = gson.fromJson(json, BackupData::class.java)

        database.withTransaction {
            transactionDao.deleteAll()
            loanDao.deleteAll()
            customerDao.deleteAll()

            backupData.customers.forEach { customerDao.insertCustomer(it) }
            backupData.loans.forEach { loanDao.insertLoan(it) }
            backupData.transactions.forEach { transactionDao.insertTransaction(it) }
        }
    }
}
